package hu.bme.aut.onlab.model;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

@Entity
@Table(name = "member")
public class Member {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    
	@Column(name = "user_name")
	private String userName;
    
	@Column(name = "password")
	private String password;
    
	@Column(name = "email")
	private String email;
    
	@Column(name = "display_name")
	
	private String displayName;
    
	@Column(name = "post_count")
	private int postCount;
	
	@Column(name = "topic_count")
	private int topicCount;

	@Column(name = "views_count")
	private int viewsCount;
	
	@Column(name = "birthday")
	private Date birthday;
    
    @OneToMany(mappedBy = "member")
	private List<MemberLike> likes;
    
    @ManyToOne
    @JoinColumn(name = "member_group_id", referencedColumnName = "id", nullable = false)
	private MemberGroup memberGroup;
    
    @OneToMany(mappedBy = "member")
	private List<Notification> notifications;
    
    @OneToMany(mappedBy = "member")
	private List<Post> posts;
    
    @OneToMany(mappedBy = "member")
	private List<SubcategorySubscription> subcategorySubscriptions;
    
    @OneToMany(mappedBy = "member")
	private List<TopicSubscription> topicSubscriptions;
    
    @ManyToMany(mappedBy = "members")
    private List<Conversation> conversations;
    
    @OneToMany(mappedBy = "member")
    private List<ConversationToMember> conversationToMemberList;
    
    @Column(name = "register_time")
	private Timestamp registerTime;
    
    @Column(name = "active_time")
    private Timestamp activeTime;
    
    @Column(name = "picture_id")
	private String pictureId;
    
	@OneToMany(mappedBy = "member")
    private List<ConversationSeenByMember> conversationSeenByMembers;
    
	@OneToMany(mappedBy = "member")
	private List<TopicSeenByMember> topicSeenByMembers;

	@Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Member member = (Member) o;

        return id == member.id;

    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public int getPostCount() {
        return postCount;
    }

    public void setPostCount(int postCount) {
        this.postCount = postCount;
    }
    
    public int getTopicCount() {
        return topicCount;
    }

    public void setTopicCount(int topicCount) {
        this.topicCount = topicCount;
    }
    
    public int getViewsCount() {
        return viewsCount;
    }

    public void setViewsCount(int viewsCount) {
        this.viewsCount = viewsCount;
    }

    public Date getBirthday() {
        return birthday;
    }

    public void setBirthday(Date birthday) {
        this.birthday = birthday;
    }

    public List<MemberLike> getLikes() {
        return likes;
    }

    public void setLikes(List<MemberLike> likes) {
        this.likes = likes;
    }

    public MemberGroup getMemberGroup() {
        return memberGroup;
    }

    public void setMemberGroup(MemberGroup memberGroup) {
        this.memberGroup = memberGroup;
    }

    public List<Notification> getNotifications() {
        return notifications;
    }

    public void setNotifications(List<Notification> notifications) {
        this.notifications = notifications;
    }
    
    public List<Post> getPosts() {
        return posts;
    }

    public void setPosts(List<Post> posts) {
        this.posts = posts;
    }

    public List<SubcategorySubscription> getSubcategorySubscriptions() {
        return subcategorySubscriptions;
    }

    public void setSubcategorySubscriptions(List<SubcategorySubscription> subcategorySubscriptions) {
        this.subcategorySubscriptions = subcategorySubscriptions;
    }

    public List<TopicSubscription> getTopicSubscriptions() {
        return topicSubscriptions;
    }

    public void setTopicSubscriptions(List<TopicSubscription> topicSubscriptions) {
        this.topicSubscriptions = topicSubscriptions;
    }

    public List<Conversation> getConversations() {
        return conversations;
    }

    public void setConversations(List<Conversation> conversations) {
        this.conversations = conversations;
    }

    public List<ConversationToMember> getConversationToMemberList() {
    	return conversationToMemberList;
    }

    public void setConversationToMemberList(List<ConversationToMember> conversationToMemberList) {
    	this.conversationToMemberList = conversationToMemberList;
    }

    public Timestamp getRegisterTime() {
        return registerTime;
    }

    public void setRegisterTime(Timestamp registerTime) {
        this.registerTime = registerTime;
    }
    
    public Timestamp getActiveTime() {
        return activeTime;
    }

    public void setActiveTime(Timestamp activeTime) {
        this.activeTime = activeTime;
    }

    public String getPictureId() {
        return pictureId;
    }

    public void setPictureId(String pictureId) {
        this.pictureId = pictureId;
    }

    public List<ConversationSeenByMember> getConversationSeenByMembers() {
        return conversationSeenByMembers;
    }

    public void setConversationSeenByMembers(List<ConversationSeenByMember> conversationSeenByMembers) {
        this.conversationSeenByMembers = conversationSeenByMembers;
    }

    public List<TopicSeenByMember> getTopicSeenByMembers() {
        return topicSeenByMembers;
    }

    public void setTopicSeenByMembers(List<TopicSeenByMember> topicSeenByMembers) {
        this.topicSeenByMembers = topicSeenByMembers;
    }
    
}
