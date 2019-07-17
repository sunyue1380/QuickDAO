package cn.schoolwow.quickdao.dao;

import cn.schoolwow.quickdao.QuickDAOTest;
import cn.schoolwow.quickdao.entity.logic.Comment;
import cn.schoolwow.quickdao.entity.logic.PlayList;
import cn.schoolwow.quickdao.entity.user.User;
import cn.schoolwow.quickdao.entity.user.UserPlayList;
import com.alibaba.fastjson.JSON;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.List;

@RunWith(Parameterized.class)
public class DAOTest extends QuickDAOTest{
    Logger logger = LoggerFactory.getLogger(DAOTest.class);

    public DAOTest(DAO dao) {
        super(dao);
    }

    @Test
    public void exist(){
        initializeUser();
        User user = new User();
        user.setUsername("sunyue@schoolwow.cn");
        user.setPassword("123456");
        boolean exist = dao.exist(user);
        Assert.assertEquals(true,exist);
    }

    @Test
    public void fetch(){
        initializeUser();
        {
            User user = dao.fetch(User.class,1l);
            logger.debug("[获取用户id为1的记录]:{}", JSON.toJSONString(user));
            Assert.assertNotNull("获取用户id为1的记录失败",user);
        }
        {
            User user = dao.fetch(User.class,3);
            logger.debug("[获取用户id为3的记录]:{}", JSON.toJSONString(user));
            Assert.assertNull("获取用户id为3的记录失败",user);
        }
    }

    @Test
    public void fetchWithProperty(){
        initializeUser();
        User user = dao.fetch(User.class,"username","sunyue@schoolwow.cn");
        logger.debug("[获取用户名为sunyue@schoolwow.cn的记录]:{}",JSON.toJSONString(user));
        Assert.assertNotNull(user);
    }

    @Test
    public void fetchList(){
        initializeUser();
        {
            List<User> userList = dao.fetchList(User.class,"password","123456789");
            logger.debug("[获取用户密码为为123456789的记录]:{}",userList);
            Assert.assertTrue(userList!=null&&userList.size()==2);
        }
        {
            List<User> userList = dao.fetchList(User.class,"lastLogin",null);
            logger.debug("[获取lastLogin为null的记录]:{}",userList);
            Assert.assertTrue(userList!=null&&userList.size()==0);
        }
    }

    @Test
    public void save(){
        initializeUser();
        initializeComment();
        {
            Object o = null;
            Assert.assertTrue("保存空对象应该返回0",dao.save(o)==0);
        }
        //根据UniqueKey更新
        {
            User user = new User();
            user.setUsername("sunyue@schoolwow.cn");
            user.setPassword("123456");
            long effect = dao.save(user);
            logger.debug("[把用户名为sunyue@schoolwow.cn的密码改为123456]:{}",effect);
            Assert.assertEquals("123456",dao.fetch(User.class,"username","sunyue@schoolwow.cn").getPassword());
        }
        //根据id更新
        {
            Comment comment = new Comment();
            comment.setId(1);
            comment.setAuthor("sunyue");
            long effect = dao.save(comment);
            logger.debug("[更新id为1的评论,设置author为sunyue]影响:{}",effect);
            Assert.assertEquals("sunyue",dao.fetch(Comment.class,1).getAuthor());
        }
        //添加一条新的Comment记录
        {
            Comment newComment = new Comment();
            newComment.setAuthor("_前端农民工");
            newComment.setAvatar("https://r1.ykimg.com/0510000058CB4CA9429D3E61C9029307");
            newComment.setPublishTime(new Date());
            newComment.setContent("看到杨颖就想跳过");
            newComment.setVideoId(1);
            long effect = dao.save(newComment);
            logger.debug("[添加一条新的评论]影响:{},id:{}",effect,newComment.getId());
            Assert.assertNotNull(dao.fetch(Comment.class,"author","_前端农民工"));
        }
    }

    @Test
    public void saveArray(){
        initializeUser();
        initializeComment();
        User[] users = {dao.fetch(User.class,1),dao.fetch(User.class,2)};
        for(int i=0;i<users.length;i++){
            users[i].setPassword("123456");
        }
        long effect = dao.save(users);
        logger.info("[批量将用户id为1和2的用户密码更改为123456]影响:{}",effect);
        long count = dao.query(User.class).addQuery("password","123456").count();
        Assert.assertEquals(2,count);

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
        effect = dao.save(comments);
        logger.debug("[批量更新评论]影响:{}",effect);
        count = dao.query(Comment.class).count();
        Assert.assertEquals(2,count);
    }

    @Test
    public void delete(){
        initializeUser();
        long effect = dao.delete(User.class,1);
        logger.info("[删除用户id为1的记录]影响:{}",effect);
        User user = dao.fetch(User.class,1);
        Assert.assertNull(user);
    }

    @Test
    public void deleteProperty(){
        initializeUser();
        long effect = dao.delete(User.class,"username","sunyue@schoolwow.cn");
        logger.info("[删除用户名为sunyue@schoolwow.cn的记录]影响:{}",effect);
        User user = dao.fetch(User.class,"username","sunyue@schoolwow.cn");
        Assert.assertNull(user);
    }

    @Test
    public void clear(){
        initializeUser();
        long effect = dao.clear(User.class);
        logger.info("[清空User表]影响:{}",effect);
        long count = dao.query(User.class).count();
        Assert.assertEquals(0,count);
    }

    @Test
    public void testTransaction(){
        initializeUserPlaylist();
        //用户订阅播单,播单订阅数加1,同时插入一条用户播单订阅记录
        {
            dao.startTransaction();
            UserPlayList userPlayList = new UserPlayList();
            userPlayList.setUserId(1);
            userPlayList.setPlaylistId(2);
            long effect = dao.save(userPlayList);
            logger.info("[插入用户播单订阅记录]实体:{},影响:{}",JSON.toJSONString(userPlayList),effect);
            //更新用户播单订阅数
            PlayList playList = dao.fetch(PlayList.class,1);
            playList.setSubscribeCount(playList.getSubscribeCount()+1);
            effect = dao.save(playList);
            logger.info("[更新播单订阅数]实体:{},影响:{}",JSON.toJSONString(playList),effect);

            dao.commit();
            dao.endTransaction();
            long count = dao.query(UserPlayList.class).addQuery("userId",1).count();
            logger.info("[检查用户播单订阅数]{}",count);
            Assert.assertEquals(2,count);
        }
        //开启事务插入一条订阅记录后回滚
        {
            dao.startTransaction();
            UserPlayList userPlayList = new UserPlayList();
            userPlayList.setUserId(2);
            userPlayList.setPlaylistId(1);
            logger.info("[插入用户播单订阅记录]实体:{},影响:{}",JSON.toJSONString(userPlayList),dao.save(userPlayList));
            dao.rollback();
            dao.endTransaction();
            long count = dao.query(UserPlayList.class).addQuery("userId",2).count();
            logger.info("[检查用户播单订阅数]{}",count);
            Assert.assertEquals(0,count);
        }
    }
}