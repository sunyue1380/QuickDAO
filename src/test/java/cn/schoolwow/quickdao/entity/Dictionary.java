package cn.schoolwow.quickdao.entity;

import cn.schoolwow.quickdao.annotation.ColumnType;
import cn.schoolwow.quickdao.annotation.NotNull;
import cn.schoolwow.quickdao.annotation.Unique;

import java.util.Date;

public class Dictionary {
    private long id;
    @NotNull
    @Unique
    private String key;
    @ColumnType("varchar(10240)")
    @NotNull
    private String value;
    private Date updateTime;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }
}
