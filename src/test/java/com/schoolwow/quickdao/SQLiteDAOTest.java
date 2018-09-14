package com.schoolwow.quickdao;

import org.junit.BeforeClass;
import org.junit.Test;

public class SQLiteDAOTest extends CommonTest{
    @BeforeClass
    public static void beforeClass(){
        basicDataSource.setDriverClassName("org.sqlite.JDBC");
        basicDataSource.setUrl("jdbc:sqlite:c:/db/quickdao.db");
        quickDAO = new QuickDAO(basicDataSource,packageName);
    }

    @Test
    public void init(){

    }
}