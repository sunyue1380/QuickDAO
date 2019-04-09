package cn.schoolwow.quickdao.entity.user;

import cn.schoolwow.quickdao.annotation.Ignore;
import cn.schoolwow.quickdao.annotation.Unique;

import java.util.Date;

public class User {
    /**
     * 唯一标识
     */
    private long id;
    /**
     * 用户
     */
    @Unique
    private String username;
    /**
     * 密码
     */
    private String password;
    /**
     * 上次登录
     */
    private Date lastLogin;
    /**
     * 类型 (0-普通用户,1-管理员)
     */
    private int type;

    /**给用户颁发的token*/
    private String token;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Date getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(Date lastLogin) {
        this.lastLogin = lastLogin;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
