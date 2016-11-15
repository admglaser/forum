package hu.bme.aut.onlab.bean;

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
import javax.persistence.criteria.ListJoin;
import javax.persistence.criteria.Root;

import hu.bme.aut.onlab.model.Conversation;
import hu.bme.aut.onlab.model.ConversationSeenByMember;
import hu.bme.aut.onlab.model.ConversationSeenByMember_;
import hu.bme.aut.onlab.model.Conversation_;
import hu.bme.aut.onlab.model.Member;
import hu.bme.aut.onlab.model.Member_;
import hu.bme.aut.onlab.model.Message;
import hu.bme.aut.onlab.model.Message_;
import hu.bme.aut.onlab.util.NavigationUtils;

@LocalBean
@Stateless
public class MessagingService {

	@PersistenceContext
	private EntityManager em;

	public List<Conversation> getConversations(Member member, int pageNumber) {
		CriteriaBuilder builder = em.getCriteriaBuilder();
		CriteriaQuery<Conversation> query = builder.createQuery(Conversation.class);
		Root<Conversation> conversationRoot = query.from(Conversation.class);
	
		ListJoin<Conversation, Member> join = conversationRoot.join(Conversation_.members);
		
		query.where(builder.equal(join.get(Member_.id), member.getId()));
	
		try {
			return em.createQuery(query)
					.setFirstResult((pageNumber-1)*NavigationUtils.ELEMENTS_PER_PAGE)
					.setMaxResults(NavigationUtils.ELEMENTS_PER_PAGE)
					.getResultList();
		} catch (NoResultException e) {
			return Collections.emptyList();
		}
	}

	public int getConversationsCount(Member member) {
		CriteriaBuilder builder = em.getCriteriaBuilder();
		CriteriaQuery<Long> query = builder.createQuery(Long.class);
		Root<Conversation> conversationRoot = query.from(Conversation.class);

		ListJoin<Conversation, Member> memberJoin = conversationRoot.join(Conversation_.members);
		
		query.select(builder.count(conversationRoot));

		query.where(builder.equal(memberJoin.get(Member_.id), member.getId()));
	
		try {
			return em.createQuery(query).getSingleResult().intValue();
		} catch (NoResultException e) {
			return 0;
		}
	}

	public List<Message> getMessagesOfConversationOnPage(Conversation conversation, int pageNumber) {
        CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
        CriteriaQuery<Message> criteriaQuery = criteriaBuilder.createQuery(Message.class);
        Root<Message> messageRoot = criteriaQuery.from(Message.class);

        Join<Message, Conversation> conversationJoin = messageRoot.join(Message_.conversation);

        criteriaQuery.where(criteriaBuilder.equal(conversationJoin.get(Conversation_.id), conversation.getId()));

        criteriaQuery.orderBy(criteriaBuilder.asc(messageRoot.get(Message_.messageNumber)));

        criteriaQuery.select(messageRoot);

        try {
            return em.createQuery(criteriaQuery)
                    .setFirstResult((pageNumber-1) * NavigationUtils.ELEMENTS_PER_PAGE)
                    .setMaxResults(NavigationUtils.ELEMENTS_PER_PAGE)
                    .getResultList();
        } catch (NoResultException e) {
            return Collections.emptyList();
        }
    }

    public int getMessagesCountOfConversation(Conversation conversation) {
        CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
        CriteriaQuery<Long> query = criteriaBuilder.createQuery(Long.class);
        Root<Message> messageRoot = query.from(Message.class);

        Join<Message, Conversation> conversationJoin = messageRoot.join(Message_.conversation);

        query.where(criteriaBuilder.equal(conversationJoin.get(Conversation_.id), conversation.getId()));

        query.orderBy(criteriaBuilder.asc(messageRoot.get(Message_.messageNumber)));

        query.select(criteriaBuilder.count(messageRoot));

        try {
            return em.createQuery(query).getSingleResult().intValue();
        } catch (NoResultException e) {
            return 0;
        }
    }

	public boolean isConversationUnread(Conversation conversation, Member member) {
		CriteriaBuilder builder = em.getCriteriaBuilder();
		CriteriaQuery<ConversationSeenByMember> query = builder.createQuery(ConversationSeenByMember.class);
		Root<ConversationSeenByMember> conversationSeenByMemberRoot = query.from(ConversationSeenByMember.class);

		query.where(
				builder.and(
						builder.equal(conversationSeenByMemberRoot.get(ConversationSeenByMember_.conversationId), conversation.getId()),
						builder.equal(conversationSeenByMemberRoot.get(ConversationSeenByMember_.memberId), member.getId())));

		query.select(conversationSeenByMemberRoot);
		
		try {
			ConversationSeenByMember conversationSeenByMember = em.createQuery(query).getSingleResult();
			return conversationSeenByMember.getSeenMessageNumber() < conversation.getMessageCount();
		} catch (NoResultException e) {
			//if conversation is not in "seen" table then its unread
			return true;
		}
	}

	public Message getMessageOfConversation(Conversation conversation, int messageNumber) {
		CriteriaBuilder builder = em.getCriteriaBuilder();
		CriteriaQuery<Message> query = builder.createQuery(Message.class);
		Root<Message> root = query.from(Message.class);

		Join<Message, Conversation> join = root.join(Message_.conversation);

		query.where(
				builder.and(
						builder.equal(join.get(Conversation_.id), conversation.getId()),
						builder.equal(root.get(Message_.messageNumber), messageNumber)));
		
		query.select(root);
		
		try {
			 return em.createQuery(query).getSingleResult();
		} catch (NoResultException e) {
			return null;
		}
	}

	public List<Message> getLastFiveMessages(Member member) {
		CriteriaBuilder builder = em.getCriteriaBuilder();
		CriteriaQuery<Message> query = builder.createQuery(Message.class);
		Root<Message> messageRoot = query.from(Message.class);
		
		Join<Message, Conversation> conversationJoin = messageRoot.join(Message_.conversation);
		ListJoin<Conversation, Member> memberJoin = conversationJoin.join(Conversation_.members);
		
		query.where(
				builder.and(
						builder.equal(memberJoin.get(Member_.id), member.getId()),
						builder.equal(messageRoot.get(Message_.messageNumber), conversationJoin.get(Conversation_.messageCount))));
		
		query.select(messageRoot);
		
		query.orderBy(builder.desc(messageRoot.get(Message_.time)));
		
		try {
			return em.createQuery(query).getResultList();
		} catch (NoResultException e) {
			return Collections.emptyList();
		}
	}

	public Conversation getConversation(Member member, int conversationNumber) {
		CriteriaBuilder builder = em.getCriteriaBuilder();
		CriteriaQuery<Conversation> query = builder.createQuery(Conversation.class);
		Root<Conversation> conversationRoot = query.from(Conversation.class);
		
		ListJoin<Conversation, Member> memberJoin = conversationRoot.join(Conversation_.members);
		
		query.where(
				builder.and(
						builder.equal(memberJoin.get(Member_.id), member.getId()),
						builder.equal(conversationRoot.get(Conversation_.conversationNumber), conversationNumber)
				)
		);
		
		query.select(conversationRoot);
		
		try {
			return em.createQuery(query).getSingleResult();
		} catch (NoResultException e) {
			return null;
		}
	}
	
}
