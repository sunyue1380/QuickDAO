package cn.schoolwow.quickdao.condition;

import cn.schoolwow.quickdao.QuickDAOTest;
import cn.schoolwow.quickdao.dao.DAO;
import cn.schoolwow.quickdao.domain.PageVo;
import cn.schoolwow.quickdao.entity.logic.PlayHistory;
import cn.schoolwow.quickdao.entity.logic.PlayList;
import cn.schoolwow.quickdao.entity.logic.Project;
import cn.schoolwow.quickdao.entity.logic.Video;
import cn.schoolwow.quickdao.entity.user.*;
import cn.schoolwow.quickdao.util.SQLUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
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
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

@RunWith(Parameterized.class)
public class ConditionTest extends QuickDAOTest{
    Logger logger = LoggerFactory.getLogger(ConditionTest.class);
    public ConditionTest(DAO dao) {
        super(dao);
    }

    @Test
    public void testAddBetweenQuery() throws Exception {
        Condition<User> condition = dao.query(User.class)
                .addBetweenQuery("uid",1,2);
        List<User> userList = condition.getList();
        Assert.assertEquals(2,userList.size());
    }

    @Test
    public void testMultiSubConditionJoinTable() throws Exception {
        List<Report> reportList = dao.query(Report.class)
                .joinTable(User.class,"userId","uid")
                .done()
                .joinTable(Talk.class,"talkId","id")
                .joinTable(User.class,"userId","uid")
                .joinTable(Project.class,"project","key")
                .doneSubCondition()
                .done()
                .getCompositList();
        logger.info("[子表关联查询]结果:{}",JSON.toJSONString(reportList));
        Assert.assertEquals(1,reportList.size());
    }


    @Test
    public void testSubConditionJoinTable() throws Exception {
        List<Report> reportList = dao.query(Talk.class)
                .joinTable(User.class,"userId","uid")
                .addQuery("uid",1)
                .joinTable(Project.class,"project","key")
                .doneSubCondition()
                .done()
                .getCompositList();
        logger.info("[子表关联查询]结果:{}",JSON.toJSONString(reportList));
        Assert.assertEquals(1,reportList.size());
    }

    @Test
    public void testOuterJoinTable() throws Exception {
        //查询用户id为1所订阅的播单
        List<PlayList> playListList= dao.query(PlayList.class)
                .joinTable(UserPlayList.class,"id","playlist_id")
                .leftJoin()
                .addQuery("user_id",1)
                .done()
                .getList();
        logger.info("[左外连接查询][查询用户id为1所订阅的播单]查询结果:{}", JSON.toJSON(playListList));
        Assert.assertTrue(playListList.size()==1);
    }

    @Test
    public void testClone() throws Exception {
        Condition<UserTalk> condition = dao.query(UserTalk.class)
                .joinTable(User.class,"userId","uid")
                .addQuery("username","@")
                .addQuery("type",">=",1)
                .addInQuery("token",new String[]{"7a746f17a9bf4903b09b617135152c71","9204d99472c04ce7abf1bcb9773b0d49"})
                .addNotNullQuery("lastLogin")
                .orderByDesc("uid")
                .done()
                .page(1,10);
        Condition<User> cloneCondition = condition.clone();
        logger.info("[clone]{}",cloneCondition);
        Assert.assertTrue(condition.count()==cloneCondition.count());
    }

    @Test
    public void testAddJSONObjectQuery() throws Exception {
        JSONObject queryCondition = new JSONObject();
        queryCondition.put("username","@");
        queryCondition.put("typeStart",1);
        queryCondition.put("tokenIN",new String[]{"7a746f17a9bf4903b09b617135152c71","9204d99472c04ce7abf1bcb9773b0d49"});
        queryCondition.put("lastLoginNOTNULL",null);
        queryCondition.put("_orderByDesc","uid");
        queryCondition.put("_pageNumber",1);
        queryCondition.put("_pageSize",10);
        PageVo<User> userPageVo = dao.query(User.class)
                .addJSONObjectQuery(queryCondition)
                .getPagingList();
        logger.info("[自定义查询条件]查询结果:{}",JSON.toJSONString(userPageVo));
        Assert.assertTrue(userPageVo.getTotalSize()==2);
    }

    @Test
    public void testAddQuery() throws Exception {
        Condition<User> userCondition = dao.query(User.class)
                .distinct()
                .addQuery("username","@")
                .addQuery("type",">=",1)
                .addInQuery("token",new String[]{"7a746f17a9bf4903b09b617135152c71","9204d99472c04ce7abf1bcb9773b0d49"})
                .addNotNullQuery("lastLogin")
                .orderByDesc("uid")
                .page(1,10);
        JSONArray userArray = userCondition.getArray();
        logger.info("[Array查询]查询结果:{}", userArray.toJSONString());
        Assert.assertTrue(userArray.size()==2);
        List<User> userList = userCondition.getList();
        logger.info("[List查询]查询结果:{}", JSON.toJSONString(userList));
        Assert.assertTrue(userList.size()==2);

        userCondition = dao.query(User.class)
                .addNotEmptyQuery("username")
                .addNotInQuery("username",new String[]{"1212"})
                .addQuery("type = 1")
                .orderByDesc("uid")
                .limit(0,10);
        userArray = userCondition.getArray();
        logger.info("[Array查询]查询结果:{}", userArray.toJSONString());
        Assert.assertTrue(userArray.size()==2);
        userList = userCondition.getList();
        logger.info("[List查询]查询结果:{}", JSON.toJSONString(userList));
        Assert.assertTrue(userList.size()==2);
    }

    @Test
    public void testAggerateQuery() throws Exception {
        JSONArray array = dao.query(PlayList.class)
                .addAggerate("SUM","subscribeCount")
                .addAggerate("COUNT","id","count(id)")
                .addColumn("tv")
                .groupBy("tv")
                .orderBy("id")
                .getAggerateList();
        logger.info("[聚合查询]查询结果:{}",array.toJSONString());
        Assert.assertTrue(array.getJSONObject(0).getString("count(id)").equals("1"));
    }

    @Test
    public void testPartListQuery() throws Exception {
        Condition<User> userCondition = dao.query(User.class)
                .addColumn("username")
                .addColumn("password")
                .addQuery("username","@")
                .addQuery("type",">=",1)
                .addInQuery("token",new String[]{"7a746f17a9bf4903b09b617135152c71","9204d99472c04ce7abf1bcb9773b0d49"})
                .addNotNullQuery("lastLogin")
                .orderByDesc("uid")
                .page(1,10);
        List<User> userList = userCondition.getPartList();
        logger.info("[部分查询]查询结果:{}", JSON.toJSONString(userList, SerializerFeature.NotWriteDefaultValue));
        Assert.assertTrue(userList.size()==2);

        PageVo<User> userPageVo = userCondition.getPartPagingList();
        logger.info("[部分分页查询]查询结果:{}", JSON.toJSONString(userPageVo, SerializerFeature.NotWriteDefaultValue));
        Assert.assertTrue(userPageVo.getTotalSize()==2&&userPageVo.getList().get(0).getLastLogin()==null);
    }

    @Test
    public void testPagingListQuery() throws Exception {
        PageVo<User> pagingList = dao.query(User.class)
                .addQuery("username","@")
                .addQuery("type",">=",1)
                .addInQuery("token",new String[]{"7a746f17a9bf4903b09b617135152c71","9204d99472c04ce7abf1bcb9773b0d49"})
                .addNotNullQuery("lastLogin")
                .orderByDesc("uid")
                .page(1,10)
                .getPagingList();
        logger.info("[分页简单查询]查询结果:{}", JSON.toJSONString(pagingList));
        Assert.assertTrue(pagingList.getList().size()==2);
    }

    @Test
    public void testPagingListCompositQuery() throws Exception {
        PageVo<User> pagingList = dao.query(Video.class)
                .joinTable(PlayList.class,"playlistId","id")
                .done()
                .page(1,10)
                .getCompositPagingList();
        logger.info("[分页复杂查询]查询结果:{}", JSON.toJSONString(pagingList));
        Assert.assertTrue(pagingList.getList().size()>0);
    }

    @Test
    public void testGetValueList() throws Exception {
        List<Long> userIds = dao.query(User.class)
                .addQuery("username","@")
                .addQuery("type",">=",1)
                .addInQuery("token",new String[]{"7a746f17a9bf4903b09b617135152c71","9204d99472c04ce7abf1bcb9773b0d49"})
                .addNotNullQuery("lastLogin")
                .orderByDesc("uid")
                .page(1,10)
                .getValueList(Long.class,"uid");
        logger.info("[ValueList查询]查询结果:{}", JSON.toJSON(userIds));
        Assert.assertTrue(userIds.size()==2);
    }

    @Test
    public void testJoinTable() throws Exception {
        //查询用户id为1所订阅的播单
        List<PlayList> playListList= dao.query(PlayList.class)
                .joinTable(UserPlayList.class,"id","playlist_id")
                .addQuery("user_id",1)
                .done()
                .getList();
        logger.info("[关联查询][查询用户id为1所订阅的播单]查询结果:{}", JSON.toJSON(playListList));
        Assert.assertTrue(playListList.size()==1);

        //测试多个外键关联
        List<PlayHistory> playHistoryList = dao.query(PlayHistory.class)
                .joinTable(User.class,"user_id","uid")
                .addQuery("username","sunyue@schoolwow.cn")
                .orderByDesc("uid")
                .done()
                .joinTable(Video.class,"video_id","id")
                .addQuery("title","创业时代 01")
                .addInQuery("id",new Long[]{1l})
                .addNotNullQuery("title")
                .addNullQuery("publishTime")
                .addNotEmptyQuery("picture")
                .orderByDesc("id")
                .done()
                .getList();
        logger.info("[多个外键关联查询][查询用户名为sunyue@schoolwow.cn的对于视频标题为创业时代 01的播放历史]查询结果:{}", JSON.toJSON(playHistoryList));
        Assert.assertTrue(playListList.size()==1);
    }

    @Test
    public void testGetCompositArray() throws Exception {
        Condition<Video> videoCondition = dao.query(Video.class)
                .addQuery("id","1")
                .joinTable(PlayList.class,"playlist_id","id")
                .done();
        JSONArray array = videoCondition.getCompositArray();
        logger.info("[复杂查询-Array][查询视频id为1的带播单信息的视频信息]{}",array.toJSONString());
        List<Video> videoList = videoCondition.getCompositList();
        logger.info("[复杂查询-List][查询视频id为1的带播单信息的视频信息]{}",videoList);
    }

    @Test
    public void testCount() throws Exception {
        long count = dao.query(User.class)
                .addQuery("username","@")
                .addQuery("type",">=",1)
                .addInQuery("token",new String[]{"7a746f17a9bf4903b09b617135152c71","9204d99472c04ce7abf1bcb9773b0d49"})
                .addNotNullQuery("lastLogin")
                .orderByDesc("uid")
                .page(1,10)
                .count();
        logger.info("[Count]查询结果:{}",count);
    }

    @Test
    public void testUpdate() throws Exception {
        dao.startTransaction();
        long count = dao.query(User.class)
                .addQuery("username","@")
                .addUpdate("password","123456")
                .update();
        logger.info("[批量更新]查询结果:{}",count);
        dao.rollback();
        dao.endTransaction();
    }

    @Test
    public void testDelete() throws Exception {
        dao.startTransaction();
        long count = dao.query(User.class)
                .addQuery("username","quickdao")
                .delete();
        logger.info("[批量删除]查询结果:{}",count);
        dao.rollback();
        dao.endTransaction();
    }
}