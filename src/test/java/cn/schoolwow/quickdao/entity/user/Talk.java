package cn.schoolwow.quickdao.entity.user;

import cn.schoolwow.quickdao.annotation.ColumnType;
import cn.schoolwow.quickdao.annotation.Comment;
import cn.schoolwow.quickdao.annotation.Ignore;
import cn.schoolwow.quickdao.annotation.NotNull;

import java.util.Date;

/**说说*/
public class Talk {
    /**自增主键*/
    @Comment("自增主键")
    private long id;
    /**用户id*/
    @Comment("用户id")
    private long userId;
    /**说说内容*/
    @Comment("说说内容")
    @NotNull
    @ColumnType("varchar(4096)")
    private String content;
    /**图片路径*/
    @Comment("图片路径")
    private String picture;
    /**是否大图显示*/
    @Comment("是否大图显示")
    private boolean displayBig;
    /**发表时间*/
    @Comment("发表时间")
    @NotNull
    private Date publishTime;
    /**该说说是否可用*/
    @Comment("是否可用")
    private boolean enable;
    @Comment("是否是导入数据")
    private boolean isImported;
    /**用户信息*/
    private User user;
    /**浏览总数*/
    private long browserCount;
    /**点赞数*/
    private long zanCount;
    /**评论数*/
    private long commentCount;
    /**转发总数*/
    private long forwardCount;
    @Ignore
    private boolean hasZan;

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

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getPicture() {
        return picture;
    }

    public void setPicture(String picture) {
        this.picture = picture;
    }

    public boolean isDisplayBig() {
        return displayBig;
    }

    public void setDisplayBig(boolean displayBig) {
        this.displayBig = displayBig;
    }

    public Date getPublishTime() {
        return publishTime;
    }

    public void setPublishTime(Date publishTime) {
        this.publishTime = publishTime;
    }

    public boolean isEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    public boolean isImported() {
        return isImported;
    }

    public void setImported(boolean imported) {
        isImported = imported;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public long getBrowserCount() {
        return browserCount;
    }

    public void setBrowserCount(long browserCount) {
        this.browserCount = browserCount;
    }

    public long getZanCount() {
        return zanCount;
    }

    public void setZanCount(long zanCount) {
        this.zanCount = zanCount;
    }

    public long getCommentCount() {
        return commentCount;
    }

    public void setCommentCount(long commentCount) {
        this.commentCount = commentCount;
    }

    public long getForwardCount() {
        return forwardCount;
    }

    public void setForwardCount(long forwardCount) {
        this.forwardCount = forwardCount;
    }

    public boolean isHasZan() {
        return hasZan;
    }

    public void setHasZan(boolean hasZan) {
        this.hasZan = hasZan;
    }
}
