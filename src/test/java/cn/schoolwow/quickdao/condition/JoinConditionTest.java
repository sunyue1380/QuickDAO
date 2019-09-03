package cn.schoolwow.quickdao.condition;

import cn.schoolwow.quickdao.QuickDAOTest;
import cn.schoolwow.quickdao.dao.DAO;
import cn.schoolwow.quickdao.domain.PageVo;
import cn.schoolwow.quickdao.entity.logic.PlayList;
import cn.schoolwow.quickdao.entity.logic.Project;
import cn.schoolwow.quickdao.entity.user.Report;
import cn.schoolwow.quickdao.entity.user.Talk;
import cn.schoolwow.quickdao.entity.user.User;
import cn.schoolwow.quickdao.entity.user.UserPlayList;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@RunWith(Parameterized.class)
public class JoinConditionTest extends QuickDAOTest {
    Logger logger = LoggerFactory.getLogger(ConditionTest.class);

    public JoinConditionTest(DAO dao) {
        super(dao);
        initializeUser();
        initializeProject();
        initializeTalk();
        initializeReport();
        initializePlaylist();
        initializeUserPlaylist();
    }

    @Test
    public void testAddJSONObjectQueryWithJoinTable(){
        String condition = "{\n" +
                "\t\"_joinTables\": [{\n" +
                "\t\t\"uid\":2,\n" +
                "\t\t\"_class\": \"cn.schoolwow.quickdao.entity.user.User\",\n" +
                "\t\t\"_primaryField\": \"userId\",\n" +
                "\t\t\"_joinTableField\": \"uid\"\n" +
                "\t},\n" +
                "\t{\n" +
                "\t\t\"_class\": \"cn.schoolwow.quickdao.entity.user.Talk\",\n" +
                "\t\t\"_primaryField\": \"talkId\",\n" +
                "\t\t\"_joinTableField\": \"id\",\n" +
                "\t\t\"_joinTables\": [{\n" +
                "\t\t\t\"username\":\"sunyue@schoolwow.cn\",\n" +
                "\t\t\t\"_class\": \"cn.schoolwow.quickdao.entity.user.User\",\n" +
                "\t\t\t\"_primaryField\": \"userId\",\n" +
                "\t\t\t\"_joinTableField\": \"uid\",\n" +
                "\t\t\t\"_joinTables\": [{\n" +
                "\t\t\t\t\"_class\": \"cn.schoolwow.quickdao.entity.logic.Project\",\n" +
                "\t\t\t\t\"_primaryField\": \"project\",\n" +
                "\t\t\t\t\"_joinTableField\": \"key\"\n" +
                "\t\t\t}]\n" +
                "\t\t}]\n" +
                "\t}]\n" +
                "}";
        JSONObject queryCondition = JSON.parseObject(condition);
        Assert.assertEquals(1,dao.query(Report.class).addJSONObjectQuery(queryCondition).getList().size());
    }

    @Test
    public void testJoinTable(){
        //查询用户id为1所订阅的播单
        List<PlayList> playListList = dao.query(PlayList.class)
                .joinTable(UserPlayList.class, "id", "playlist_id")
                .addQuery("user_id", 1)
                .orderByDesc("id")
                .done()
                .getList();
        logger.info("[关联查询][查询用户id为1所订阅的播单]查询结果:{}", JSON.toJSON(playListList));
        Assert.assertEquals(1,playListList.size());
    }

    @Test
    public void testPagingListCompositQuery(){
        PageVo<User> pagingList = dao.query(User.class)
                .joinTable(Project.class, "project", "key")
                .done()
                .orderByDesc("uid")
                .page(1, 10)
                .getCompositPagingList();
        logger.info("[分页复杂查询]查询结果:{}", JSON.toJSONString(pagingList));
        Assert.assertEquals(2,pagingList.getTotalSize());
    }

    @Test
    public void testGetCompositArray(){
        Condition<User> userCondition = dao.query(User.class)
                .addQuery("uid", 1)
                .joinTable(Project.class, "project", "key")
                .done();
        JSONArray array = userCondition.getCompositArray();
        logger.info("[复杂查询-Array][查询用户id为1]{}", array.toJSONString());
        Assert.assertEquals(1,array.size());
        List<User> videoList = userCondition.getCompositList();
        logger.info("[复杂查询-List][查询用户id为1]{}", videoList);
        Assert.assertEquals(1,videoList.size());
    }

    @Test
    public void testMultiSubConditionJoinTable(){
        List<Report> reportList = dao.query(Report.class)
                .joinTable(User.class, "userId", "uid")
                .done()
                .joinTable(Talk.class, "talkId", "id")
                .joinTable(User.class, "userId", "uid")
                .joinTable(Project.class, "project", "key")
                .doneSubCondition()
                .done()
                .getList();
        logger.info("[子表关联查询]结果:{}", JSON.toJSONString(reportList));
        Assert.assertEquals(1, reportList.size());
    }


    @Test
    public void testSubConditionJoinTable(){
        List<Report> reportList = dao.query(Talk.class)
                .joinTable(User.class, "userId", "uid")
                .addQuery("uid", 1)
                .joinTable(Project.class, "project", "key")
                .doneSubCondition()
                .done()
                .getCompositList();
        logger.info("[子表关联查询]结果:{}", JSON.toJSONString(reportList));
        Assert.assertEquals(1, reportList.size());
    }

    @Test
    public void testOuterJoinTable(){
        //查询用户id为1所订阅的播单
        List<PlayList> playListList = dao.query(PlayList.class)
                .joinTable(UserPlayList.class, "id", "playlist_id")
                .leftJoin()
                .addQuery("user_id", 1)
                .done()
                .getList();
        logger.info("[左外连接查询][查询用户id为1所订阅的播单]查询结果:{}", JSON.toJSON(playListList));
        Assert.assertEquals(1,playListList.size());
    }
}
