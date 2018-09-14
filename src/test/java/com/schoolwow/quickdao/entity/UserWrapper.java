package com.schoolwow.quickdao.entity;

import com.schoolwow.quickdao.annotation.ColumnType;
import com.schoolwow.quickdao.annotation.Ignore;
import com.schoolwow.quickdao.annotation.NotNull;
import com.schoolwow.quickdao.annotation.Unique;

public class UserWrapper {
    private Long id;
    @Unique
    @NotNull
    private String username;
    @ColumnType("varchar(16)")
    @NotNull
    @Unique
    private String password;

    @Unique
    private String nickname;

    private Integer age;

    @Ignore
    private String address;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
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

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
