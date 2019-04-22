package cn.schoolwow.quickdao.entity.user;

import cn.schoolwow.quickdao.annotation.Unique;

public class UserPlayList {
    /** 唯一标识 */
    private long id;
    /** 用户id */
    @Unique
    private long userId;
    /** 播单id */
    @Unique
    private long playlistId;

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
}
