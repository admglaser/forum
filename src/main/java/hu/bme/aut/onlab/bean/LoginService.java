package hu.bme.aut.onlab.bean;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateful;
import javax.enterprise.context.ApplicationScoped;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import hu.bme.aut.onlab.bean.dao.MemberBean;
import hu.bme.aut.onlab.bean.helper.CriteriaHelper;
import hu.bme.aut.onlab.bean.helper.CriteriaHelper.CriteriaType;
import hu.bme.aut.onlab.model.Member;
import hu.bme.aut.onlab.model.Member_;

@LocalBean
@Stateful
@ApplicationScoped
public class LoginService {

	@PersistenceContext
	private EntityManager em;

	@EJB
	private MemberBean memberBean;

	private Map<String, Integer> membersMap = new HashMap<>();

	public Member getMember(String encodedUserPassword) {
		if (membersMap.containsKey(encodedUserPassword)) {
			int id = membersMap.get(encodedUserPassword);
			Member member = memberBean.findEntityById(id);
			updateLastActiveTime(member);
			return member;
		} 
		
		CriteriaHelper<Member> criteriaHelper = new CriteriaHelper<>(Member.class, em, CriteriaType.SELECT);
		CriteriaQuery<Member> criteriaQuery = criteriaHelper.getCriteriaQuery();
		Root<Member> rootEntity = criteriaHelper.getRootEntity();
		CriteriaBuilder criteriaBuilder = criteriaHelper.getCriteriaBuilder();
		
		try {
			byte[] decodedBytes = Base64.getDecoder().decode(encodedUserPassword.replaceFirst("Basic" + " ", ""));
			String usernameAndPassword = new String(decodedBytes, "UTF-8");
			final StringTokenizer tokenizer = new StringTokenizer(usernameAndPassword, ":");
			final String username = tokenizer.nextToken();
			final String password = tokenizer.nextToken();

			criteriaQuery.where(
					criteriaBuilder.and(
							criteriaBuilder.equal(rootEntity.get(Member_.userName), username),
							criteriaBuilder.equal(rootEntity.get(Member_.password), password)
							)
					);
			criteriaQuery.select(rootEntity);

			Member member = em.createQuery(criteriaQuery).getSingleResult();
			membersMap.put(encodedUserPassword, member.getId());
			updateLastActiveTime(member);
			return member;
		} catch (Exception e) {
			return null;
		}
	}

	private void updateLastActiveTime(Member member) {
		member.setActiveTime(Timestamp.valueOf(LocalDateTime.now()));
	}

}
