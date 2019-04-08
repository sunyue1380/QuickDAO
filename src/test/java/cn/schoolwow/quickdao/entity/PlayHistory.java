package cn.schoolwow.quickdao.entity;

import cn.schoolwow.quickdao.annotation.NotNull;
import cn.schoolwow.quickdao.annotation.Unique;

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
    private long userId;
    /**
     * 视频id
     */
    @NotNull
    @Unique
    private long videoId;
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
}
