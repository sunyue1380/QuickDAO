package cn.schoolwow.quickdao.entity.user;

import cn.schoolwow.quickdao.annotation.Unique;

import java.util.Date;

/**用户播单浏览历史*/
public class UserPlayListHistory {
    /**主键*/
    private long id;
    /**用户id*/
    @Unique
    private long userId;
    /**播单id*/
    @Unique
    private long playlistId;
    /**浏览时间*/
    private Date time;

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

    public long getPlaylistId() {
        return playlistId;
    }

    public void setPlaylistId(long playlistId) {
        this.playlistId = playlistId;
    }

    public Date getTime() {
        return time;
    }

    public void setTime(Date time) {
        this.time = time;
    }
}
