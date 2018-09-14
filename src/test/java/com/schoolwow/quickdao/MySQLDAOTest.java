package com.schoolwow.quickdao;

import com.alibaba.fastjson.JSON;
import com.schoolwow.quickdao.dao.Condition;
import com.schoolwow.quickdao.entity.NoUniqueKey;
import com.schoolwow.quickdao.entity.User;
import com.schoolwow.quickdao.entity.UserWrapper;
import org.apache.commons.dbcp.BasicDataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;

public class MySQLDAOTest extends CommonTest{
    @BeforeClass
    public static void beforeClass(){
        basicDataSource.setDriverClassName("com.mysql.jdbc.Driver");
        basicDataSource.setUrl("jdbc:mysql://127.0.0.1:3306/quickdao");
        basicDataSource.setUsername("root");
        basicDataSource.setPassword("123456");
        quickDAO = new QuickDAO(basicDataSource,packageName);
    }
}