package cn.schoolwow.quickdao.entity;

import cn.schoolwow.quickdao.annotation.NotNull;
import cn.schoolwow.quickdao.annotation.Unique;

import java.util.Date;

public class Video {
  /** 唯一标识 */
  private long id;
  /** 视频标题 */
  @NotNull
  private String title;
  /** 视频url */
  @Unique
  private String url;
  /** 集数 */
  private int episode;
  /** 视频发布时间 */
  private Date publishTime;
  /** 视频贴图地址 */
  private String picture;
  /** 视频简介 */
  private String description;
  /** 视频内容类型 (0-正片,1-预告,2-花絮) */
  private int contentType;
  /** 视频大小(byte) */
  private long size;
  /** 视频长度(秒) */
  private int seconds;
  /** 格式化时长 */
  private String secondsFormat;
  /** 是否是vip视频 */
  private boolean vip;
  /** 播单id */
  private long playlistId;

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public int getEpisode() {
    return episode;
  }

  public void setEpisode(int episode) {
    this.episode = episode;
  }

  public Date getPublishTime() {
    return publishTime;
  }

  public void setPublishTime(Date publishTime) {
    this.publishTime = publishTime;
  }

  public String getPicture() {
    return picture;
  }

  public void setPicture(String picture) {
    this.picture = picture;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public int getContentType() {
    return contentType;
  }

  public void setContentType(int contentType) {
    this.contentType = contentType;
  }

  public long getSize() {
    return size;
  }

  public void setSize(long size) {
    this.size = size;
  }

  public int getSeconds() {
    return seconds;
  }

  public void setSeconds(int seconds) {
    this.seconds = seconds;
  }

  public String getSecondsFormat() {
    return secondsFormat;
  }

  public void setSecondsFormat(String secondsFormat) {
    this.secondsFormat = secondsFormat;
  }

  public boolean isVip() {
    return vip;
  }

  public void setVip(boolean vip) {
    this.vip = vip;
  }

  public long getPlaylistId() {
    return playlistId;
  }

  public void setPlaylistId(long playlistId) {
    this.playlistId = playlistId;
  }
}
