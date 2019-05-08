package cn.schoolwow.quickdao.domain;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public class QuickDAOConfig {
    /**数据源*/
    public static DataSource dataSource;
    /**待扫描包名*/
    public static Map<String,String> packageNameMap = new HashMap<>();
    /**要忽略的类*/
    public static List<Class> ignoreClassList;
    /**要忽略的包名*/
    public static List<String> ignorePackageNameList;
    /**函数式接口过滤类*/
    public static Predicate<Class> predicate;
    /**是否开启外键约束*/
    public static boolean openForeignKey;
    /**是否启动时自动建表*/
    public static boolean autoCreateTable = true;
}
