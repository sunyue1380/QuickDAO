package com.schoolwow.quickdao;

import org.junit.BeforeClass;
import org.junit.Test;

public class H2DAOTest extends CommonTest{
    @BeforeClass
    public static void beforeClass(){
        basicDataSource.setDriverClassName("org.h2.Driver");
        basicDataSource.setUrl("jdbc:h2:c:/db/quickdao_h2.db;MODE=MySQL");
        quickDAO = new QuickDAO(basicDataSource,packageName);
    }

    @Test
    public void init(){

    }
}