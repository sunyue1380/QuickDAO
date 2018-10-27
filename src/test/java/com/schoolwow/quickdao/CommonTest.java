package com.schoolwow.quickdao;

import com.alibaba.fastjson.JSON;
import com.schoolwow.quickdao.QuickDAO;
import com.schoolwow.quickdao.dao.Condition;
import com.schoolwow.quickdao.dao.SubCondition;
import com.schoolwow.quickdao.entity.*;
import org.apache.commons.dbcp.BasicDataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Date;
import java.util.List;

public class CommonTest {
    static BasicDataSource basicDataSource = new BasicDataSource();
    static QuickDAO quickDAO;
    static String packageName = "com.schoolwow.quickdao.entity";

    @BeforeClass
    public static void beforeClass(){
//        basicDataSource.setDriverClassName("org.h2.Driver");
//        basicDataSource.setUrl("jdbc:h2:c:/db/quickdao_h2.db;MODE=MySQL");

        basicDataSource.setDriverClassName("com.mysql.jdbc.Driver");
        basicDataSource.setUrl("jdbc:mysql://127.0.0.1:3306/quickdao");
        basicDataSource.setUsername("root");
        basicDataSource.setPassword("123456");
        quickDAO = new QuickDAO(basicDataSource,packageName);

        quickDAO = new QuickDAO(basicDataSource,packageName);
    }

//    @Before
//    public void before(){
//        //插入一个用户
//        User user = new User();
//        user.setUsername("sunyue");
//        user.setPassword("123456");
//        user.setAge(27);
//        user.setNickname("nickname");
//        user.setAddress("ignore address");
//        System.out.println("插入用户:"+quickDAO.save(user));
//
//        //插入一个订单
//        Order order = new Order();
//        order.setName("洗面奶X1");
//        order.setPrice(100);
//        order.setProduceTime(new Date());
//        System.out.println("插入订单:"+quickDAO.save(order));
//
//        //插入用户订单表
//        UserOrder userOrder = new UserOrder();
//        userOrder.setUserId(user.getId());
//        userOrder.setOrderId(order.getId());
//        System.out.println("插入用户订单:"+quickDAO.save(userOrder));
//    }

    @After
    public void after(){
        quickDAO.clear(User.class);
    }

    @Test
    public void fetch() {
        User user = quickDAO.fetch(User.class,1);
        System.out.println("fetch:"+ JSON.toJSONString(user));
    }

    @Test
    public void fetchProperty() {
        User user = quickDAO.fetch(User.class,"username","sunyue");
        System.out.println("fetchProperty:"+JSON.toJSONString(user));
    }

    @Test
    public void query() {
        Condition condition = quickDAO.query(User.class)
                .addQuery("username","sunyue")
                .addQuery("age",">",10)
                .addQuery("nickname = 'nickname'")
                .addInQuery("id",new Long[]{1L})
                .addNullQuery("address")
                .done();
        long count = condition.count();
        System.out.println("count:"+count);

        List<User> useList = condition.getList();
        System.out.println("userList:"+JSON.toJSONString(useList));

        List<Long> ids = condition.getValueList(Long.class,"id");
        System.out.println("ids:"+JSON.toJSONString(ids));
    }

    @Test
    public void queryDelete() {
        Condition condition = quickDAO.query(User.class)
                .addQuery("username","sunyue")
                .done();
        long effect = condition.delete();
        System.out.println("queryDelete"+effect);
    }

    @Test
    public void querySubCondition() {
        Condition<Order> condition = quickDAO.query(Order.class);
        SubCondition<UserOrder> subCondition = condition.joinTable(UserOrder.class,"id","order_id")
                .addQuery("user_id",1);
        System.out.println(JSON.toJSONString(condition.getList()));
    }

    @Test
    public void queryGroupBy() {
        Condition condition = quickDAO.query(User.class)
                .addColumn("count(username) as u_count")
//                .groupBy("username")
//                .having("username = 'sunyue'")
                .done();
        List<Long> list = condition.getValueList(Long.class,"u_count");
        System.out.println("queryGroupBy:"+list.get(0));
    }

    @Test
    public void queryOrderByLimit() {
        Condition condition = quickDAO.query(User.class)
                .addQuery("username","sunyue")
                .orderByDesc("id")
                .limit(0,10)
                .done();
        List<User> userList = condition.getList();
        System.out.println("queryOrderByLimit:"+JSON.toJSONString(userList));
    }

    @Test
    public void save() {
        User user = new User();
        user.setUsername("sunyue");
        user.setPassword("aa1122334455");
        user.setAge(26);
        user.setNickname("ice");
        long effect = quickDAO.save(user);
        System.out.println("save:"+effect);
        System.out.println("userId:"+user.getId());
    }

    @Test
    public void saveUpdate() {
        User user = new User();
        user.setId(10);
        user.setAge(26);
        user.setNickname("ice");
        long effect = quickDAO.save(user);
        System.out.println("saveUpdate:"+effect);
    }

    @Test
    public void saveWrapper() {
        UserWrapper userWrapper = new UserWrapper();

        userWrapper.setAge(28);
        userWrapper.setNickname("ice");
        long effect = quickDAO.save(userWrapper);
        System.out.println("saveWrapper:"+effect);
    }

    @Test
    public void saveNoUniqueKey() {
        NoUniqueKey noUniqueKey = new NoUniqueKey();
        noUniqueKey.setProperty1("property1");
        noUniqueKey.setProperty2("property2");
        long effect = quickDAO.save(noUniqueKey);
        System.out.println("saveNoUniqueKey:"+effect);
    }

    @Test
    public void saveMulti() {
        User[] users = new User[100];
        for(int i=0;i<users.length;i++){
            users[i] = new User();
            users[i].setUsername("user"+i);
            users[i].setPassword("password"+i);
            users[i].setNickname("nickanme:"+i);
            users[i].setAge(i+20);
            users[i].setAddress("address"+i);
        }
        long effect = quickDAO.save(users);
        System.out.println("saveMulti:"+effect);
    }

    @Test
    public void update() {
        long effect = quickDAO.query(User.class)
                .addUpdate("username","sunyue_update")
                .addQuery("password","123456")
                .update();
        System.out.println("update:"+effect);
    }

    @Test
    public void delete() {
        long effect = quickDAO.delete(User.class,1L);
        System.out.println("delete:"+effect);
    }

    @Test
    public void deleteProperty() {
        long effect = quickDAO.delete(User.class,"username","sunyue");
        System.out.println("deleteProperty:"+effect);
    }
}
