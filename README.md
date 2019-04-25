# QuickDAO
QuickDAO是一款简单,易用,轻量级的java ORM框架.

# 支持数据库
目前版本只支持数据库Mysql,SQlite以及H2.

# 快速入门
# 1 导入QuickDAO
## 1.1 使用maven
```
<dependency>
  <groupId>cn.schoolwow</groupId>
  <artifactId>QuickDAO</artifactId>
  <version>2.2</version>
</dependency>
```
## 1.2 下载单独jar包
请[点击此处](https://oss.sonatype.org/service/local/artifact/maven/redirect?r=releases&g=cn.schoolwow&a=QuickDAO&v=2.2&e=jar)下载jar包

# 2 初始化QuickDAO
QuickDAO基于JDBC,为提高效率,默认只支持数据库连接池.
## 2.1 导入commons-dbcp(或者其他的DataSource实现)
```
<dependency>
   <groupId>commons-dbcp</groupId>
   <artifactId>commons-dbcp</artifactId>
   <version>1.4</version>
</dependency>
```

## 2.2 初始化QuickDAO
```
String packageName = "cn.schoolwow.quickdao.entity";
BasicDataSource mysqlDataSource = new BasicDataSource();
mysqlDataSource.setDriverClassName("com.mysql.jdbc.Driver");
mysqlDataSource.setUrl("jdbc:mysql://127.0.0.1:3306/quickdao");
mysqlDataSource.setUsername("root");
mysqlDataSource.setPassword("123456");
cn.schoolwow.quickdao.dao.DAO dao = QuickDAO.newInstance().dataSource(dataSources[i])
                    .packageName(packageName)
                    .build();
//开始使用QuickDAO吧
```

到此QuickDAO初始化完成. 

> QuickDAO目前只支持实体主键为int和long型主键.

## 3 实体注解

* ColumnType 作用于成员属性,自定义数据库映射类型
* Comment 作用于成员属性,为数据库建表添加注释
* DefaultValue 作用于成员属性,表示数据库字段default默认值
* Id 作用于成员属性,标记主键字段.实体类中若无@Id注解,则成员属性中变量为id的int型或者long型的属性被标记为主键
* Ignore 作用于类和成员属性,表示映射到数据库时忽略该类或者某个成员属性
* NotNull 作用于成员属性,表示数据库字段not null
* Unique 作用于成员属性,表示数据库字段唯一 unique.若有多个Unique注解,则建立联合唯一索引

注解参数等详细信息请[访问这里](https://github.com/sunyue1380/QuickDAO/wiki/%E5%AE%9E%E4%BD%93%E6%B3%A8%E8%A7%A3)

## 4 数据库操作API
## 4.1  简单操作
```
//查询用户id为1的用户记录
User user = dao.fetch(User.class,1);
//查询用户名为quickdao的用户
User user = dao.fetch(User.class,"username","quickdao")
//保存(更新)用户,有id则更新,无id则插入
dao.save(new User());
dao.save(new ArrayList<User>());
//删除
dao.delete(User.class,1);
dao.delete(User.class,"username","quickdao")
```

## 4.2 复杂操作
dao.query(User.class);方法返回对用户类的一个Condition对象,该Condition类包含三类操作
1. 添加查询条件
2. 分页排序等功能
3. 获取结果集
具体各类操作包含API,请查看[接口文档](https://github.com/sunyue1380/QuickDAO/wiki/%E6%8E%A5%E5%8F%A3%E6%96%87%E6%A1%A3)
以下给出一个较完整的例子: 
```
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
```

```
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
```

## 相关文档

* [实体注解](https://github.com/sunyue1380/QuickDAO/wiki/%E5%AE%9E%E4%BD%93%E6%B3%A8%E8%A7%A3) - 自定义实体注解(可略过)
* [接口文档](https://github.com/sunyue1380/QuickDAO/wiki/%E6%8E%A5%E5%8F%A3%E6%96%87%E6%A1%A3) - 使用QuickDAO所提供的接口进行增删查改

## 反馈
目前项目处于初期阶段,欢迎提交Issue反馈,作者将第一时间跟进并努力解决.同时也欢迎热心人士提交PR共同维护此项目.