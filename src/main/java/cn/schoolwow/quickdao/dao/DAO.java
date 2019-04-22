package cn.schoolwow.quickdao.dao;

import cn.schoolwow.quickdao.condition.Condition;

import java.sql.Savepoint;
import java.util.List;

public interface DAO {
    /**根据id查询*/
    <T> T fetch(Class<T> _class, long id);

    /**根据属性查询*/
    <T> T fetch(Class<T> _class, String property, Object value);

    /**根据属性查询*/
    <T> List<T> fetchList(Class<T> _class, String property, Object value);

    /**Property查询*/
    <T> Condition<T> query(Class<T> _class);

    /**保存实体*/
    long save(Object instance);

    /**保存实体*/
    long save(Object[] instances);

    /**保存实体*/
    long save(List instanceList);

    /**根据id删除*/
    long delete(Class _class, long id);

    /**根据属性删除*/
    long delete(Class _class, String property, Object value);

    /**清空表*/
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
