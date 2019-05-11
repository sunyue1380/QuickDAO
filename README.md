# QuickDAO
QuickDAO是一款简单,易用,轻量级的java ORM框架.目前支持Mysql,SQlite以及H2数据库.

> SpringBoot环境下由于类加载器问题请慎用,可能会导致未知问题出现!

> QuickDAO目前没有在高并发环境下实践过,因此对于高并发项目请慎用.本项目适合于中小项目使用,提供封装良好的API与数据库进行交互,减少样板代码,尽可能提高开发效率!

> 目前QuickDAO使用Logback日志框架,DEBUG级别下会输入执行的SQL语句!

# 快速入门
## 1 建立实体类
```java
//用户类
public class User {
    private long id;
    private String username;
    private String password;
//用户设置表
public class UserSetting {
    private long id;
    private long userId;
    private String setting;
    private User user;
//用户关注表
public class UserFollow {
    private long id;
    private long userId;
    private long followerId;
    private User user;
    private User followUser;
```

## 2 导入QuickDAO
QuickDAO基于JDBC,为提高效率,默认只支持数据库连接池.

* 导入commons-dbcp(或者其他的DataSource实现)
```
<dependency>
   <groupId>commons-dbcp</groupId>
   <artifactId>commons-dbcp</artifactId>
   <version>1.4</version>
</dependency>
```

## 2.1 使用maven
```
<dependency>
  <groupId>cn.schoolwow</groupId>
  <artifactId>QuickDAO</artifactId>
  <version>2.4</version>
</dependency>
```

## 2.2 下载jar包
请[点击此处](https://oss.sonatype.org/service/local/artifact/maven/redirect?r=releases&g=cn.schoolwow&a=QuickDAO&v=2.4&e=jar)下载jar包

## 3 使用QuickDAO
QuickDAO支持自动建表,自动新增字段功能.当您在Java代码中配置好QuickDAO后无需再对数据库做任何操作.

```java
BasicDataSource mysqlDataSource = new BasicDataSource();
mysqlDataSource.setDriverClassName("com.mysql.jdbc.Driver");
mysqlDataSource.setUrl("jdbc:mysql://127.0.0.1:3306/quickdao");
mysqlDataSource.setUsername("root");
mysqlDataSource.setPassword("123456");
//指定实体所在包名
cn.schoolwow.quickdao.dao.DAO dao = QuickDAO.newInstance()
                    .dataSource(mysqlDataSource)
                    .packageName("cn.schoolwow.quickdao.entity")
                    .build();
//之后所有的操作使用dao对象完成
```

## 3.1 简单查询与更新操作
```java
//根据id查询
User user = dao.fetch(User.class,1);
//根据属性查询
User user = dao.fetch(User.class,"username","quickdao");
List<User> user = dao.fetchList(User.class,"password","123456");
//保存用户
dao.save(user);
dao.save(userList);
//删除用户
dao.delete(User.class,1);
dao.delete(User.class,"username","quickdao");
```

## 3.2 复杂查询

```java
List<User> userList = dao.query(User.class)
   .addNotEmptyQuery("username")
   .getList();
```

> 关于复杂查询详细信息请[点此查看](https://github.com/sunyue1380/QuickDAO/wiki/%E5%A4%8D%E6%9D%82%E6%9F%A5%E8%AF%A2)

## 3.3 外键查询

```java
List<User> userList = dao.query(User.class)
   .join(UserSetting.class,"id","userId")
   .addNotEmptyQuery("setting")
   .done()
   .getList();
```

> 关于外键查询详细信息请[点此查看](https://github.com/sunyue1380/QuickDAO/wiki/%E5%A4%96%E9%94%AE%E6%9F%A5%E8%AF%A2)

## 3.3 分页排序查询

```java
List<User> userList = dao.query(User.class)
   .pageNumber(1,10)
   .orderByDesc("id")
   .getList();
```

> 关于分页排序详细信息请[点此查看](https://github.com/sunyue1380/QuickDAO/wiki/%E5%88%86%E9%A1%B5%E6%8E%92%E5%BA%8F)

## 3.4 建表删表
```java
//删除User表
dao.drop(User.class);
//建立User表
dao.create(User.class);
```

# 详细手册

* [实体注解](https://github.com/sunyue1380/QuickDAO/wiki/%E5%AE%9E%E4%BD%93%E6%B3%A8%E8%A7%A3)
QuickDAO有自动建表功能,用户可使用注解为实体类信息添加相关字段信息.同时QuickDAO能够自动匹配实体类和数据库表,自动添加新增的实体类字段信息.

* [配置信息](https://github.com/sunyue1380/QuickDAO/wiki/%E9%85%8D%E7%BD%AE%E4%BF%A1%E6%81%AF)
QuickDAO可配置是否自动新建表,是否建立外键约束,忽略表和包等等.

* [事务功能](https://github.com/sunyue1380/QuickDAO/wiki/%E4%BA%8B%E5%8A%A1%E5%8A%9F%E8%83%BD)
QuickDAO基于JDBC封装了简单的事务功能.若需使用事务,请查询此手册

> 请注意: 开启事务时只作用于调用方法的dao对象,若配置有多个dao对象,对其他dao对象无影响(也即其他dao对象不会跟着开启事务功能)!

QuickDAO本身提供了一套较完整的JUnit测试用例,可查看[ConditionTest](https://github.com/sunyue1380/QuickDAO/blob/master/src/test/java/cn/schoolwow/quickdao/condition/ConditionTest.java)和[DAOTest](https://github.com/sunyue1380/QuickDAO/blob/master/src/test/java/cn/schoolwow/quickdao/dao/DAOTest.java).

# 反馈
目前QuickDAO还不成熟,还在不断完善中.若有问题请提交Issue,作者将第一时间跟进并努力解决.同时欢迎热心认识提交PR,共同完善QuickDAO项目!