# 此仓库已废弃

请访问 https://github.com/sunyue1380/QuickDAO3 获取最新版本









# QuickDAO
QuickDAO是一款简单,易用,轻量级的java ORM框架.

# 支持数据库
最新版本2.10,支持以下数据库,更多数据库陆续支持中......

* MySQL
* SQLite
* H2
* Postgre
* SQL Server(2012版本以上)

# 详细文档(gitbook)
QuickDAO使用了gitbook编写了文档,帮助您快速了解和使用QuickDAO.[点此访问](http://quickdao.schoolwow.cn)文档

# 注意事项
> SpringBoot环境下由于类加载器问题请慎用,可能会导致未知问题出现!

> QuickDAO目前没有在高并发环境下实践过,因此对于高并发项目请慎用.本项目适合于中小项目使用,提供封装良好的API与数据库进行交互,减少样板代码,尽可能提高开发效率!

> 目前QuickDAO使用Logback日志框架,DEBUG级别下会输出执行的SQL语句!

# 快速入门
## 1 建立实体类
```java
//用户类
public class User {
    private long id;
    private String username;
    private String password;
}
```

> 实体类中必须有id属性.若有@Id注解则以@Id注解修饰的属性作为id属性,若无@Id注解则以变量名为id的属性作为id.

> id属性的类型必须为long型!

## 2 导入QuickDAO
QuickDAO基于JDBC,为提高效率,默认只支持数据库连接池.

* 导入commons-dbcp(或者其他的DataSource实现)
* 导入QuickDAO最新版本
```
<dependency>
   <groupId>commons-dbcp</groupId>
   <artifactId>commons-dbcp</artifactId>
   <version>1.4</version>
</dependency>
<dependency>
  <groupId>cn.schoolwow</groupId>
  <artifactId>QuickDAO</artifactId>
  <version>2.10</version>
</dependency>
```

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

# 反馈
目前QuickDAO还不成熟,还在不断完善中.若有问题请提交Issue,作者将第一时间跟进并努力解决.同时欢迎热心认识提交PR,共同完善QuickDAO项目!

# 开源协议
本软件使用 [GPL](http://www.gnu.org/licenses/gpl-3.0.html) 开源协议!
