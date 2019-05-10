package cn.schoolwow.quickdao.entity.user;

import cn.schoolwow.quickdao.annotation.ColumnType;
import cn.schoolwow.quickdao.annotation.Comment;

import java.util.Date;

public class UserTalk {
    /**自增主键*/
    @Comment("自增主键")
    private long id;
    /**用户id*/
    @Comment("用户id")
    private long userId;
    /**说说id*/
    @Comment("说说id")
    private long talkId;
    /**类型 1-评论 2-点赞*/
    @Comment("类型 1-浏览 2-点赞 3-评论 4-转发")
    private int type;
    /**评论内容(最大长度40个字符)*/
    @Comment("评论内容")
    @ColumnType("varchar(140)")
    private String content;
    /**评论时间*/
    @Comment("评论时间")
    private Date time;

    private User user;

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

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Date getTime() {
        return time;
    }

    public void setTime(Date time) {
        this.time = time;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
