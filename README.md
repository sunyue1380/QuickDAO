# QuickDAO
QuickDAO是一款简单,易用,轻量级的java ORM框架.

# 支持数据库
目前版本只支持数据库Mysql,SQlite以及H2.QuickDAO支持自动建表,自动比较实体类与数据库的差异并自动新建字段.

> 注意事项
SpringBoot环境下由于类加载器问题请慎用,可能会导致未知问题出现!

# 快速入门
# 1 导入QuickDAO
## 1.1 使用maven
```
<dependency>
  <groupId>cn.schoolwow</groupId>
  <artifactId>QuickDAO</artifactId>
  <version>2.3</version>
</dependency>
```
## 1.2 下载单独jar包
请[点击此处](https://oss.sonatype.org/service/local/artifact/maven/redirect?r=releases&g=cn.schoolwow&a=QuickDAO&v=2.3&e=jar)下载jar包

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

> QuickDAO目前只支持实体主键类型为long.

## 3 实体注解

* **ColumnType** 作用于成员属性,自定义数据库映射类型
* **Comment** 作用于成员属性,为数据库建表添加注释
* **DefaultValue** 作用于成员属性,表示数据库字段default默认值
* **Id** 作用于成员属性,标记主键字段.实体类中若无@Id注解,则成员属性中变量为id的int型或者long型的属性被标记为主键
* **Ignore** 作用于类和成员属性,表示映射到数据库时忽略该类或者某个成员属性
* **NotNull** 作用于成员属性,表示数据库字段not null
* **Unique** 作用于成员属性,表示数据库字段唯一 unique.若有多个Unique注解,则建立联合唯一索引

注解参数等详细信息请[访问这里](https://github.com/sunyue1380/QuickDAO/wiki/%E5%AE%9E%E4%BD%93%E6%B3%A8%E8%A7%A3)

## 4 数据库操作API
QuickDAO建立了一套较完整的JUnit测试用例,使用者可查看工程test目录下编写的测试用例.

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

## 4.2 数据库更新操作

```java
/**
 * 数据库更新操作
 * */
public interface DAO {
    /**
     * <p>根据id查询</p>
     * @param _class 类名
     * @param id 指定要查询id字段的值
     * */
    <T> T fetch(Class<T> _class, long id);

    /**
     * <p>根据属性查询单个记录</p>
     * <p>若符合条件的记录有多条,则只会返回第一条记录</p>
     * @param _class 类名
     * @param property 指定要查询的字段
     * @param value 指定要查询的字段的值
     * */
    <T> T fetch(Class<T> _class, String property, Object value);

    /**
     * <p>根据属性查询记录</p>
     * @param _class 类名
     * @param property 指定要查询的字段
     * @param value 指定要查询的字段的值
     * */
    <T> List<T> fetchList(Class<T> _class, String property, Object value);

    /**
     * <p>复杂查询接口</p>
     * @param _class 类名,指定要查询的表
     * */
    <T> Condition<T> query(Class<T> _class);

    /**
     * <p>保存对象</p>
     * <p>判断该实例是否有id,无id则直接插入;然后判断该实例是否有唯一性约束,若有则根据唯一性约束更新,否则根据id更新</p>
     * @param instance 要保存的实例
     * */
    long save(Object instance);

    /**
     * <p>保存对象数组</p>
     * <p>判断该实例是否有id,无id则直接插入;然后判断该实例是否有唯一性约束,若有则根据唯一性约束更新,否则根据id更新</p>
     * @param instances 要保存的实例
     * */
    long save(Object[] instances);

    /**
     * <p>保存对象数组</p>
     * <p>判断该实例是否有id,无id则直接插入;然后判断该实例是否有唯一性约束,若有则根据唯一性约束更新,否则根据id更新</p>
     * @param instanceList 要保存的实例
     * */
    long save(List instanceList);

    /**
     * <p>根据id删除</p>
     * @param _class 类名,对应数据库中的一张表
     * @param id 要删除的id
     * */
    long delete(Class _class, long id);

    /**
     * <p>根据id删除</p>
     * @param _class 类名,对应数据库中的一张表
     * @param field 要删除的字段名
     * @param value 要删除的字段的值
     * */
    long delete(Class _class, String field, Object value);

    /**
     * <p>清空表</p>
     * @param _class 类名,对应数据库中的一张表
     * */
    long clear(Class _class);

    /**开启事务*/
    void startTransaction();

    /**设置保存点*/
    Savepoint setSavePoint(String name);

    /**事务回滚*/
    void rollback();

    /**事务回滚*/
    void rollback(Savepoint savePoint);

    /**事务提交*/
    void commit();

    /**结束事务*/
    void endTransaction();
}
```

## 4.3 数据库复杂查询

```java
/**
 * 查询条件接口
 * */
public interface Condition<T> {
    /**添加distinct语句*/
    Condition distinct();
    /**
     * 添加空查询
     * @param field 指明哪个字段为Null
     * */
    Condition addNullQuery(String field);
    /**
     * 添加非空查询
     * @param field 指明哪个字段不为Null
     * */
    Condition addNotNullQuery(String field);
    /**
     * 添加非空查询
     * @param field 指明哪个字段不为空字符串
     * */
    Condition addNotEmptyQuery(String field);
    /**
     * 添加范围查询语句
     * @param field 字段
     * @param values 指明在该范围内的值
     * */
    Condition addInQuery(String field, Object[] values);
    /**
     * 添加范围查询语句
     * @param field 字段
     * @param values 指明在该范围内的值
     * */
    Condition addInQuery(String field, List values);
    /**
     * 添加范围查询语句
     * @param field 字段
     * @param values 指明在不该范围内的值
     * */
    Condition addNotInQuery(String field, Object[] values);
    /**
     * 添加范围查询语句
     * @param field 字段
     * @param values 指明在不该范围内的值
     * */
    Condition addNotInQuery(String field, List values);
    /**
     * 添加自定义查询条件
     * @param query 子查询条件(<b>主表</b>统一别名为t)
     * */
    Condition addQuery(String query);
    /**
     * 添加字段查询
     * @param field 指明字段
     * @param value 字段值
     * */
    Condition addQuery(String field, Object value);
    /**
     * 添加字段查询
     * @param field 指明字段
     * @param operator 操作符,可为<b>></b>,<b>>=</b>,<b>=</b>,<b><</b><b><=</b>
     * @param value 字段值
     * */
    Condition addQuery(String field, String operator, Object value);
    /**
     * 添加实体属性查询
     * @param instance 实体类实例
     * */
    Condition addInstanceQuery(Object instance);
    /**
     * 添加实体属性
     * @param userBasicDataType 是否使用基本属性类型进行查询
     * */
    Condition addInstanceQuery(Object instance,boolean userBasicDataType);
    /**
     * 添加自定义查询条件
     * @deprecated 不建议使用,将来的版本可能会删除
     * */
    Condition addQuery(JSONObject queryCondition);
    /**
     * 添加更新字段,用于<b>{@link Condition#update()}</b>方法
     * @param field 待更新的字段
     * @param value 待更新字段的值
     * */
    Condition addUpdate(String field, Object value);
    /**
     * <p>添加聚合字段,默认别名为<b>{@link Condition#getAggerateList()}</b></p>
     * <p>用于<b>getAggerateList()</b>方法</p>
     * @param aggerate COUNT,SUM,MAX,MIN,AVG
     * @param field 字段名
     * */
    Condition addAggerate(String aggerate,String field);
    /**
     * 添加聚合字段
     * @param aggerate COUNT,SUM,MAX,MIN,AVG
     * @param field 字段名
     * @param alias 聚合字段别名
     * */
    Condition addAggerate(String aggerate,String field,String alias);
    /**
     * 添加分组查询
     * @param field 分组字段
     * */
    Condition groupBy(String field);
//    /**分组过滤*/
//    Condition having(String query);
    /**
     * 关联表
     * <p><b>主表,默认别名为t</b>(即query方法传递的类对象)</p>
     * <p><b>子表,第一次调用JoinTable方法关联的表别名为t1，第二个为t2,以此类推</b></p>
     * @param _class 要关联的表
     * @param primaryField <b>主表</b>关联字段
     * @param joinTableField <b>子表</b>关联字段
     * */
    <T> SubCondition<T> joinTable(Class<T> _class, String primaryField, String joinTableField);
    /**
     * 关联表
     * <p><b>主表,默认别名为t</b>(即query方法传递的类对象)</p>
     * <p><b>子表,第一次调用JoinTable方法关联的表别名为t1，第二个为t2,以此类推</b></p>
     * @param _class 要关联的表
     * @param primaryField <b>主表</b>关联字段
     * @param joinTableField <b>子表</b>关联字段
     * @param compositField <b>主表</b>关联字段对应复杂字段的字段名
     * */
    <T> SubCondition<T> joinTable(Class<T> _class, String primaryField, String joinTableField,String compositField);

    /**
     * 根据指定字段升序排列
     * @param field 升序排列字段名
     * */
    Condition orderBy(String field);
    /**
     * 根据指定字段降序排列
     * @param field 降序排列字段名
     * */
    Condition orderByDesc(String field);
    /**
     * 分页操作
     * @param offset 偏移量
     * @param limit 返回个数
     * */
    Condition limit(long offset, long limit);
    /**
     * 分页操作
     * @param pageNum 第几页
     * @param pageSize 每页个数
     * */
    Condition page(int pageNum,int pageSize);
    /**
     * 添加待查询字段,用于<b>{@link Condition#getPartList()}</b>
     * @param field 要返回的字段
     * */
    Condition addColumn(String field);

    /**
     * 获取符合条件的总数目
     * */
    long count();
    /**
     * <p>更新符合条件的记录</p>
     * <p><b>前置条件</b>:请先调用<b>{@link Condition#addUpdate(String, Object)}</b>方法</p>
     * */
    long update();
    /**
     * 删除符合条件的数据库记录
     * */
    long delete();
    /**
     * 返回符合条件的数据库记录
     * */
    List<T> getList();
    /**
     * 返回符合条件的数据库记录
     * */
    JSONArray getArray();
    /**
     * <p>返回符合条件的数据库分页记录</p>
     * <p><b>前置条件</b>:请先调用<b>{@link Condition#page(int, int)} </b>方法</p>
     * */
    PageVo<T> getPagingList();
    /**
     * <p>返回符合条件的数据库分页记录,同时返回关联查询方法({@link Condition#joinTable(Class, String, String)})所关联的字段信息</p>
     * <p><b>前置条件</b>:请先调用<b>{@link Condition#page(int, int)} </b>方法</p>
     * */
    PageVo<T> getPagingCompositList();
    /**
     * <p>返回符合条件的数据库记录,同时返回关联查询方法({@link Condition#joinTable(Class, String, String)})所关联的字段信息</p>
     * <p><b>注意</b>:若未调用过joinTable方法,则该方法不会返回复杂对象字段信息</p>
     * */
    List<T> getCompositList();
    /**
     * <p>返回符合条件的数据库记录,同时返回关联查询方法({@link Condition#joinTable(Class, String, String)})所关联的字段信息</p>
     * <p><b>注意</b>:若未调用过joinTable方法,则该方法不会返回复杂对象字段信息</p>
     * */
    JSONArray getCompositArray();
    /**
     * <p>返回指定的部分字段的数据库记录</p>
     * <p><b>前置条件</b>:请先调用<b>{@link Condition#addColumn(String)} </b></p>
     * */
    List<T> getPartList();
    /**
     * <p>返回指定单个字段的集合</p>
     * @param _class 返回字段类型
     * @param column 待返回的字段
     * */
    <E> List<E> getValueList(Class<E> _class, String column);
    /**
     * <p>返回聚合字段的数据库记录</p>
     * <p><b>前置条件</b>:请先调用{@link Condition#addAggerate(String, String)}</p>
     * <p>若调用了{@link Condition#addColumn(String)} 则会返回addColumn所指定的字段</p>
     * */
    JSONArray getAggerateList();
    /**
     * <p>克隆该Condition对象</p>
     * */
    Condition clone() throws CloneNotSupportedException;
}
```

## 4.4 简单实例
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