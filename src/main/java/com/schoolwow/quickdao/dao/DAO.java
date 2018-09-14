package com.schoolwow.quickdao.dao;

import com.alibaba.fastjson.JSONArray;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public interface DAO {
    /**id查询*/
    <T> T fetch(Class<T> _class,long id) throws SQLException, IllegalAccessException, InstantiationException;

    /**Property查询*/
    <T> T fetch(Class<T> _class,String property,Object value) throws SQLException, IllegalAccessException, InstantiationException;

    /**Property查询*/
    <T> Condition<T> query(Class<T> _class);

    /**保存实体*/
    long save(Object instance) throws SQLException, IllegalAccessException, NoSuchFieldException;

    /**保存实体*/
    long save(Object[] instances) throws SQLException, IllegalAccessException, NoSuchFieldException;

    /**保存实体*/
    long save(List instanceList) throws SQLException, IllegalAccessException, NoSuchFieldException;

    /**根据id删除*/
    long delete(Class _class,long id) throws SQLException;

    /**根据属性删除*/
    long delete(Class _class,String property,Object value) throws SQLException;

    /**清空表*/
    long clear(Class _class) throws SQLException;

    /**获取实体类信息*/
    JSONArray getEntityInfo(String packageName,String[] filterPackageName) throws IOException, ClassNotFoundException;

    /**获取数据库信息*/
    JSONArray getDatabaseInfo() throws SQLException;

    /**执行数据库更新操作*/
    void updateDatabase(JSONArray entityList,JSONArray dbEntityList) throws SQLException;
}
