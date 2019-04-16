package cn.schoolwow.quickdao.entity.logic;

import cn.schoolwow.quickdao.annotation.ColumnType;
import cn.schoolwow.quickdao.annotation.NotNull;
import cn.schoolwow.quickdao.annotation.Unique;

import java.util.Date;

public class PlayList {
  /** 唯一标识 */
  private long id;
  /** 播单名 */
  @NotNull
  private String name;
  /** 播单对应视频网站 */
  private String tv;
  /** 播单专辑页 */
  @Unique
  private String url;
  /** 播单类型 (0-综艺 1-剧集 2-其他) */
  private int type;
  /** 搜索关键字 */
  private String searchWord;
  /** 播单图片 */
  private String picture;
  /** 播单描述 */
  @ColumnType("varchar(512)")
  private String description;
  /** 播单订阅数 */
  private long subscribeCount;
  /** 播单是否完结 */
  private boolean end;
  /**播单搜索来源(系统抓取为system)*/
  private String searchSource;
  /**上次解析时间*/
  private Date lastAnalyzeTime;
  /**豆瓣评分*/
  private float doubanRank;
  /**豆瓣主页地址*/
  private String doubanUrl;
  /**更新时间*/
  private Date updateTime;

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getTv() {
    return tv;
  }

  public void setTv(String tv) {
    this.tv = tv;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public int getType() {
    return type;
  }

  public void setType(int type) {
    this.type = type;
  }

  public String getSearchWord() {
    return searchWord;
  }

  public void setSearchWord(String searchWord) {
    this.searchWord = searchWord;
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

  public long getSubscribeCount() {
    return subscribeCount;
  }

  public void setSubscribeCount(long subscribeCount) {
    this.subscribeCount = subscribeCount;
  }

  public boolean isEnd() {
    return end;
  }

  public void setEnd(boolean end) {
    this.end = end;
  }

  public String getSearchSource() {
    return searchSource;
  }

  public void setSearchSource(String searchSource) {
    this.searchSource = searchSource;
  }

  public Date getLastAnalyzeTime() {
    return lastAnalyzeTime;
  }

  public void setLastAnalyzeTime(Date lastAnalyzeTime) {
    this.lastAnalyzeTime = lastAnalyzeTime;
  }

  public float getDoubanRank() {
    return doubanRank;
  }

  public void setDoubanRank(float doubanRank) {
    this.doubanRank = doubanRank;
  }

  public String getDoubanUrl() {
    return doubanUrl;
  }

  public void setDoubanUrl(String doubanUrl) {
    this.doubanUrl = doubanUrl;
  }

  public Date getUpdateTime() {
    return updateTime;
  }

  public void setUpdateTime(Date updateTime) {
    this.updateTime = updateTime;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    PlayList playList = (PlayList) o;

    return url.equals(playList.url);
  }

  @Override
  public int hashCode() {
    return url.hashCode();
  }
}
