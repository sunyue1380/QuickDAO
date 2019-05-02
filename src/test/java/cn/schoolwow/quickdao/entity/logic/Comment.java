package cn.schoolwow.quickdao.entity.logic;

import cn.schoolwow.quickdao.annotation.ForeignKey;

import java.util.Date;

public class Comment {
  /** 唯一标识 */
  private long id;
  /** 头像地址 */
  private String avatar;
  /** 评论者 */
  private String author;
  /** 发布时间 */
  private Date publishTime;
  /** 评论内容 */
  private String content;
  /** 视频id */
  @ForeignKey(table = Video.class,field = "id")
  private long videoId;

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public String getAvatar() {
    return avatar;
  }

  public void setAvatar(String avatar) {
    this.avatar = avatar;
  }

  public String getAuthor() {
    return author;
  }

  public void setAuthor(String author) {
    this.author = author;
  }

  public Date getPublishTime() {
    return publishTime;
  }

  public void setPublishTime(Date publishTime) {
    this.publishTime = publishTime;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public long getVideoId() {
    return videoId;
  }

  public void setVideoId(long videoId) {
    this.videoId = videoId;
  }
}
