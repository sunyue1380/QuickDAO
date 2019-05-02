package cn.schoolwow.quickdao.entity.logic;

import cn.schoolwow.quickdao.annotation.ForeignKey;
import cn.schoolwow.quickdao.annotation.NotNull;
import cn.schoolwow.quickdao.annotation.Unique;
import cn.schoolwow.quickdao.entity.user.User;

import java.util.Date;

public class PlayHistory {
    /**
     * 唯一标识
     */
    private long id;
    /**
     * 用户id
     */
    @NotNull
    @Unique
    @ForeignKey(table = User.class,field = "uid")
    private long userId;

    private User user;
    /**
     * 视频id
     */
    @NotNull
    @Unique
    @ForeignKey(table = Video.class,field = "id")
    private long videoId;

    private Video video;
    /**
     * 已观看时间(单位:秒)
     */
    private int startTime;
    /**
     * 已观看时间(X分X秒)
     */
    private String startTimeFormat;
    /**更新时间*/
    @NotNull
    private Date updateTime;

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

    public long getVideoId() {
        return videoId;
    }

    public void setVideoId(long videoId) {
        this.videoId = videoId;
    }

    public int getStartTime() {
        return startTime;
    }

    public void setStartTime(int startTime) {
        this.startTime = startTime;
    }

    public String getStartTimeFormat() {
        return startTimeFormat;
    }

    public void setStartTimeFormat(String startTimeFormat) {
        this.startTimeFormat = startTimeFormat;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Video getVideo() {
        return video;
    }

    public void setVideo(Video video) {
        this.video = video;
    }
}
