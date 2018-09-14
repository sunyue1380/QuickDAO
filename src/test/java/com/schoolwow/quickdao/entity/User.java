package com.schoolwow.quickdao.entity;

import com.schoolwow.quickdao.annotation.*;

public class User {
    private long id;
    @Unique
    @NotNull
    private String username;
    @ColumnType("varchar(16)")
    @NotNull
    @Unique
    private String password;

    @Unique
    private String nickname;

    private int age;

    @Ignore
    private String address;

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

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
