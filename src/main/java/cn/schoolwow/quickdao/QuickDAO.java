package cn.schoolwow.quickdao;

import cn.schoolwow.quickdao.dao.AbstractDAO;
import cn.schoolwow.quickdao.dao.DAO;
import cn.schoolwow.quickdao.dao.MySQLDAO;
import cn.schoolwow.quickdao.dao.SQLiteDAO;
import cn.schoolwow.quickdao.util.ValidateUtil;
import com.alibaba.fastjson.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Set;
import java.util.function.Predicate;

public class QuickDAO {
    Logger logger = LoggerFactory.getLogger(QuickDAO.class);
    private static HashMap<String,Class> driverMapping = new HashMap();
    static{
        driverMapping.put("jdbc:h2",MySQLDAO.class);
        driverMapping.put("jdbc:sqlite", SQLiteDAO.class);
        driverMapping.put("jdbc:mysql", MySQLDAO.class);
    }

    private DataSource dataSource;
    private String packageName;
    private String regexPattern;
    private String[] excludePackageNames;
    private Predicate<Class> predicate;

    public static QuickDAO newInstance(){
        return new QuickDAO();
    }

    public QuickDAO dataSource(DataSource dataSource){
        this.dataSource = dataSource;
//        logger.debug("[设置数据源]{}",dataSource);
        return this;
    }
    public QuickDAO packageName(String packageName){
        this.packageName = packageName;
        logger.debug("[设置实体类包]{}",packageName);
        return this;
    }
    public QuickDAO filter(String regexPattern){
        this.regexPattern = regexPattern;
        logger.debug("[过滤包-正则表达式]{}",regexPattern);
        return this;
    }
    public QuickDAO filter(String[] excludePackageNames){
        this.excludePackageNames = excludePackageNames;
        logger.debug("[过滤包-正则表达式]{}", JSON.toJSONString(excludePackageNames));
        return this;
    }
    public QuickDAO filter(Predicate<Class> predicate){
        this.predicate = predicate;
        logger.debug("[过滤包-函数式接口]{}",predicate);
        return this;
    }

    public DAO build() {
        if(ValidateUtil.isNull(packageName)||ValidateUtil.isNull(dataSource)){
            throw new IllegalArgumentException("packageName和dataSource不能为空!");
        }
        AbstractDAO dao = null;
        try {
            Connection connection = dataSource.getConnection();
            String url = connection.getMetaData().getURL();
            logger.info("[数据源地址]{}",url);
            Set<String> keySet = driverMapping.keySet();
            for(String key:keySet){
                if(url.contains(key)){
                    dao = (AbstractDAO) driverMapping.get(key).getConstructor(DataSource.class).newInstance(dataSource);
                    break;
                }
            }
            if(dao==null){
                throw new UnsupportedOperationException("当前数据源没有合适的适配器.数据源地址:"+url);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        //自动建表并返回
        if(ValidateUtil.isNotEmpty(this.regexPattern)){
            dao.autoBuildDatabase(packageName,this.regexPattern);
        }else if(ValidateUtil.isNotEmpty(this.excludePackageNames)){
            dao.autoBuildDatabase(packageName,excludePackageNames);
        }else if(ValidateUtil.isNull(this.predicate)){
            dao.autoBuildDatabase(packageName,predicate);
        }else{
            dao.autoBuildDatabase(packageName);
        }
        return dao;
    }
}
