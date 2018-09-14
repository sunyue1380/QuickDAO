package com.schoolwow.quickdao;

import com.alibaba.fastjson.JSONArray;
import com.schoolwow.quickdao.dao.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class QuickDAO {
    Logger logger = LoggerFactory.getLogger(QuickDAO.class);
    private static HashMap<String,Class> driverMapping = new HashMap();
    static{
        driverMapping.put("jdbc:h2",AbstractDAO.class);
        driverMapping.put("jdbc:sqlite", SQLiteDAO.class);
        driverMapping.put("jdbc:mysql", MySQLDAO.class);
    }
    DAO dao;
    public QuickDAO(DataSource dataSource, String packageName) {
        this(dataSource,packageName,null);
    }
    public QuickDAO(DataSource dataSource, String packageName,String[] filterNames) {
        try {
            Connection connection = dataSource.getConnection();
            String url = connection.getMetaData().getURL();
            Set<String> keySet = driverMapping.keySet();
            for(String key:keySet){
                if(url.contains(key)){
                    dao = (DAO) driverMapping.get(key).getConstructor(DataSource.class).newInstance(dataSource);
                    break;
                }
            }
            if(dao==null){
                throw new UnsupportedOperationException("No proper adapter for db:"+url);
//                logger.warn("No proper adapter for db:"+url+"! using default dao policy!");
//                dao = new AbstractDAO(dataSource);
            }
            JSONArray sourceEntityList = dao.getEntityInfo(packageName,filterNames);
            JSONArray targetEntityList = dao.getDatabaseInfo();
            dao.updateDatabase(sourceEntityList,targetEntityList);
        } catch (SQLException | NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException | IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**根据id查找*/
    public <T> T fetch(Class<T> _class, long id) {
        try {
            return dao.fetch(_class,id);
        } catch (SQLException | IllegalAccessException | InstantiationException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**根据id查找*/
    public <T> T fetch(Class<T> _class,String property,Object value) {
        try {
            return dao.fetch(_class,property,value);
        } catch (SQLException | IllegalAccessException | InstantiationException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**根据id查找*/
    public <T> Condition<T> query(Class<T> _class) {
        return dao.query(_class);
    }

    /**根据id查找*/
    public long save(Object instance) {
        try {
            return dao.save(instance);
        } catch (SQLException | IllegalAccessException | NoSuchFieldException e) {
            e.printStackTrace();
        }
        return 0;
    }
    /**保存实体*/
    public long save(Object[] instances) {
        try {
            return dao.save(instances);
        } catch (SQLException | IllegalAccessException | NoSuchFieldException e) {
            e.printStackTrace();
        }
        return 0;
    }
    /**保存实体*/
    public long save(List instanceList) {
        try {
            return dao.save(instanceList);
        } catch (SQLException | IllegalAccessException | NoSuchFieldException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public long delete(Class _class,long id) {
        try {
            return dao.delete(_class,id);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public long delete(Class _class,String property,Object value) {
        try {
            return dao.delete(_class,property,value);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**清空表*/
    public long clear(Class _class) {
        try {
            return dao.clear(_class);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }
}
