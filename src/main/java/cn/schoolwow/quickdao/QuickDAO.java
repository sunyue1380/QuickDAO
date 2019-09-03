package cn.schoolwow.quickdao;

import cn.schoolwow.quickdao.dao.*;
import cn.schoolwow.quickdao.util.QuickDAOConfig;
import cn.schoolwow.quickdao.util.ReflectionUtil;
import cn.schoolwow.quickdao.util.ValidateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.function.Predicate;

public class QuickDAO {
    Logger logger = LoggerFactory.getLogger(QuickDAO.class);
    private static HashMap<String, Class> driverMapping = new HashMap();

    static {
        driverMapping.put("jdbc:h2", H2DAO.class);
        driverMapping.put("jdbc:sqlite", SQLiteDAO.class);
        driverMapping.put("jdbc:mysql", MySQLDAO.class);
        driverMapping.put("jdbc:postgresql", PostgreDAO.class);
        driverMapping.put("jdbc:sqlserver", SQLServerDAO.class);
    }

    public static QuickDAO newInstance() {
        return new QuickDAO();
    }

    public QuickDAO dataSource(DataSource dataSource) {
        QuickDAOConfig.dataSource = dataSource;
        return this;
    }

    public QuickDAO packageName(String packageName) {
        QuickDAOConfig.packageNameMap.put(packageName, "");
        return this;
    }

    public QuickDAO packageName(String packageName, String prefix) {
        QuickDAOConfig.packageNameMap.put(packageName, prefix + "_");
        return this;
    }

    public QuickDAO ignorePackageName(String ignorePackageName) {
        if (QuickDAOConfig.ignorePackageNameList == null) {
            QuickDAOConfig.ignorePackageNameList = new ArrayList<>();
        }
        QuickDAOConfig.ignorePackageNameList.add(ignorePackageName);
        return this;
    }

    public QuickDAO ignoreClass(Class _class) {
        if (QuickDAOConfig.ignoreClassList == null) {
            QuickDAOConfig.ignoreClassList = new ArrayList<>();
        }
        QuickDAOConfig.ignoreClassList.add(_class);
        return this;
    }

    public QuickDAO filter(Predicate<Class> predicate) {
        QuickDAOConfig.predicate = predicate;
        return this;
    }

    public QuickDAO foreignKey(boolean openForeignKey) {
        QuickDAOConfig.openForeignKey = openForeignKey;
        return this;
    }

    public QuickDAO autoCreateTable(boolean autoCreateTable) {
        QuickDAOConfig.autoCreateTable = autoCreateTable;
        return this;
    }

    public DAO build() {
        if (QuickDAOConfig.packageNameMap.isEmpty()) {
            throw new IllegalArgumentException("请设置要扫描的实体类包名!");
        }
        if (ValidateUtil.isNull(QuickDAOConfig.dataSource)) {
            throw new IllegalArgumentException("请设置数据库连接池属性!");
        }
        AbstractDAO dao = null;
        try {
            Connection connection = QuickDAOConfig.dataSource.getConnection();
            String url = connection.getMetaData().getURL();
            logger.info("[数据源地址]{}", url);
            Set<String> keySet = driverMapping.keySet();
            for (String key : keySet) {
                if (url.contains(key)) {
                    dao = (AbstractDAO) driverMapping.get(key).getConstructor(DataSource.class).newInstance(QuickDAOConfig.dataSource);
                    break;
                }
            }
            if (dao == null) {
                throw new UnsupportedOperationException("当前数据源没有合适的适配器.数据源地址:" + url);
            }
            ReflectionUtil.getEntityInfo();
            //自动建表
            dao.autoBuildDatabase();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        return dao;
    }
}
