package com.schoolwow.quickdao;

import com.schoolwow.quickdao.entity.Order;
import com.schoolwow.quickdao.entity.User;
import com.schoolwow.quickdao.entity.UserOrder;
import org.apache.commons.dbcp.BasicDataSource;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

import java.util.Date;

public class DataInsertTest {
    static QuickDAO quickDAO;
    static String packageName = "com.schoolwow.quickdao.entity";

    @BeforeClass
    public static void beforeClass(){
//        basicDataSource.setDriverClassName("org.h2.Driver");
//        basicDataSource.setUrl("jdbc:h2:c:/db/quickdao_h2.db;MODE=MySQL");
        BasicDataSource basicDataSource = new BasicDataSource();
        basicDataSource.setDriverClassName("com.mysql.jdbc.Driver");
        basicDataSource.setUrl("jdbc:mysql://127.0.0.1:3306/quickdao");
        basicDataSource.setUsername("root");
        basicDataSource.setPassword("123456");
        quickDAO = new QuickDAO(basicDataSource,packageName);

        //插入一个用户
        User user = new User();
        user.setUsername("sunyue");
        user.setPassword("123456");
        user.setAge(27);
        user.setNickname("nickname");
        user.setAddress("ignore address");
        System.out.println("插入用户:"+quickDAO.save(user));

        //插入一个订单
        Order order = new Order();
        order.setName("洗面奶X1");
        order.setPrice(100);
        order.setProduceTime(new Date());
        System.out.println("插入订单:"+quickDAO.save(order));

        //插入用户订单表
        UserOrder userOrder = new UserOrder();
        userOrder.setUserId(user.getId());
        userOrder.setOrderId(order.getId());
        System.out.println("插入用户订单:"+quickDAO.save(userOrder));
    }

    @AfterClass
    public static void afterClass(){
        quickDAO.clear(User.class);
        quickDAO.clear(Order.class);
        quickDAO.clear(UserOrder.class);
    }
}
