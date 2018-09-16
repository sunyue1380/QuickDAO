package com.schoolwow.quickdao;

import com.alibaba.fastjson.JSON;
import com.schoolwow.quickdao.entity.User;
import org.apache.commons.dbcp.BasicDataSource;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;

public class H2DAOTest {
    static BasicDataSource basicDataSource = new BasicDataSource();
    static QuickDAO quickDAO;
    static String packageName = "com.schoolwow.quickdao.entity";

    @BeforeClass
    public static void beforeClass(){
        basicDataSource.setDriverClassName("org.h2.Driver");
        basicDataSource.setUrl("jdbc:h2:c:/db/quickdao_h2.db;MODE=MySQL");
        quickDAO = new QuickDAO(basicDataSource,packageName);
    }

    @Test
    public void addNullQuery() {
        List<User> userList = quickDAO.query(User.class).addNullQuery("username").getList();
        System.out.println(userList);
    }
}