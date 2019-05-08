package cn.schoolwow.quickdao.dao;

import cn.schoolwow.quickdao.QuickDAO;
import cn.schoolwow.quickdao.entity.WatchLater;
import cn.schoolwow.quickdao.entity.logic.Comment;
import cn.schoolwow.quickdao.entity.logic.PlayHistory;
import cn.schoolwow.quickdao.entity.logic.PlayList;
import cn.schoolwow.quickdao.entity.logic.Video;
import cn.schoolwow.quickdao.entity.user.User;
import cn.schoolwow.quickdao.entity.user.UserFollow;
import cn.schoolwow.quickdao.entity.user.UserPlayList;
import cn.schoolwow.quickdao.util.SQLUtil;
import cn.schoolwow.quickdao.util.ValidateUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.dbcp.BasicDataSource;
import org.h2.store.fs.FileUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.java2d.pipe.SpanShapeRenderer;

import javax.sql.DataSource;
import java.io.File;
import java.io.FileNotFoundException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@RunWith(Parameterized.class)
public class DAOTest {
    Logger logger = LoggerFactory.getLogger(DAOTest.class);
    protected DataSource dataSource;
    protected DAO dao;

    @Parameterized.Parameters
    public static Collection prepareData(){
        BasicDataSource mysqlDataSource = new BasicDataSource();
        mysqlDataSource.setDriverClassName("com.mysql.jdbc.Driver");
        mysqlDataSource.setUrl("jdbc:mysql://127.0.0.1:3306/quickdao");
        mysqlDataSource.setUsername("root");
        mysqlDataSource.setPassword("123456");

        BasicDataSource sqliteDataSource = new BasicDataSource();
        sqliteDataSource.setDriverClassName("org.sqlite.JDBC");
        sqliteDataSource.setUrl("jdbc:sqlite:d:/db/quickdao_sqlite.db");

        BasicDataSource h2DataSource = new BasicDataSource();
        h2DataSource.setDriverClassName("org.h2.Driver");
        h2DataSource.setUrl("jdbc:h2:d:/db/quickdao_h2.db;mode=MYSQL");

        //各种数据库产品
//        DataSource[] dataSources = {h2DataSource};
        DataSource[] dataSources = {mysqlDataSource,sqliteDataSource,h2DataSource};
        Object[][] data = new Object[dataSources.length][2];
        for(int i=0;i<dataSources.length;i++){
            data[i][0] = QuickDAO.newInstance().dataSource(dataSources[i])
                    .packageName("cn.schoolwow.quickdao.entity")
                    .packageName("cn.schoolwow.quickdao.domain","d")
                    .autoCreateTable(false)
//                    .ignoreClass(WatchLater.class)
//                    .ignorePackageName("cn.schoolwow.quickdao.entity.logic")
                    .build();
            data[i][1] = dataSources[i];
        };
        return Arrays.asList(data);
    }

    public DAOTest(DAO dao,DataSource dataSource){
        this.dao = dao;
        this.dataSource = dataSource;
    }

    @Before
    public void before() throws FileNotFoundException, ClassNotFoundException {
        File file = new File("data.json");
        Scanner scanner = new Scanner(file);
        StringBuilder sb = new StringBuilder();
        while(scanner.hasNext()){
            sb.append(scanner.nextLine());
        }
        scanner.close();
        JSONArray array = JSON.parseArray(sb.toString());
        for(int i=0;i<array.size();i++){
            JSONObject o = array.getJSONObject(i);
            Set<Map.Entry<String,String>> entrySet = SQLUtil.classTableMap.entrySet();
            for(Map.Entry<String,String> entry:entrySet){
                if(entry.getValue().equals(o.getString("table"))){
                    Class _class = Class.forName(entry.getKey());
                    dao.drop(_class);
                    dao.create(_class);
                    List list = o.getJSONArray("rows").toJavaList(_class);
                    dao.save(list);
                    break;
                }
            }
        }
    }

    @Test
    public void aotoBuild() throws Exception {

    }

    @Test
    public void fetch() throws Exception {
        User user = dao.fetch(User.class,1l);
        logger.debug("[获取用户id为1的记录]:{}", JSON.toJSONString(user));
        Assert.assertNotNull(user);

        //fetchNull
        user = dao.fetch(User.class,3);
        logger.debug("[获取用户id为3的记录]:{}", JSON.toJSONString(user));
        Assert.assertNull(user);
    }

    @Test
    public void fetchWithProperty() throws Exception {
        User user = dao.fetch(User.class,"username","sunyue@schoolwow.cn");
        logger.debug("[获取用户名为sunyue@schoolwow.cn的记录]:{}",JSON.toJSONString(user));
        Assert.assertNotNull(user);
    }

    @Test
    public void fetchList() throws Exception {
        List<User> userList = dao.fetchList(User.class,"password","123456789");
        logger.debug("[获取用户密码为为123456789的记录]:{}",userList);
        Assert.assertTrue(userList!=null&&userList.size()==2);
        userList = dao.fetchList(User.class,"lastLogin",null);
        logger.debug("[获取lastLogin为null的记录]:{}",userList);
        Assert.assertTrue(userList!=null&&userList.size()==0);
    }

    @Test
    public void save() throws Exception {
        Object o = null;
        Assert.assertTrue("保存空对象应该返回0",dao.save(o)==0);

        //根据UniqueKey更新
        User user = dao.fetch(User.class,1);
        user.setPassword("123456");
        long effect = dao.save(user);
        logger.debug("[把用户名为sunyue@schoolwow.cn的密码改为123456]:{}",effect);
        Assert.assertTrue(user.getUid()==1);

        //根据id更新
        Comment comment = dao.fetch(Comment.class,1);
        comment.setAuthor("sunyue");
        dao.save(comment);
        logger.debug("[更新id为1的评论,设置author为sunyue]{}",effect);

        //添加一条新的Comment记录
        Comment newComment = new Comment();
        newComment.setAuthor("_前端农民工");
        newComment.setAvatar("https://r1.ykimg.com/0510000058CB4CA9429D3E61C9029307");
        newComment.setPublishTime(new Date());
        newComment.setContent("看到杨颖就想跳过");
        newComment.setVideoId(1);
        effect = dao.save(newComment);
        logger.debug("[添加一条新的评论]影响:{},id:{}",effect,newComment.getId());
        Assert.assertTrue(newComment.getId()>1);
    }

    @Test
    public void saveArray() throws Exception {
        User[] users = {dao.fetch(User.class,1),dao.fetch(User.class,2)};
        for(int i=0;i<users.length;i++){
            users[i].setPassword("123456");
        }
        logger.info("[批量将用户id为1和2的用户密码更改为123456]影响:{}",dao.save(users));

        //根据id更新
        Comment comment = dao.fetch(Comment.class,1);
        comment.setAuthor("sunyue");

        //添加一条新的Comment记录
        Comment newComment = new Comment();
        newComment.setAuthor("_前端农民工");
        newComment.setAvatar("https://r1.ykimg.com/0510000058CB4CA9429D3E61C9029307");
        newComment.setPublishTime(new Date());
        newComment.setContent("看到杨颖就想跳过");
        newComment.setVideoId(1);

        Comment[] comments = {comment,newComment};
        logger.debug("[批量更新评论]影响:{}",dao.save(comments));
    }

    @Test
    public void delete() throws Exception {
        logger.info("[删除用户id为1的记录]影响:{}",dao.delete(User.class,1));
    }

    @Test
    public void deleteProperty() throws Exception {
        logger.info("[删除用户名为sunyue@schoolwow.cn的记录]影响:{}",dao.delete(User.class,"username","sunyue@schoolwow.cn"));
    }

    @Test
    public void clear() throws Exception {
        logger.info("[清空User表]影响:{}",dao.clear(User.class));
    }

    @Test
    public void testTransaction() throws Exception {
        //用户订阅播单,播单订阅数加1,同时插入一条用户播单订阅记录
        dao.startTransaction();
        UserPlayList userPlayList = new UserPlayList();
        userPlayList.setUserId(1);
        userPlayList.setPlaylistId(2);
        logger.info("[插入用户播单订阅记录]实体:{},影响:{}",JSON.toJSONString(userPlayList),dao.save(userPlayList));
        //更新用户播单订阅数
        PlayList playList = dao.fetch(PlayList.class,1);
        playList.setSubscribeCount(playList.getSubscribeCount()+1);
        logger.info("[更新播单订阅数]实体:{},影响:{}",JSON.toJSONString(playList),dao.save(playList));
        dao.commit();
        dao.endTransaction();
        long count = dao.query(UserPlayList.class).addQuery("userId",1).count();
        logger.info("[检查用户播单订阅记录]count:{}",count);
        Assert.assertTrue(count==2);

        //开启事务插入一条订阅记录后回滚
        dao.startTransaction();
        userPlayList = new UserPlayList();
        userPlayList.setUserId(2);
        userPlayList.setPlaylistId(1);
        logger.info("[插入用户播单订阅记录]实体:{},影响:{}",JSON.toJSONString(userPlayList),dao.save(userPlayList));
        dao.rollback();
        dao.endTransaction();
        count = dao.query(UserPlayList.class).addQuery("userId",2).count();
        logger.info("[检查用户播单订阅记录]count:{}",count);
        Assert.assertTrue(count==0);
    }
}