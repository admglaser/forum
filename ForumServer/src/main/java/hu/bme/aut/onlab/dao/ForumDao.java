package hu.bme.aut.onlab.dao;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.ListJoin;
import javax.persistence.criteria.Root;

import hu.bme.aut.onlab.dao.model.TopicSeenByMemberBean;
import hu.bme.aut.onlab.model.Member;
import hu.bme.aut.onlab.model.MemberGroup;
import hu.bme.aut.onlab.model.MemberGroup_;
import hu.bme.aut.onlab.model.MemberLike;
import hu.bme.aut.onlab.model.MemberLike_;
import hu.bme.aut.onlab.model.Member_;
import hu.bme.aut.onlab.model.Permission;
import hu.bme.aut.onlab.model.PermissionSet;
import hu.bme.aut.onlab.model.PermissionSet_;
import hu.bme.aut.onlab.model.Permission_;
import hu.bme.aut.onlab.model.Post;
import hu.bme.aut.onlab.model.Post_;
import hu.bme.aut.onlab.model.Subcategory;
import hu.bme.aut.onlab.model.SubcategorySubscription;
import hu.bme.aut.onlab.model.SubcategorySubscription_;
import hu.bme.aut.onlab.model.Subcategory_;
import hu.bme.aut.onlab.model.Topic;
import hu.bme.aut.onlab.model.TopicSeenByMember;
import hu.bme.aut.onlab.model.TopicSeenByMember_;
import hu.bme.aut.onlab.model.TopicSubscription;
import hu.bme.aut.onlab.model.TopicSubscription_;
import hu.bme.aut.onlab.model.Topic_;
import hu.bme.aut.onlab.util.NavigationUtils;

@LocalBean
@Stateless
public class ForumDao {

	private final int GUEST_PERMISSION_SET_ID = 0;
	
	@PersistenceContext
	private EntityManager em;

	@EJB
	private TopicSeenByMemberBean topicSeenByMemberBean;

	private enum Position {
		FIRST, LAST
	}

	public Timestamp getLastSeenTimeOfMemberInTopic(Topic topic, Member member) {
		CriteriaBuilder builder = em.getCriteriaBuilder();
		CriteriaQuery<TopicSeenByMember> query = builder.createQuery(TopicSeenByMember.class);
		Root<TopicSeenByMember> topicSeenByMemberRoot = query.from(TopicSeenByMember.class);
		
		query.where(
				builder.and(
						builder.equal(topicSeenByMemberRoot.get(TopicSeenByMember_.member).get(Member_.id), member.getId()),
						builder.equal(topicSeenByMemberRoot.get(TopicSeenByMember_.topic).get(Topic_.id), topic.getId())
				)
		);

		query.select(topicSeenByMemberRoot);

		try {
			TopicSeenByMember lastSeenTime = em.createQuery(query).getSingleResult();
			return lastSeenTime.getSeenTime();
		} catch (NoResultException e) {
			// Has no record for the given topic and member.
			// Member has not seen the topic yet.
			return null;
		}
	}

	public Topic getTopicWithLastPostFromSubcategory(Subcategory subcategory) {
		CriteriaBuilder builder = em.getCriteriaBuilder();
		CriteriaQuery<Topic> query = builder.createQuery(Topic.class);
		Root<Topic> topicRoot = query.from(Topic.class);
		
		Join<Topic, Subcategory> subcategoryJoin = topicRoot.join(Topic_.subcategory);
		Join<Topic, Post> postJoin = topicRoot.join(Topic_.posts);

		query.groupBy(subcategoryJoin.get(Subcategory_.id));
		query.where(builder.equal(subcategoryJoin.get(Subcategory_.id), subcategory.getId()));
		query.orderBy(builder.desc(postJoin.get(Post_.time)));

		query.select(topicRoot);

		return em.createQuery(query).setMaxResults(1).getSingleResult();
	}

	public boolean hasTopicUnreadPostsByMember(Topic topic, Member member) {
		Timestamp lastRead = getLastSeenTimeOfMemberInTopic(topic, member);
		if (lastRead != null) {
			Post lastPost = getLastPostFromTopic(topic);
			if (lastPost != null) {
				Timestamp lastPostTime = lastPost.getTime();
				if (lastPostTime.after(lastRead)) {
					return true;
				} else {
					return false;
				}
			} else {
				return false;
			}
		}
		return true;
	}

	public List<Topic> getCreatedTopicsByMemberOnPage(Member member, int pageNumber) {
		CriteriaBuilder builder = em.getCriteriaBuilder();
		CriteriaQuery<Topic> query = builder.createQuery(Topic.class);
		Root<Topic> topicRoot = query.from(Topic.class);
		
		Join<Topic, Post> postJoin = topicRoot.join(Topic_.posts);

		query.where(
				builder.and(
						builder.equal(postJoin.get(Post_.member).get(Member_.id), member.getId()),
						builder.equal(postJoin.get(Post_.postNumber), 1)
				)
		);

		try {
			return em.createQuery(query)
					.setFirstResult((pageNumber-1)*NavigationUtils.ELEMENTS_PER_PAGE)
					.setMaxResults(NavigationUtils.ELEMENTS_PER_PAGE)
					.getResultList();
		} catch (NoResultException e) {
			// Has no created Topic.
			return Collections.emptyList();
		}
	}
	
	public List<Post> getPostsByMemberOnPage(Member member, int pageNumber) {
		CriteriaBuilder builder = em.getCriteriaBuilder();
		CriteriaQuery<Post> query = builder.createQuery(Post.class);
		Root<Post> postRoot = query.from(Post.class);
		
		postRoot.join(Post_.member);
		query.where(builder.equal(postRoot.get(Post_.member).get(Member_.id), member.getId()));

		try {
			return em.createQuery(query)
					.setFirstResult((pageNumber-1)*NavigationUtils.ELEMENTS_PER_PAGE)
					.setMaxResults(NavigationUtils.ELEMENTS_PER_PAGE)
					.getResultList();
		} catch (NoResultException e) {
			// Has no posts.
			return Collections.emptyList();
		}
	}
	
	private Post getPostFromTopicAtPosition(Topic topic, Position position) {
		CriteriaBuilder builder = em.getCriteriaBuilder();
		CriteriaQuery<Post> query = builder.createQuery(Post.class);
		Root<Post> postRoot = query.from(Post.class);

		query.where(builder.equal(postRoot.get(Post_.topic).get(Topic_.id), topic.getId()));
		if (position == Position.FIRST) {
			query.orderBy(builder.asc(postRoot.get(Post_.time)));
		} else if (position == Position.LAST) {
			query.orderBy(builder.desc(postRoot.get(Post_.time)));
		} else {
			throw new IllegalArgumentException("Unknown ordering as parameter.");
		}

		return em.createQuery(query).setMaxResults(1).getSingleResult();
	}

	public Post getFirstPostFromTopic(Topic topic) {
		return getPostFromTopicAtPosition(topic, Position.FIRST);
	}

	public Post getLastPostFromTopic(Topic topic) {
		return getPostFromTopicAtPosition(topic, Position.LAST);
	}
	
	public List<Post> getLikedPostsOfMemberOnPage(Member member, int pageNumber) {
		CriteriaBuilder builder = em.getCriteriaBuilder();
		CriteriaQuery<Post> query = builder.createQuery(Post.class);
		Root<Post> root = query.from(Post.class);

		// Posts that has no like will not be included
		ListJoin<Post, MemberLike> likeJoin = root.join(Post_.likes, JoinType.INNER);

		query.where(builder.equal(root.get(Post_.member).get(Member_.id), member.getId()));

		query.select(root).distinct(true);
		query.orderBy(builder.desc(builder.count(likeJoin)));
		query.groupBy(root);
		
		try {
			return em.createQuery(query)
					.setFirstResult((pageNumber - 1) * NavigationUtils.ELEMENTS_PER_PAGE)
					.setMaxResults(NavigationUtils.ELEMENTS_PER_PAGE)
					.getResultList();
		} catch (NoResultException e) {
			// Has no likes.
			return Collections.emptyList();
		}
	}
	
	public int getLikedPostsCountOfMember(Member member) {
		CriteriaBuilder builder = em.getCriteriaBuilder();
		CriteriaQuery<Long> query = builder.createQuery(Long.class);
		Root<Post> postRoot = query.from(Post.class);

		// Posts that has no like will not be included
		postRoot.join(Post_.likes, JoinType.INNER);		

		query.select(builder.count(postRoot));
		query.where(builder.equal(postRoot.get(Post_.member).get(Member_.id), member.getId()));
		
		try {
			return em.createQuery(query).getSingleResult().intValue();
		} catch (NoResultException e) {
			return 0;
		}
	}

	public List<Member> getMembersOnPage(int pageNumber) {
		CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
		CriteriaQuery<Member> criteriaQuery = criteriaBuilder.createQuery(Member.class);
		Root<Member> root = criteriaQuery.from(Member.class);

		criteriaQuery.select(root);

		try {
			return em.createQuery(criteriaQuery)
					.setFirstResult((pageNumber - 1) * NavigationUtils.ELEMENTS_PER_PAGE)
					.setMaxResults(NavigationUtils.ELEMENTS_PER_PAGE)
					.getResultList();
		} catch (NoResultException e) {
			// Has no likes.
			return Collections.emptyList();
		}
	}

	public List<Post> getPostsOfTopicOnPage(Topic topic, int pageNumber) {
		CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
		CriteriaQuery<Post> query = criteriaBuilder.createQuery(Post.class);
		Root<Post> root = query.from(Post.class);

		Join<Post, Topic> topicJoin = root.join(Post_.topic);

		query.where(criteriaBuilder.and(criteriaBuilder.equal(topicJoin.get(Topic_.id), topic.getId())));

		query.orderBy(criteriaBuilder.asc(root.get(Post_.time)));

		query.select(root);

		try {
			return em.createQuery(query)
					.setFirstResult((pageNumber - 1) * NavigationUtils.ELEMENTS_PER_PAGE)
					.setMaxResults(NavigationUtils.ELEMENTS_PER_PAGE)
					.getResultList();
		} catch (NoResultException e) {
			return Collections.emptyList();
		}
	}

	public int getPostsCountOfTopic(Topic topic) {
		CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
		CriteriaQuery<Long> query = criteriaBuilder.createQuery(Long.class);
		Root<Post> root = query.from(Post.class);

		Join<Post, Topic> topicJoin = root.join(Post_.topic);

		query.where(criteriaBuilder.and(criteriaBuilder.equal(topicJoin.get(Topic_.id), topic.getId())));

		query.select(criteriaBuilder.count(root));

		try {
			return em.createQuery(query).getSingleResult().intValue();
		} catch (NoResultException e) {
			return 0;
		}
	}
	
	public int getPostLikesCount(Post post) {
		CriteriaBuilder criteriaBuilder  = em.getCriteriaBuilder();
		CriteriaQuery<Long> query = criteriaBuilder.createQuery(Long.class);
		Root<MemberLike> root = query.from(MemberLike.class);

		query.where(criteriaBuilder.equal(root.get(MemberLike_.post).get(Post_.id), post.getId()));

		query.groupBy(root.get(MemberLike_.post).get(Post_.id));

		query.select(criteriaBuilder.count(root));

		try {
			return em.createQuery(query).getSingleResult().intValue();
		} catch (NoResultException e) {
			return 0;
		}
	}

	public List<Member> getMembersWhoLikedPost(Post post) {
		CriteriaBuilder criteriaBuilder  = em.getCriteriaBuilder();
		CriteriaQuery<Member> query = criteriaBuilder.createQuery(Member.class);
		Root<Member> root = query.from(Member.class);

		Join<Member, MemberLike> memberLikeJoin = root.join(Member_.likes);

		query.where(criteriaBuilder.equal(memberLikeJoin.get(MemberLike_.post).get(Post_.id), post.getId()));

		query.select(root);

		try {
			return em.createQuery(query).getResultList();
		} catch (NoResultException e) {
			return Collections.emptyList();
		}
	}
	
	public List<Permission> getMemberPermissionsForSubcategory(Member member, Subcategory subcategory) {
		CriteriaBuilder builder = em.getCriteriaBuilder();
		CriteriaQuery<Permission> query = builder.createQuery(Permission.class);
		Root<Permission> permissionRoot = query.from(Permission.class);
		
		ListJoin<Permission, PermissionSet> permissionSetjoin = permissionRoot.join(Permission_.permissionSets);
		ListJoin<PermissionSet, MemberGroup> memberGroupJoin = permissionSetjoin.join(PermissionSet_.memberGroups);
		
		query.where(
				builder.and(
						builder.equal(memberGroupJoin.get(MemberGroup_.id), member.getMemberGroup().getId()),
						builder.equal(permissionRoot.get(Permission_.subcategory).get(Subcategory_.id), subcategory.getId())
				)
		);
				
		query.select(permissionRoot);
		
		try {
			return em.createQuery(query).getResultList();
		} catch (NoResultException e) {
			return Collections.emptyList();
		}
	}
	
	public List<Permission> getGuestPermissionsForSubcategory(Subcategory subcategory) {
		CriteriaBuilder builder = em.getCriteriaBuilder();
		CriteriaQuery<Permission> query = builder.createQuery(Permission.class);
		Root<Permission> permissionRoot = query.from(Permission.class);
		
		ListJoin<Permission, PermissionSet> permissionSetjoin = permissionRoot.join(Permission_.permissionSets);
		
		query.where(
				builder.and(
						builder.equal(permissionSetjoin.get(PermissionSet_.id), GUEST_PERMISSION_SET_ID),
						builder.equal(permissionRoot.get(Permission_.subcategory).get(Subcategory_.id), subcategory.getId())
				)
		);
				
		query.select(permissionRoot);
		
		try {
			return em.createQuery(query).getResultList();
		} catch (NoResultException e) {
			return Collections.emptyList();
		}
	}

	public boolean canMemberViewSubcategory(Member member, Subcategory subcategory) {
		boolean canView = true;
		List<Permission> permissions = null;
		if (member == null) {
			permissions = getGuestPermissionsForSubcategory(subcategory);
		} else {
			permissions = getMemberPermissionsForSubcategory(member, subcategory);
		}
		for (int i = 0; i < permissions.size(); i++) {
			Permission permission = permissions.get(i);
			if (!permission.getReadAllowed()) {
				canView = false;
				break;
			}
		}
		return canView;
	}

	public boolean canMemberStartTopicInSubcategory(Member member, Subcategory subcategory) {
		boolean canStart = true;
		List<Permission> permissions = null;
		if (member == null) {
			permissions = getGuestPermissionsForSubcategory(subcategory);
		} else {
			permissions = getMemberPermissionsForSubcategory(member, subcategory);
		}

		for (Permission permission : permissions) {
			if (!permission.getStartAllowed()) {
				canStart = false;
				break;
			}
		}
		return canStart;
	}

	public boolean canMemberReplyInTopic(Member member, Topic topic) {
		boolean canReply = true;
		List<Permission> permissions = null;
		if (member == null) {
			permissions = getGuestPermissionsForSubcategory(topic.getSubcategory());
		} else {
			permissions = getMemberPermissionsForSubcategory(member, topic.getSubcategory());
		}

		for (Permission permission : permissions) {
			if (!permission.getReplyAllowed()) {
				canReply = false;
				break;
			}
		}
		return canReply;
	}

	public TopicSubscription getTopicSubscription(Member member, Topic topic) {
		CriteriaBuilder builder = em.getCriteriaBuilder();
		CriteriaQuery<TopicSubscription> query = builder.createQuery(TopicSubscription.class);
		Root<TopicSubscription> root = query.from(TopicSubscription.class);

		query.where(
				builder.and(
						builder.equal(root.get(TopicSubscription_.member).get(Member_.id), member.getId()),
						builder.equal(root.get(TopicSubscription_.topic).get(Topic_.id), topic.getId())
				)
		);

		try {
			return em.createQuery(query).getSingleResult();
		} catch (NoResultException e) {
			return null;
		}
	}

	public boolean isMemberFollowingTopic(Member member, Topic topic) {
		if (member == null	|| topic == null) {
			return false;
		}

		return getTopicSubscription(member, topic) != null;
	}

	public SubcategorySubscription getSubcategorySubscription(Member member, Subcategory subcategory) {
		CriteriaBuilder builder = em.getCriteriaBuilder();
		CriteriaQuery<SubcategorySubscription> query = builder.createQuery(SubcategorySubscription.class);
		Root<SubcategorySubscription> root = query.from(SubcategorySubscription.class);

		query.where(
				builder.and(
						builder.equal(root.get(SubcategorySubscription_.member).get(Member_.id), member.getId()),
						builder.equal(root.get(SubcategorySubscription_.subcategory).get(Subcategory_.id), subcategory.getId())
				)
		);

		try {
			return em.createQuery(query).getSingleResult();
		} catch (NoResultException e) {
			return null;
		}
	}

	public boolean isMemberFollowingSubcategory(Member member, Subcategory subcategory) {
		if (member == null	|| subcategory == null) {
			return false;
		}

		return getSubcategorySubscription(member, subcategory) != null;
	}

	public TopicSeenByMember getTopicSeenByMember(Member member, Topic topic) {
		CriteriaBuilder builder = em.getCriteriaBuilder();
		CriteriaQuery<TopicSeenByMember> query = builder.createQuery(TopicSeenByMember.class);
		Root<TopicSeenByMember> root = query.from(TopicSeenByMember.class);

		query.where(
				builder.and(
						builder.equal(root.get(TopicSeenByMember_.member).get(Member_.id), member.getId()),
						builder.equal(root.get(TopicSeenByMember_.topic).get(Topic_.id), topic.getId())
				)
		);

		try {
			return em.createQuery(query).getSingleResult();
		} catch (NoResultException e) {
			return null;
		}
	}

	public void renewTopicSeenByMember(Member member, Topic topic) {
		if (member != null && topic != null) {
			TopicSeenByMember topicSeenByMember = getTopicSeenByMember(member, topic);
			if (topicSeenByMember == null) {
				topicSeenByMember = new TopicSeenByMember();
			}
			topicSeenByMember.setMember(member);
			topicSeenByMember.setTopic(topic);
			topicSeenByMember.setSeenTime(Timestamp.valueOf(LocalDateTime.now()));

			topicSeenByMemberBean.merge(topicSeenByMember);
		}
	}

	public Post getPostByPostNumber(Topic topic, int postNumber) {
		CriteriaBuilder builder = em.getCriteriaBuilder();
		CriteriaQuery<Post> query = builder.createQuery(Post.class);
		Root<Post> root = query.from(Post.class);

		query.where(
				builder.and(
						builder.equal(root.get(Post_.topic).get(Topic_.id), topic.getId()),
						builder.equal(root.get(Post_.postNumber), postNumber)
				)
		);

		try {
			return em.createQuery(query).getSingleResult();
		} catch (NoResultException e) {
			return null;
		}
	}

	public MemberLike getMemberLike(Member member, Post post) {
		if (member == null || post == null) {
			return null;
		}

		CriteriaBuilder builder = em.getCriteriaBuilder();
		CriteriaQuery<MemberLike> query = builder.createQuery(MemberLike.class);
		Root<MemberLike> root = query.from(MemberLike.class);

		query.where(
				builder.and(
						builder.equal(root.get(MemberLike_.member).get(Member_.id), member.getId()),
						builder.equal(root.get(MemberLike_.post).get(Post_.id), post.getId())
				)
		);

		try {
			return em.createQuery(query).getSingleResult();
		} catch (NoResultException e) {
			return null;
		}
	}

	public int getMembersCount() {
		CriteriaBuilder builder = em.getCriteriaBuilder();
		CriteriaQuery<Long> query = builder.createQuery(Long.class);
		Root<Member> memberRoot = query.from(Member.class);

		query.select(builder.count(memberRoot));

		try {
			return em.createQuery(query).getSingleResult().intValue();
		} catch (NoResultException e) {
			return 0;
		}
	}

	public List<Topic> getTopicsOnPage(Subcategory subcategory, int pageNumber) {
		CriteriaBuilder builder = em.getCriteriaBuilder();
		CriteriaQuery<Topic> query = builder.createQuery(Topic.class);
		Root<Topic> topicRoot = query.from(Topic.class);

		ListJoin<Topic, Post> postJoin = topicRoot.join(Topic_.posts);
		
		query.where(
				builder.equal(topicRoot.get(Topic_.subcategory).get(Subcategory_.id), subcategory.getId())		
		);
		
		query.groupBy(topicRoot);
		query.orderBy(builder.desc(postJoin.get(Post_.time)));
		
		query.select(topicRoot);

		try {
			return em.createQuery(query)
					.setFirstResult((pageNumber - 1) * NavigationUtils.ELEMENTS_PER_PAGE)
					.setMaxResults(NavigationUtils.ELEMENTS_PER_PAGE)
					.getResultList();
		} catch (NoResultException e) {
			return Collections.emptyList();
		}
	}

	public int getTopicCountInSubcategory(Subcategory subcategory) {
		CriteriaBuilder builder = em.getCriteriaBuilder();
		CriteriaQuery<Long> query = builder.createQuery(Long.class);
		Root<Topic> topicRoot = query.from(Topic.class);

		query.where(
				builder.equal(topicRoot.get(Topic_.subcategory).get(Subcategory_.id), subcategory.getId())		
		);
		
		query.select(builder.count(topicRoot));

		try {
			return em.createQuery(query).getSingleResult().intValue();
		} catch (NoResultException e) {
			return 0;
		}
	}
}
