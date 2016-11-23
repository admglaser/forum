package hu.bme.aut.onlab.rest;

import hu.bme.aut.onlab.bean.ForumService;
import hu.bme.aut.onlab.bean.NotificationService;
import hu.bme.aut.onlab.bean.dao.*;
import hu.bme.aut.onlab.model.*;
import hu.bme.aut.onlab.util.Formatter;
import hu.bme.aut.onlab.util.LinkUtils;
import hu.bme.aut.onlab.util.NavigationUtils;
import hu.bme.aut.onlab.util.NotificationUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.ejb.EJB;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Path("topic")
public class TopicRs {

	@EJB
	private ForumService forumReadService;

	@EJB
	private NotificationService notificationService;

	@EJB
	private TopicBean topicBean;

	@EJB
	private PostBean postBean;

	@EJB
	private MemberBean memberBean;

	@EJB
	private TopicSubscriptionBean topicSubscriptionBean;

	@EJB
	private MemberLikeBean memberLikeBean;

	@GET
	@Path("{topicId}")
	@Produces(MediaType.APPLICATION_JSON)
	public String getTopic(@Context Member member, @PathParam("topicId") int topicId) {
		return getTopicWithPage(member, topicId, 1);
	}

	@GET
	@Path("{topicId}/{pageNumber}")
	@Produces(MediaType.APPLICATION_JSON)
	public String getTopicWithPage(@Context Member member, @PathParam("topicId") int topicId, @PathParam("pageNumber") int pageNumber) {
		JSONObject result = new JSONObject();
		JSONArray postsJsonArray = new JSONArray();

		Topic topic = topicBean.findEntityById(topicId);

		if (topic != null && forumReadService.canMemberViewSubcategory(member, topic.getSubcategory())) {
			List<Post> posts = forumReadService.getPostsOfTopicOnPage(topic, pageNumber);
			// posts is empty if navigated to a too high page number
			if (! posts.isEmpty() ) {
				Post firstPost = posts.get(0);
				for (Post post : posts) {
					JSONObject postJson = new JSONObject();

					Member memberOfPost = post.getMember();
					MemberGroup memberGroup = memberOfPost.getMemberGroup();

					List<Member> members = forumReadService.getMembersWhoLikedPost(post);
					StringBuilder sb = new StringBuilder();
					for (int i=0; i<members.size(); i++) {
						sb.append(members.get(i).getDisplayName());
						if (i < members.size()-1) {
							sb.append(", ");
						}
					}

					List<StyleOfMemberGroup> styles = memberGroup.getStyleOfMemberGroups();
					JSONArray stylesJsonArray = new JSONArray();
					for (int i=0; i<styles.size(); i++) {
						JSONObject styleJson = new JSONObject();
						styleJson.put("style", styles.get(i).getStyle());

						stylesJsonArray.put(styleJson);
					}

					postJson.put("likers", sb.toString());
					postJson.put("styles", stylesJsonArray);
					postJson.put("username", memberOfPost.getDisplayName());
					postJson.put("userLink", "#/user/" + memberOfPost.getId());
					postJson.put("userImageLink", LinkUtils.getProfilePictureLink(memberOfPost.getPictureId()));
					postJson.put("postCount", memberOfPost.getPostCount());
					postJson.put("memberGroup", memberGroup.getTitle());
					postJson.put("time", Formatter.formatTimeStamp(post.getTime()));
					postJson.put("text", post.getText());
					postJson.put("likeCount", forumReadService.getPostLikesCount(post));
					postJson.put("postNumber", post.getPostNumber());
					postJson.put("postLink", "#/topic/" + topic.getId() + "/" + pageNumber);
					postJson.put("isPostLiked", (forumReadService.getMemberLike(member, post) != null));
					postsJsonArray.put(postJson);
				}

				result.put("title", topic.getTitle());
				result.put("startedByText", firstPost.getMember().getDisplayName() + ", " + Formatter.formatTimeStamp(firstPost.getTime()));
				result.put("isFollowedByMember", forumReadService.isMemberFollowingTopic(member, topic));
				result.put("canFollow", (member != null));
				result.put("canReply", forumReadService.canMemberReplyInTopic(member, topic));
				result.put("posts", postsJsonArray);
				result.put("pages", NavigationUtils.getPagesJsonArray("#/topic/" + topic.getId(), pageNumber, forumReadService.getPostsCountOfTopic(topic)));
			}
			// Renew seen of topic of a member
			forumReadService.renewTopicSeenByMember(member, topic);

			// Increase view count of the topic
			topic.setViewCount(topic.getViewCount() + 1);
			topicBean.merge(topic);
		} else {
			result.put("error", true);
		}
		return result.toString();
	}


	@POST
	@Path("new")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public String addPost(@Context Member member, String data) {
		JSONObject input = new JSONObject(data);
		JSONObject result = new JSONObject();
		String errorMessage;

		if (member != null) {
			int topicId = Integer.parseInt((String) input.get("topic"));
			Topic topic = topicBean.findEntityById( topicId );
			if (topic != null) {
				if (forumReadService.canMemberReplyInTopic(member, topic)) {
					String quotedPostText = (input.has("quote")) ? (String) input.get("quote") : null;
					String postText = (String) input.get("text");
					int quotePostNumber = input.has("quotePostNumber") ? input.getInt("quotePostNumber") : 0;

					Post lastPostInTopic = forumReadService.getLastPostFromTopic(topic);

					Post post = new Post();
					post.setTopic(topic);
					post.setMember(member);
					post.setText((quotedPostText == null) ? postText : quotedPostText + postText);
					post.setPostNumber(lastPostInTopic.getPostNumber() + 1);
					post.setTime(Timestamp.valueOf(LocalDateTime.now()));

					postBean.add(post);

					member.setPostCount(member.getPostCount() + 1);
					memberBean.merge(member);

					//add mention notification
					if (postText.contains("@")) {
						String mentionedName = NotificationUtils.getMentionedName(postText);
						if (mentionedName != null) {
							List<Member> list = memberBean.findEntitiesByEquality(Member_.displayName, mentionedName);
							if (list.size() > 0) {
								Member mentionedMember = list.get(0);
								notificationService.addMention(member, mentionedMember, post);
							}
						}
					}

					//add quote notification
					if (quotePostNumber > 0) {
						notificationService.addQuote(member, topicId, quotePostNumber);
					}

					//add subscribtion notification
					notificationService.addNewReply(post);


					forumReadService.renewTopicSeenByMember(member, topic);

					result.put("success", true);
					return result.toString();
				} else {
					errorMessage = "You have no permission to create new reply.";
				}
			} else {
				errorMessage = "Unknown topic.";
			}
		} else {
			errorMessage = "Unidentified member.\nPlease log in.";
		}

		result.put("success", false);
		result.put("errorMessage", errorMessage);
		return result.toString();
	}

	@POST
	@Path("follow")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public String followTopic(@Context Member member, String data) {
		JSONObject input = new JSONObject(data);
		JSONObject result = new JSONObject();
		String errorMessage;
		int topicId;

		try {
			topicId = Integer.parseInt((String) input.get("topic"));
		} catch (NumberFormatException e) {
			result.put("success", false);
			result.put("errorMessage", "Invalid number format for topic ID.");
			return result.toString();
		}

		if (member != null) {
			Topic topic = topicBean.findEntityById( topicId );
			if (topic != null) {
				if (input.has("isFollowRequest")) {

					boolean isFollowRequest = input.getBoolean("isFollowRequest");
					TopicSubscription existingTopicSubscription = forumReadService.getTopicSubscription(member, topic);

					if (isFollowRequest) {
						// Request to follow the topic
						if (existingTopicSubscription == null) {
							TopicSubscription topicSubscription = new TopicSubscription();
							topicSubscription.setMember(member);
							topicSubscription.setTopic(topic);
							topicSubscriptionBean.add(topicSubscription);
						}

					} else {
						// Request to unfollow the topic
						if (existingTopicSubscription != null) {
							topicSubscriptionBean.remove(existingTopicSubscription);
						}
					}

					result.put("success", true);
					return result.toString();
				} else {
					errorMessage = "Unknown error has occurred.";
				}
			} else {
				errorMessage = "Unknown topic.";
			}
		} else {
			errorMessage = "Unidentified member.\nPlease log in.";
		}

		result.put("success", false);
		result.put("errorMessage", errorMessage);
		return result.toString();
	}

	@POST
	@Path("like")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public String likePost(@Context Member member, String data) {
		JSONObject input = new JSONObject(data);
		JSONObject result = new JSONObject();
		String errorMessage;
		int topicId;
		int postNumber;

		try {
			topicId = Integer.parseInt(input.getString("topic"));
		} catch (NumberFormatException e) {
			result.put("success", false);
			result.put("errorMessage", "Invalid number format for topic ID.");
			return result.toString();
		}

		try {
			postNumber = input.getInt("postNumber");
		} catch (NumberFormatException e) {
			result.put("success", false);
			result.put("errorMessage", "Invalid number format for post number.");
			return result.toString();
		}

		if (member != null) {
			Topic topic = topicBean.findEntityById(topicId);
			if (topic != null) {
				Post post = forumReadService.getPostByPostNumber(topic, postNumber);
				if (post != null) {
					if (input.has("isLikeRequest")) {

						boolean isLikeRequest = input.getBoolean("isLikeRequest");
						MemberLike existingMemberLike = forumReadService.getMemberLike(member, post);

						if (isLikeRequest) {
							// Request to like the post
							if (existingMemberLike == null) {
								MemberLike memberLike = new MemberLike();
								memberLike.setMember(member);
								memberLike.setPost(post);

								memberLikeBean.add(memberLike);

								//like notification
								notificationService.addLike(member, post);

							}

						} else {
							// Request to unlike the post
							if (existingMemberLike != null) {
								memberLikeBean.remove(existingMemberLike);
							}
						}

						result.put("success", true);
						return result.toString();
					} else {
						errorMessage = "Unknown error has occurred.";
					}
				} else {
					errorMessage = "Unknown post.";
				}
			} else {
				errorMessage = "Unknown topic.";
			}
		} else {
			errorMessage = "Unidentified member.\nPlease log in.";
		}

		result.put("success", false);
		result.put("errorMessage", errorMessage);
		return result.toString();
	}

}
