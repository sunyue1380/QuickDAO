package cn.schoolwow.quickdao.domain;

import cn.schoolwow.quickdao.annotation.Comment;

@Comment("Banner")
public class Banner {
    private long id;
    /**类型1-答题 2-web地址 3-新闻详情*/
    private int type;
    private String data;
    private String image;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }
}
