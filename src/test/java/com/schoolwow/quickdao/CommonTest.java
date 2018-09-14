package com.schoolwow.quickdao;

import com.alibaba.fastjson.JSON;
import com.schoolwow.quickdao.QuickDAO;
import com.schoolwow.quickdao.dao.Condition;
import com.schoolwow.quickdao.entity.NoUniqueKey;
import com.schoolwow.quickdao.entity.User;
import com.schoolwow.quickdao.entity.UserWrapper;
import org.apache.commons.dbcp.BasicDataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

public class CommonTest {
    static BasicDataSource basicDataSource = new BasicDataSource();
    static QuickDAO quickDAO;
    static String packageName = "com.schoolwow.quickdao.entity";

    @Before
    public void before(){
        User user = new User();
        user.setUsername("sunyue");
        user.setPassword("123456");
        user.setAge(27);
        user.setNickname("nickname");
        user.setAddress("ignore address");
        System.out.println("before save:"+quickDAO.save(user));
    }

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
    public void queryGroupBy() {
        Condition condition = quickDAO.query(User.class)
                .addColumn("count(username) as u_count")
                .groupBy("username")
                .having("username = 'sunyue'")
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
