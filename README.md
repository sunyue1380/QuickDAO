# QuickDAO
QuickDAO是一款简单,易用的java ORM框架.

# 支持数据库
目前版本只支持数据库Mysql,SQlite以及H2.

# 快速入门
# 1 导入QuickDAO
## 1.1 使用maven
```
<dependency>
  <groupId>cn.schoolwow</groupId>
  <artifactId>QuickDAO</artifactId>
  <version>1.0</version>
</dependency>
```
## 1.2 下载单独jar包
请[点击此处](http://pan.schoolwow.cn/f/a8591b2cc8af4e1cae9f/?dl=1)下载jar包

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
BasicDataSource basicDataSource = new BasicDataSource();
basicDataSource.setDriverClassName("com.mysql.jdbc.Driver");
basicDataSource.setUrl("jdbc:mysql://127.0.0.1:3306/quickvideo");
basicDataSource.setUsername("root");
basicDataSource.setPassword("password");
QuickDAO quickDAO = new QuickDAO(basicDataSource,"cn.schoolwow.quickdaoer.entity");
```

到此QuickDAO初始化完成. 

> QuickDAO目前只支持实体主键为int和long型主键,且主键变量名必须为``id``.

## 下一步

* [实体注解](https://gitee.com/648823596/QuickDAO/wikis/pages?title=%E5%AE%9E%E4%BD%93%E6%B3%A8%E8%A7%A3&parent=) - 自定义实体注解(可略过)
* [接口文档](https://gitee.com/648823596/QuickDAO/wikis/pages?title=%E6%8E%A5%E5%8F%A3%E6%96%87%E6%A1%A3&parent=) - 使用QuickDAO所提供的接口进行增删查改

## 反馈
目前项目处于初期阶段,欢迎留言反馈,作者将第一时间跟进并努力解决.


#### 参与贡献
1. Fork 本项目
2. 新建 fork 分支
3. 提交代码
4. 新建 Pull Request