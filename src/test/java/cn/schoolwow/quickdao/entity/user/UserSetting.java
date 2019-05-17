package cn.schoolwow.quickdao.entity.user;

import cn.schoolwow.quickdao.annotation.*;

@Comment("用户设置表")
public class UserSetting {
    private long id;
    @Unique
    @NotNull
    @ForeignKey(table = User.class,field = "uid")
    private long userId;
    @ColumnType("varchar(1024)")
    private String setting;

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

    public String getSetting() {
        return setting;
    }

    public void setSetting(String setting) {
        this.setting = setting;
    }
}
