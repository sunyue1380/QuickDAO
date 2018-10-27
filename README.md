# QuickDAO

#### 项目介绍
简单，易用的Java ORM框架。

#### 安装教程
* 使用中央仓库
```
<dependency>
  <groupId>cn.schoolwow</groupId>
  <artifactId>QuickDAO</artifactId>
  <version>1.0</version>
</dependency>
```

* 使用单独的jar文件
请[点击此处](http://pan.schoolwow.cn/f/a8591b2cc8af4e1cae9f/?dl=1)下载

#### 使用说明
1. 使用BasicDatasource(也可换用其他DataSource实现)
```
<dependency>
   <groupId>commons-dbcp</groupId>
   <artifactId>commons-dbcp</artifactId>
   <version>1.4</version>
</dependency>
```

2. 初始化QuickDAO,传递需要扫描的entity包名
```
BasicDataSource basicDataSource = new BasicDataSource();
basicDataSource.setDriverClassName("com.mysql.jdbc.Driver");
basicDataSource.setUrl("jdbc:mysql://127.0.0.1:3306/quickvideo");
basicDataSource.setUsername("root");
basicDataSource.setPassword("123456");
QuickDAO quickDAO = new QuickDAO(basicDataSource,"com.schoolwow.quickvideo.entity");
```

> QuickDAO目前只支持实体主键为int和long型主键,且主键变量名必须为``id``

3.使用QuickDAO,请查询API文档

#### 支持数据库
目前本项目只测试了Mysql,Sqlite,H2,其他数据库暂未测试,无法保证正常运行.如果您使用的数据不在此之列,欢迎提出issue.

#### QuickDAO API
quickDAO设计思想采用流式处理,使用起来简单易懂.
* fetch 根据id查询
```
User user = quickDAO.fetch(User.class,1);
User user = quickDAO.fetch(User.class,"username","sunyue");
```

* query 查询
```
Condition condition = quickDAO.query(User.class)
                .addQuery("username","sunyue")
                .addQuery("age",">",10)
                .addQuery("nickname = 'nickname'")
                .addInQuery("id",new Long[]{1L})
                .addNullQuery("address")
                .done();
```
使用``quickDAO.query(User.class)``返回一个Condition类型,它的方法如下:
field参数使用实体类对应属性的变量名即可,比如User类一个属性名为``userName``,则使用``"userName"``作为参数即可.

##### 查询条件API
|方法签名|说明|实例|
|---|---|---|
|``Condition addNullQuery(String field);``|添加空查询,即该字段为null或者|``condition.addNullQuery("username")``|
|``Condition addNotNullQuery(String field);``|添加非空查询|``condition.addNotNullQuery("username")``|
|``Condition addInQuery(String field, Object[] values);``|添加in查询|``condition.addInQuery("username",new String[]{"mary","tom"})``|
|``Condition addInQuery(String field, List values);``|添加in查询|``condition.addInQuery("username")``|
|``Condition addQuery(String query);``|自定义查询语句|``condition.addQuery("username = 'mary'")``|
|``Condition addQuery(String property, Object value);``|添加字段查询|``condition.addQuery("username","mary")``|
|``Condition addQuery(String property, String operator, Object value);``|添加字段查询,operator目前支持``>``,``>=``,``<``,``<=``,``=``|``condition.addQuery("publish_time","<=",new Date())``|
|``Condition addUpdate(String property, Object value);``|添加更新值(用于批量更新)|``condition.addUpdate("username","mary")``|
|``<T> SubCondition<T> joinTable(Class<T> _class, String primaryField, String joinTableField);``|用于外键关联,请查看外键关联部分查看详细文档|``condition.joinTable(Address.class,"id","userId")``|

##### 排序分页API
|方法签名|说明|实例|
|---|---|---|
|``Condition orderBy(String field);``|根据字段升序排列|``condition.orderBy("username")``|
|``Condition orderByDesc(String field);``|根据字段降序排列|``condition.orderByDesc("username")``|
|``Condition limit(long offset, long limit);``|limit语句|``condition.limit(0,10)``|

##### 获取结果API
long count();
    long update();
    long delete();
    List<T> getList();
    List<T> getValueList(Class<T> _class, String column);

|方法签名|说明|实例|
|---|---|---|
|``long count();``|获取查询结果条数|``condition.count()``|
|``long delete();``|删除所查询出来的结果|``condition.delete()``|
|``List<T> getList();``|获取查询结果|``condition.getList()``|
|``List<T> getValueList(Class<T> _class, String column);``|获取属性集合|``condition.getValueList(Long.class,"id")`` 获取查询结果id集合|

##### 外键关联说明
以下为场景说明,User类和Car类表示用户和汽车,UserCar表示用户和汽车的关联关系.
```
User{
   long id;
   String username;
}
Car{
   long id;
   String name;
}
UserCar{
   long userId;
   long carId;
}
```
* 查询用户id为1的用户所拥有的车的名字为路虎的车(最终结果是要返回Car类,但是查询条件要通过UserCar查询UserId)
```
quickDAO.query(Car.class).joinTable(UserCar.class,"id","carId").addQuery("userId",1).done().addQuery("name","路虎").getList();
```
``done();``表示当前关联表查询完毕,返回主表的查询
``joinTable(UserCar.class,"id","carId")``表示加入UserCar表,将Car(主表).id和UserCar(子表).carId关联起来进行查询.

对于一对多的关键查询,QuickDAO建议是将外键属性作为实体的一般属性(需为int或者long型)对待即可.
User{
  long id;
  String username;
}
Car{
  long id;
  String name;
  long userId;//表示该辆车属于哪个用户
}


#### 注解
QuickDAO提供部分注解:

|注解名|说明|示例|
|---|---|---|
|@ColumnType|数据类型|@ColumnType("varchar(1024)")|
|@DefaultValue|默认值|@DefaultValue("0")|
|@Ignore|忽略该字段/忽略该类|@Ignore")|
|@NotNull|是否非空|@NotNull|
|@Unique|是否唯一|@Unique|


#### 参与贡献

1. Fork 本项目
2. 新建 fork 分支
3. 提交代码
4. 新建 Pull Request