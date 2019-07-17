package cn.schoolwow.quickdao.condition;

import cn.schoolwow.quickdao.QuickDAOTest;
import cn.schoolwow.quickdao.dao.DAO;
import cn.schoolwow.quickdao.domain.PageVo;
import cn.schoolwow.quickdao.entity.logic.PlayList;
import cn.schoolwow.quickdao.entity.logic.Project;
import cn.schoolwow.quickdao.entity.user.User;
import cn.schoolwow.quickdao.entity.user.UserTalk;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@RunWith(Parameterized.class)
public class ConditionTest extends QuickDAOTest{
    Logger logger = LoggerFactory.getLogger(ConditionTest.class);

    public ConditionTest(DAO dao) {
        super(dao);
        initializeUser();
        initializeProject();
        initializePlaylist();
    }

    @Test
    public void testAddBetweenQuery(){
        Condition<User> condition = dao.query(User.class)
                .addBetweenQuery("uid",1,2);
        List<User> userList = condition.getList();
        Assert.assertEquals(2,userList.size());
    }

    @Test
    public void testClone(){
        Condition<UserTalk> condition = dao.query(User.class)
                .addQuery("username","@")
                .addQuery("type",">=",1)
                .addInQuery("token",new String[]{"7a746f17a9bf4903b09b617135152c71","9204d99472c04ce7abf1bcb9773b0d49"})
                .addNotNullQuery("lastLogin")
                .orderByDesc("uid")
                .joinTable(Project.class,"project","key")
                .done()
                .page(1,10);
        Condition<User> cloneCondition = condition.clone();
        logger.info("[clone]{}",cloneCondition);
        Assert.assertEquals(condition.count(),cloneCondition.count());
    }

    @Test
    public void testAddJSONObjectQueryWithJoinTable(){
        JSONObject queryCondition = new JSONObject();
        queryCondition.put("username","@");
        queryCondition.put("typeStart",1);
        queryCondition.put("tokenIN",new String[]{"7a746f17a9bf4903b09b617135152c71","9204d99472c04ce7abf1bcb9773b0d49"});
        queryCondition.put("lastLoginNOTNULL",null);
        JSONArray _orderByDesc = new JSONArray();
        _orderByDesc.add("uid");
        _orderByDesc.add("type");
        queryCondition.put("_orderByDesc",_orderByDesc);
        queryCondition.put("_pageNumber",1);
        queryCondition.put("_pageSize",10);

        JSONObject joinTable = new JSONObject();
        joinTable.put("_class","cn.schoolwow.quickdao.entity.logic.Project");
        joinTable.put("_primaryField","project");
        joinTable.put("_joinTableField","key");
        joinTable.put("key","blockchain");
        queryCondition.put("_joinTable",joinTable);

        PageVo<User> userPageVo = dao.query(User.class)
                .addJSONObjectQuery(queryCondition)
                .getPagingList();
        logger.info("[自定义查询条件]查询结果:{}",JSON.toJSONString(userPageVo));
        Assert.assertEquals(2,userPageVo.getTotalSize());
    }

    @Test
    public void testAddJSONObjectQuery(){
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
        Assert.assertEquals(2,userPageVo.getTotalSize());
    }

    @Test
    public void testAddQuery(){
        Condition<User> userCondition = dao.query(User.class)
                .distinct()
                .addNotEmptyQuery("username")
                .addQuery("username","@")
                .addQuery("type",">=",1)
                .addInQuery("token",new String[]{"7a746f17a9bf4903b09b617135152c71","9204d99472c04ce7abf1bcb9773b0d49"})
                .addNotInQuery("username",new String[]{"1212"})
                .addNotNullQuery("lastLogin")
                .orderByDesc("uid")
                .page(1,10);
        JSONArray userArray = userCondition.getArray();
        logger.info("[Array查询]查询结果:{}", userArray.toJSONString());
        Assert.assertEquals(2,userArray.size());
        List<User> userList = userCondition.getList();
        logger.info("[List查询]查询结果:{}", JSON.toJSONString(userList));
        Assert.assertEquals(2,userList.size());
    }

    @Test
    public void testAggerateQuery(){
        JSONArray array = dao.query(PlayList.class)
                .addAggerate("SUM","subscribeCount")
                .addAggerate("COUNT","id","count(id)")
                .addColumn("tv")
                .groupBy("tv")
                .orderBy("id")
                .getAggerateList();
        logger.info("[聚合查询]查询结果:{}",array.toJSONString());
        Assert.assertEquals("1",array.getJSONObject(0).getString("count(id)"));
    }

    @Test
    public void testPartListQuery(){
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
        Assert.assertEquals(2,userPageVo.getTotalSize());
        Assert.assertNull(userPageVo.getList().get(0).getLastLogin());
    }

    @Test
    public void testPagingListQuery(){
        PageVo<User> pagingList = dao.query(User.class)
                .addQuery("username","@")
                .addQuery("type",">=",1)
                .addInQuery("token",new String[]{"7a746f17a9bf4903b09b617135152c71","9204d99472c04ce7abf1bcb9773b0d49"})
                .addNotNullQuery("lastLogin")
                .orderByDesc("uid")
                .page(1,10)
                .getPagingList();
        logger.info("[分页简单查询]查询结果:{}", JSON.toJSONString(pagingList));
        Assert.assertEquals(2,pagingList.getList().size());
    }

    @Test
    public void testGetValueList(){
        List<Long> userIds = dao.query(User.class)
                .addQuery("username","@")
                .addQuery("type",">=",1)
                .addInQuery("token",new String[]{"7a746f17a9bf4903b09b617135152c71","9204d99472c04ce7abf1bcb9773b0d49"})
                .addNotNullQuery("lastLogin")
                .orderByDesc("uid")
                .page(1,10)
                .getValueList(Long.class,"uid");
        logger.info("[ValueList查询]查询结果:{}", JSON.toJSON(userIds));
        Assert.assertEquals(2,userIds.size());
    }

    @Test
    public void testCount(){
        long count = dao.query(User.class)
                .addQuery("username","@")
                .addQuery("type",">=",1)
                .addInQuery("token",new String[]{"7a746f17a9bf4903b09b617135152c71","9204d99472c04ce7abf1bcb9773b0d49"})
                .addNotNullQuery("lastLogin")
                .orderByDesc("uid")
                .page(1,10)
                .count();
        logger.info("[查询总行数]结果:{}",count);
        Assert.assertEquals(2,count);
    }

    @Test
    public void testUpdate(){
        long count = dao.query(User.class)
                .addQuery("username","@")
                .addUpdate("password","123456")
                .update();
        logger.info("[批量更新]查询结果:{}",count);
        Assert.assertEquals(2,count);
    }

    @Test
    public void testDelete(){
        long count = dao.query(User.class)
                .addQuery("username","sunyue@schoolwow.cn")
                .delete();
        logger.info("[批量删除]影响:{}",count);
        Assert.assertEquals(1,count);
    }
}