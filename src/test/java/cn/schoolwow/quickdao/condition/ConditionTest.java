package cn.schoolwow.quickdao.condition;

import cn.schoolwow.quickdao.dao.DAO;
import cn.schoolwow.quickdao.dao.DAOTest;
import cn.schoolwow.quickdao.domain.PageVo;
import cn.schoolwow.quickdao.entity.logic.PlayHistory;
import cn.schoolwow.quickdao.entity.logic.PlayList;
import cn.schoolwow.quickdao.entity.logic.Video;
import cn.schoolwow.quickdao.entity.user.User;
import cn.schoolwow.quickdao.entity.user.UserPlayList;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializeConfig;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.alibaba.fastjson.support.config.FastJsonConfig;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.util.List;

public class ConditionTest extends DAOTest{
    Logger logger = LoggerFactory.getLogger(ConditionTest.class);

    public ConditionTest(DAO dao, DataSource dataSource) {
        super(dao, dataSource);
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
                .addQuery(queryCondition)
                .getPagingList();
        logger.info("[测试查询功能]查询结果:{}",JSON.toJSONString(userPageVo));
        Assert.assertTrue(userPageVo.getTotalSize()==2);
    }

    @Test
    public void testInstanceQuery() throws Exception {
        User user = new User();
        user.setUsername("@");
        user.setType(1);
        List<User> userList = dao.query(User.class)
                .addInstanceQuery(user,false)
                .orderByDesc("uid")
                .page(1,10)
                .getList();
        logger.info("[测试查询功能]查询结果:{}", JSON.toJSONString(userList));
        Assert.assertTrue(userList.size()==2);
    }

    @Test
    public void testAddQuery() throws Exception {
        List<User> userList = dao.query(User.class)
                .distinct()
                .addQuery("username","@")
                .addQuery("type",">=",1)
                .addInQuery("token",new String[]{"7a746f17a9bf4903b09b617135152c71","9204d99472c04ce7abf1bcb9773b0d49"})
                .addNotNullQuery("lastLogin")
                .orderByDesc("uid")
                .page(1,10)
                .getList();
        logger.info("[测试查询功能]查询结果:{}", JSON.toJSONString(userList));
        Assert.assertTrue(userList.size()==2);

        userList = dao.query(User.class)
                .addNotEmptyQuery("username")
                .addNotInQuery("username",new String[]{"1212"})
                .addQuery("type = 1")
                .orderByDesc("uid")
                .limit(0,10)
                .getList();
        logger.info("[测试查询功能]查询结果:{}", JSON.toJSONString(userList));
        Assert.assertTrue(userList.size()==2);
    }

    @Test
    public void testAddUpdate() throws Exception {
        long effect = dao.query(User.class)
                .addQuery("username","@")
                .addUpdate("password","123456")
                .joinTable(UserPlayList.class,"uid","userId")
                .addQuery("playlistId",1)
                .done()
                .update();
        logger.info("[测试addUpdate功能]影响:{}",effect);
        Assert.assertTrue(effect==1||effect==2);
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
        logger.info("[测试查询功能]查询结果:{}",array.toJSONString());
        Assert.assertTrue(array.getJSONObject(0).getString("count(id)").equals("1"));
    }

    @Test
    public void testPartListQuery() throws Exception {
        List<User> userList = dao.query(User.class)
                .addColumn("username")
                .addColumn("password")
                .addQuery("username","@")
                .addQuery("type",">=",1)
                .addInQuery("token",new String[]{"7a746f17a9bf4903b09b617135152c71","9204d99472c04ce7abf1bcb9773b0d49"})
                .addNotNullQuery("lastLogin")
                .orderByDesc("uid")
                .page(1,10)
                .getPartList();
        logger.info("[测试查询功能]查询结果:{}", JSON.toJSONString(userList, SerializerFeature.NotWriteDefaultValue));
        Assert.assertTrue(userList.size()==2);
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
        logger.info("[测试查询功能]查询结果:{}", JSON.toJSONString(pagingList));
        Assert.assertTrue(pagingList.getList().size()==2);
    }

    @Test
    public void testPagingListCompositQuery() throws Exception {
        PageVo<User> pagingList = dao.query(Video.class)
                .joinTable(PlayList.class,"playlistId","id")
                .done()
                .page(1,10)
                .getPagingList(true);
        logger.info("[测试查询功能]查询结果:{}", JSON.toJSONString(pagingList));
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
        logger.info("[测试查询Value功能]查询结果:{}", JSON.toJSON(userIds));
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
        logger.info("[查询用户id为1所订阅的播单]查询结果:{}", JSON.toJSON(playListList));
        Assert.assertTrue(playListList.size()==1);

        //测试多个外键关联
        //查询用户名为sunyue@schoolwow.cn的对于视频标题为创业时代 01的播放历史
        List<PlayHistory> playHistoryList = dao.query(PlayHistory.class)
                .joinTable(User.class,"user_id","uid")
                .addQuery("username","sunyue@schoolwow.cn")
                .done()
                .joinTable(Video.class,"video_id","id")
                .addQuery("title","创业时代 01")
                .addInQuery("id",new Long[]{1l})
                .addNotNullQuery("title")
                .addNullQuery("publishTime")
                .addNotEmptyQuery("picture")
                .done()
                .getList();
        logger.info("[查询用户名为sunyue@schoolwow.cn的对于视频标题为创业时代 01的播放历史]查询结果:{}", JSON.toJSON(playHistoryList));
        Assert.assertTrue(playListList.size()==1);
    }

    @Test
    public void testGetCompositList() throws Exception {
        List<Video> videoList = dao.query(Video.class)
                .addQuery("id","1")
                .joinTable(PlayList.class,"playlist_id","id")
                .done()
                .getCompositList();
        logger.info("[查询视频id为1的带播单信息的视频信息]{}",JSON.toJSONString(videoList));
        Assert.assertTrue(videoList.get(0).getPlayList()!=null);
    }

    @Test
    public void testGetCompositArray() throws Exception {
        JSONArray array = dao.query(Video.class)
                .addQuery("id","1")
                .joinTable(PlayList.class,"playlist_id","id")
                .done()
                .getCompositArray();
        logger.info("[查询视频id为1的带播单信息的视频信息]{}",array.toJSONString());
        List<Video> videoList = array.toJavaList(Video.class);
        System.out.println(videoList);
        System.out.println(videoList.get(0).getPlayList());
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
        logger.info("[测试Count功能]查询结果:{}",count);
    }

    @Test
    public void testUpdate() throws Exception {
        //TODO Sqlite暂时不支持多表关联更新
        long count = dao.query(User.class)
                .addQuery("username","@")
                .addUpdate("password","123456")
                .update();
        logger.info("[测试批量更新功能]查询结果:{}",count);
    }

    @Test
    public void testDelete() throws Exception {
        //TODO Sqlite的更新会报错,暂时不解决
        long count = dao.query(User.class)
                .addQuery("username","@")
                .addUpdate("password","123456")
                .delete();
        logger.info("[测试批量删除功能]查询结果:{}",count);
    }
}