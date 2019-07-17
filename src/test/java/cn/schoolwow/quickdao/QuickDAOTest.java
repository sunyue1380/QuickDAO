package cn.schoolwow.quickdao;

import cn.schoolwow.quickdao.dao.DAO;
import cn.schoolwow.quickdao.entity.logic.Comment;
import cn.schoolwow.quickdao.entity.logic.PlayList;
import cn.schoolwow.quickdao.entity.logic.Project;
import cn.schoolwow.quickdao.entity.user.Report;
import cn.schoolwow.quickdao.entity.user.Talk;
import cn.schoolwow.quickdao.entity.user.User;
import cn.schoolwow.quickdao.entity.user.UserPlayList;
import org.apache.commons.dbcp.BasicDataSource;
import org.junit.runners.Parameterized;

import javax.sql.DataSource;
import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;

public class QuickDAOTest {
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
        sqliteDataSource.setUrl("jdbc:sqlite:"+new File("quickdao_sqlite.db").getAbsolutePath());

        BasicDataSource h2DataSource = new BasicDataSource();
        h2DataSource.setDriverClassName("org.h2.Driver");
        h2DataSource.setUrl("jdbc:h2:"+new File("quickdao_h2.db").getAbsolutePath()+";mode=MYSQL");

        //各种数据库产品
        DataSource[] dataSources = {mysqlDataSource,sqliteDataSource,h2DataSource};
        Object[][] data = new Object[dataSources.length][1];
        for(int i=0;i<dataSources.length;i++){
            DAO dao = QuickDAO.newInstance().dataSource(dataSources[i])
                    .packageName("cn.schoolwow.quickdao.entity")
                    .packageName("cn.schoolwow.quickdao.domain","d")
                    .autoCreateTable(true)
                    .build();
            data[i][0] = dao;
        }
        return Arrays.asList(data);
    }

    /**初始化用户播单表*/
    public void initializeUser(){
        dao.rebuild(User.class);
        {
            User user = new User();
            user.setUsername("sunyue@schoolwow.cn");
            user.setPassword("123456789");
            user.setLastLogin(new Date(1550579294000l));
            user.setType(1);
            user.setToken("7a746f17a9bf4903b09b617135152c71");
            user.setProject("blockchain");
            dao.save(user);
        }
        {
            User user = new User();
            user.setUsername("648823596@qq.com");
            user.setPassword("123456789");
            user.setLastLogin(new Date(1553442438000l));
            user.setType(1);
            user.setToken("9204d99472c04ce7abf1bcb9773b0d49");
            user.setProject("blockchain");
            dao.save(user);
        }
    }

    /**初始化说说表*/
    public void initializeTalk(){
        dao.rebuild(Talk.class);
        Talk talk = new Talk();
        talk.setUserId(1);
        talk.setContent("用户1发表的说说");
        talk.setPicture("用户1发表的说说的图片地址");
        talk.setDisplayBig(true);
        talk.setPublishTime(new Date(1550579294000l));
        talk.setEnable(true);
        talk.setImported(true);
        dao.save(talk);
    }

    /**初始化项目表*/
    public void initializeProject(){
        dao.rebuild(Project.class);
        Project project = new Project();
        project.setKey("blockchain");
        project.setValue("区块链");
        dao.save(project);
    }

    /**初始化举报表*/
    public void initializeReport(){
        dao.rebuild(Report.class);
        Report report = new Report();
        report.setUserId(2);
        report.setTalkId(1);
        report.setContent("用户2举报用户1发表的说说");
        report.setContact("15223530300");
        report.setState(0);
        report.setReportTime(new Date(1550579294000l));
        dao.save(report);
    }

    /**初始化评论表*/
    public void initializeComment(){
        dao.rebuild(Comment.class);
        Comment comment = new Comment();
        comment.setAvatar("https://wwc.alicdn.com/avatar/getAvatar.do?userId=836646215&width=160&height=160&type=sns");
        comment.setAuthor("zcyyy2012");
        comment.setPublishTime(new Date(1539959170000l));
        comment.setVideoId(1);
        dao.save(comment);
    }

    /**初始化播单表*/
    public void initializePlaylist(){
        dao.rebuild(PlayList.class);
        PlayList playList = new PlayList();
        playList.setName("创业时代");
        playList.setTv("iqiyi.com");
        playList.setUrl("http://www.iqiyi.com/a_19rrhcydw5.html");
        playList.setType(1);
        playList.setSearchWord("创业时代");
        playList.setPicture("https://pic8.iqiyipic.com/image/20181016/2c/be/a_100098518_m_601_m14_180_101.jpg");
        playList.setDescription("《创业时代》是由安建导演，黄轩,Angelababy,周一围等主演的内地电视剧，共54集。爱奇艺在线观看《创业时代》全集高清视频。剧情简介：在一次偶然的机会中，软件工程师郭鑫年找到了新的灵感，他决心开发一款新的手机通讯软件，可以将手机短信以语音的形式在用户之间传送，这个想法让郭鑫年激动不已，他怀着一腔热血，走上了创业之路。在天使投资人和旧日朋友的支持下，郭鑫年经过艰苦的研发，终于令手机软件诞生，起名为魔晶，并获得了巨大的成功，同时也与投...");
        playList.setSearchSource("baidu");
        playList.setLastAnalyzeTime(new Date(1543840913000l));
        dao.save(playList);
    }

    /**初始化用户播单表*/
    public void initializeUserPlaylist(){
        dao.rebuild(UserPlayList.class);
        UserPlayList userPlayList = new UserPlayList();
        userPlayList.setUserId(1);
        userPlayList.setPlaylistId(1);
        dao.save(userPlayList);
    }

    public QuickDAOTest(DAO dao){
        this.dao = dao;
    }
}
