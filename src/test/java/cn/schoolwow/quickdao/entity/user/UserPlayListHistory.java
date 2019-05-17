package cn.schoolwow.quickdao.entity.user;

import cn.schoolwow.quickdao.annotation.Comment;
import cn.schoolwow.quickdao.annotation.ForeignKey;
import cn.schoolwow.quickdao.annotation.Unique;
import cn.schoolwow.quickdao.entity.logic.PlayList;

import java.util.Date;

@Comment("用户播单浏览历史表")
public class UserPlayListHistory {
    /**主键*/
    private long id;
    /**用户id*/
    @Unique
    @ForeignKey(table = User.class,field = "uid")
    private long userId;
    /**播单id*/
    @Unique
    @ForeignKey(table = PlayList.class,field = "id")
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
