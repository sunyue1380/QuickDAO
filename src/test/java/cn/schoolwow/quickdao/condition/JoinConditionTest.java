package cn.schoolwow.quickdao.condition;

import cn.schoolwow.quickdao.QuickDAOTest;
import cn.schoolwow.quickdao.dao.DAO;
import cn.schoolwow.quickdao.domain.PageVo;
import cn.schoolwow.quickdao.entity.logic.PlayHistory;
import cn.schoolwow.quickdao.entity.logic.PlayList;
import cn.schoolwow.quickdao.entity.logic.Project;
import cn.schoolwow.quickdao.entity.logic.Video;
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
                .done()
                .getList();
        logger.info("[关联查询][查询用户id为1所订阅的播单]查询结果:{}", JSON.toJSON(playListList));
        Assert.assertEquals(1,playListList.size());

        //测试多个外键关联
        List<PlayHistory> playHistoryList = dao.query(PlayHistory.class)
                .joinTable(User.class, "user_id", "uid")
                .addQuery("username", "sunyue@schoolwow.cn")
                .orderByDesc("uid")
                .done()
                .joinTable(Video.class, "video_id", "id")
                .addQuery("title", "创业时代 01")
                .addInQuery("id", new Long[]{1l})
                .addNotNullQuery("title")
                .addNullQuery("publishTime")
                .addNotEmptyQuery("picture")
                .orderByDesc("id")
                .done()
                .getList();
        logger.info("[多个外键关联查询][查询用户名为sunyue@schoolwow.cn的对于视频标题为创业时代 01的播放历史]查询结果:{}", JSON.toJSON(playHistoryList));
        Assert.assertEquals(1,playListList.size());
    }

    @Test
    public void testPagingListCompositQuery(){
        PageVo<User> pagingList = dao.query(Video.class)
                .joinTable(PlayList.class, "playlistId", "id")
                .done()
                .page(1, 10)
                .getCompositPagingList();
        logger.info("[分页复杂查询]查询结果:{}", JSON.toJSONString(pagingList));
        Assert.assertEquals(1,pagingList.getTotalSize());
    }

    @Test
    public void testGetCompositArray(){
        Condition<Video> videoCondition = dao.query(Video.class)
                .addQuery("id", "1")
                .joinTable(PlayList.class, "playlist_id", "id")
                .done();
        JSONArray array = videoCondition.getCompositArray();
        logger.info("[复杂查询-Array][查询视频id为1的带播单信息的视频信息]{}", array.toJSONString());
        Assert.assertEquals(1,array.size());
        List<Video> videoList = videoCondition.getCompositList();
        logger.info("[复杂查询-List][查询视频id为1的带播单信息的视频信息]{}", videoList);
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
