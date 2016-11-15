package hu.bme.aut.onlab.bean;

import java.sql.Timestamp;
import java.util.Collections;
import java.util.List;

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

import hu.bme.aut.onlab.model.Member;
import hu.bme.aut.onlab.model.MemberGroup;
import hu.bme.aut.onlab.model.MemberGroup_;
import hu.bme.aut.onlab.model.MemberLike;
import hu.bme.aut.onlab.model.MemberLike_;
import hu.bme.aut.onlab.model.Permission;
import hu.bme.aut.onlab.model.PermissionSet;
import hu.bme.aut.onlab.model.PermissionSet_;
import hu.bme.aut.onlab.model.Permission_;
import hu.bme.aut.onlab.model.Post;
import hu.bme.aut.onlab.model.Post_;
import hu.bme.aut.onlab.model.Subcategory;
import hu.bme.aut.onlab.model.Subcategory_;
import hu.bme.aut.onlab.model.Topic;
import hu.bme.aut.onlab.model.TopicSeenByMember;
import hu.bme.aut.onlab.model.TopicSeenByMember_;
import hu.bme.aut.onlab.model.Topic_;
import hu.bme.aut.onlab.util.NavigationUtils;

@LocalBean
@Stateless
public class ForumReadService {

	private final int GUEST_PERMISSION_SET_ID = 1;
	
	@PersistenceContext
	private EntityManager em;

	private enum Position {
		FIRST, LAST
	}

	public Timestamp getLastSeenTimeOfMemberInTopic(Topic topic, Member member) {
		CriteriaBuilder builder = em.getCriteriaBuilder();
		CriteriaQuery<TopicSeenByMember> query = builder.createQuery(TopicSeenByMember.class);
		Root<TopicSeenByMember> topicSeenByMemberRoot = query.from(TopicSeenByMember.class);
		
		query.where(
				builder.and(
						builder.equal(topicSeenByMemberRoot.get(TopicSeenByMember_.memberId), member.getId()),
						builder.equal(topicSeenByMemberRoot.get(TopicSeenByMember_.topicId), topic.getId())
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
				if (lastRead.after(lastPostTime) || lastRead.equals(lastRead)) {
					return false;
				}
			} else {
				return false;
			}
		}
		return false;
	}

	public List<Topic> getCreatedTopicsByMember(Member member, int pageNumber) {
		CriteriaBuilder builder = em.getCriteriaBuilder();
		CriteriaQuery<Topic> query = builder.createQuery(Topic.class);
		Root<Topic> topicRoot = query.from(Topic.class);
		
		Join<Topic, Post> postJoin = topicRoot.join(Topic_.posts);

		query.where(
				builder.and(
						builder.equal(postJoin.get(Post_.memberId), member.getId()),
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
	
	public List<Post> getPostsByMember(Member member, int pageNumber) {
		CriteriaBuilder builder = em.getCriteriaBuilder();
		CriteriaQuery<Post> query = builder.createQuery(Post.class);
		Root<Post> postRoot = query.from(Post.class);
		
		postRoot.join(Post_.member);
		query.where(builder.equal(postRoot.get(Post_.memberId), member.getId()));

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

		query.where(builder.equal(postRoot.get(Post_.topicId), topic.getId()));
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
	
	public List<Post> getLikedPostsOfMember(Member member, int pageNumber) {
		CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
		CriteriaQuery<Post> criteriaQuery = criteriaBuilder.createQuery(Post.class);
		Root<Post> root = criteriaQuery.from(Post.class);

		// Posts that has no like will not be included
		root.join(Post_.likes, JoinType.INNER);

		criteriaQuery.where(criteriaBuilder.equal(root.get(Post_.memberId), member.getId()));

		criteriaQuery.select(root).distinct(true);

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
	
	public int getLikedPostsCountOfMember(Member member) {
		CriteriaBuilder builder = em.getCriteriaBuilder();
		CriteriaQuery<Long> query = builder.createQuery(Long.class);
		Root<Post> postRoot = query.from(Post.class);

		// Posts that has no like will not be included
		postRoot.join(Post_.likes, JoinType.INNER);		

		query.select(builder.count(query.from(Post.class)));
		query.where(builder.equal(postRoot.get(Post_.memberId), member.getId()));
		
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
					.setFirstResult((pageNumber-1) * NavigationUtils.ELEMENTS_PER_PAGE)
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

		query.where(criteriaBuilder.equal(root.get(MemberLike_.postId), post.getId()));

		query.groupBy(root.get(MemberLike_.postId));

		query.select(criteriaBuilder.count(root));

		try {
			return em.createQuery(query).getSingleResult().intValue();
		} catch (NoResultException e) {
			return 0;
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
						builder.equal(permissionRoot.get(Permission_.subcategoryId), subcategory.getId())
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
						builder.equal(permissionRoot.get(Permission_.subcategoryId), subcategory.getId())
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
		boolean canView = false;
		List<Permission> permissions = null;
		if (member == null) {
			permissions = getGuestPermissionsForSubcategory(subcategory);
		} else {
			permissions = getMemberPermissionsForSubcategory(member, subcategory);
		}
		for (int i = 0; i < permissions.size(); i++) {
			Permission permission = permissions.get(i);
			if (permission.getReadAllowed()) {
				canView = true;
				break;
			}
		}
		return canView;
	}
}
