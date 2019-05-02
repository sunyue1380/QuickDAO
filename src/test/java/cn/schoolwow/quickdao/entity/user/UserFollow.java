package cn.schoolwow.quickdao.entity.user;

import cn.schoolwow.quickdao.annotation.ForeignKey;
import cn.schoolwow.quickdao.annotation.Unique;
import cn.schoolwow.quickdao.domain.ForeignKeyOption;

public class UserFollow {
    private long id;
    @Unique
    @ForeignKey(table = User.class,field = "uid",foreignKeyOption = ForeignKeyOption.NOACTION)
    private long userId;
    @Unique
    @ForeignKey(table = User.class,field = "uid",foreignKeyOption = ForeignKeyOption.SETNULL)
    private long followerId;
    private User user;
    private User followUser;

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

    public long getFollowerId() {
        return followerId;
    }

    public void setFollowerId(long followerId) {
        this.followerId = followerId;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public User getFollowUser() {
        return followUser;
    }

    public void setFollowUser(User followUser) {
        this.followUser = followUser;
    }
}
