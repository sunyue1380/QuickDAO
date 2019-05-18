package cn.schoolwow.quickdao.entity.user;

import cn.schoolwow.quickdao.annotation.ColumnType;
import cn.schoolwow.quickdao.annotation.Comment;
import cn.schoolwow.quickdao.annotation.NotNull;
import cn.schoolwow.quickdao.annotation.Unique;

import java.util.Date;

/**举报表*/
public class Report {
    /**自增主键*/
    @Comment("自增主键")
    private long id;
    /**用户id*/
    @Comment("用户id")
    @Unique
    private long userId;
    /**说说id*/
    @Comment("说说id")
    @Unique
    private long talkId;
    /**举报原因*/
    @Comment("举报原因")
    @NotNull
    @ColumnType("varchar(140)")
    private String content;
    /**状态 0-待审批 1-通过 2-退回*/
    @Comment("状态 0-待审批 1-通过 2-退回")
    private int state;
    /**举报时间*/
    @Comment("举报时间")
    @NotNull
    private Date reportTime;
    /**联系方式*/
    @Comment("联系方式")
    private String contact;
    private User user;
    private Talk talk;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public long getTalkId() {
        return talkId;
    }

    public void setTalkId(long talkId) {
        this.talkId = talkId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public Date getReportTime() {
        return reportTime;
    }

    public void setReportTime(Date reportTime) {
        this.reportTime = reportTime;
    }

    public String getContact() {
        return contact;
    }

    public void setContact(String contact) {
        this.contact = contact;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Talk getTalk() {
        return talk;
    }

    public void setTalk(Talk talk) {
        this.talk = talk;
    }
}
