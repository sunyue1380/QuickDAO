package cn.schoolwow.quickdao.dao;

import cn.schoolwow.quickdao.QuickDAO;
import cn.schoolwow.quickdao.entity.logic.Comment;
import cn.schoolwow.quickdao.entity.logic.PlayHistory;
import cn.schoolwow.quickdao.entity.logic.PlayList;
import cn.schoolwow.quickdao.entity.logic.Video;
import cn.schoolwow.quickdao.entity.user.User;
import cn.schoolwow.quickdao.entity.user.UserPlayList;
import cn.schoolwow.quickdao.util.SQLUtil;
import cn.schoolwow.quickdao.util.ValidateUtil;
import com.alibaba.fastjson.JSON;
import org.apache.commons.dbcp.BasicDataSource;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.File;
import java.io.FileNotFoundException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

@RunWith(Parameterized.class)
public class DAOTest {
    Logger logger = LoggerFactory.getLogger(DAOTest.class);
    protected DataSource dataSource;
    protected DAO dao;

    @Parameterized.Parameters
    public static Collection prepareData(){
        String packageName = "cn.schoolwow.quickdao.entity";
//        String packageName = "cn.scrb.sunyue.entity";

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
                    .packageName(packageName)
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
    public void before() throws SQLException, FileNotFoundException {
        //TODO 数据库表存在时才执行
        Connection connection = dataSource.getConnection();
        String url = connection.getMetaData().getURL();
        logger.info("[数据源地址]{}",url);
        connection.setAutoCommit(false);
        Class[] classes = new Class[]{User.class,Comment.class, PlayList.class, UserPlayList.class, Video.class, PlayHistory.class};
        for(Class c:classes){
            String tableName = SQLUtil.classTableMap.get(c);
            if(url.contains("jdbc:mysql")||url.contains("jdbc:h2")){
                connection.prepareStatement("truncate table `"+tableName+"`;").executeUpdate();
            }else if(url.contains("jdbc:sqlite")){
                connection.prepareStatement("DELETE FROM `"+tableName+"`;").executeUpdate();
                connection.prepareStatement("DELETE FROM sqlite_sequence WHERE name = '"+tableName+"';").executeUpdate();
            }
        }

        Scanner scanner = new Scanner(new File("test.sql"));
        while(scanner.hasNext()){
            String sql = scanner.nextLine();
            if(ValidateUtil.isNotEmpty(sql)){
                if(sql.startsWith("c:")){
                    connection.prepareStatement(sql.substring(2)).executeUpdate();
                }else if(sql.startsWith("m:")&&url.contains("jdbc:mysql")){
                    connection.prepareStatement(sql.substring(2)).executeUpdate();
                }else if(sql.startsWith("h:")&&url.contains("jdbc:h2")){
                    connection.prepareStatement(sql.substring(2)).executeUpdate();
                }else if(sql.startsWith("s:")&&url.contains("jdbc:sqlite")){
                    connection.prepareStatement(sql.substring(2)).executeUpdate();
                }
            }
        }
        scanner.close();
        connection.commit();
        connection.close();
    }

    @Test
    public void aotoBuild() throws Exception {

    }


//    @Test
//    public void testLastInsertId() throws Exception {
//        Connection connection = dataSource.getConnection();
//        //设置事务并获取lastInsertId
//        connection.setAutoCommit(false);
//        connection.prepareStatement("INSERT INTO `quickdao`.`user` (`username`, `password`, `last_login`, `type`, `token`) VALUES ('sunyue2@schoolwow.cn', '123456789', '2019-01-05 19:48:29', '1', 'dc85b9187e894c4b8a9b8a22bc416098');").executeUpdate();
//        ResultSet rs = connection.prepareStatement("select last_insert_id();").executeQuery();
//        if(rs.next()){
//            System.out.println("id:"+rs.getLong(1));
//            System.out.println(JSON.toJSONString(dao.fetch(User.class,rs.getLong(1))));
//        }
//        rs.close();
//        connection.commit();
//        connection.close();
//    }

    @Test
    public void fetch() throws Exception {
        User user = dao.fetch(User.class,1l);
        logger.debug("[获取用户id为1的记录]:{}", JSON.toJSONString(dao.fetch(User.class,1)));
        Assert.assertNotNull(user);
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
    }

    @Test
    public void save() throws Exception {
        //根据UniqueKey更新
        User user = dao.fetch(User.class,1);
        user.setPassword("123456");
        long effect = dao.save(user);
        logger.debug("[把用户名为sunyue@schoolwow.cn的密码改为123456]:{}",effect);
        Assert.assertTrue(user.getId()==1);

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
//        Assert.assertTrue(newComment.getId()==2);
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

}