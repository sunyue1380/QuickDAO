package cn.schoolwow.quickdao.dao;

import cn.schoolwow.quickdao.condition.Condition;

import java.sql.Savepoint;
import java.util.List;

/**
 * 数据库更新操作
 * */
public interface DAO {
    /**
     * <p>根据id查询</p>
     * @param _class 类名
     * @param id 指定要查询id字段的值
     * */
    <T> T fetch(Class<T> _class, long id);

    /**
     * <p>根据属性查询单个记录</p>
     * <p>若符合条件的记录有多条,则只会返回第一条记录</p>
     * @param _class 类名
     * @param property 指定要查询的字段
     * @param value 指定要查询的字段的值
     * */
    <T> T fetch(Class<T> _class, String property, Object value);

    /**
     * <p>根据属性查询记录</p>
     * @param _class 类名
     * @param property 指定要查询的字段
     * @param value 指定要查询的字段的值
     * */
    <T> List<T> fetchList(Class<T> _class, String property, Object value);

    /**
     * <p>复杂查询接口</p>
     * @param _class 类名,指定要查询的表
     * */
    <T> Condition<T> query(Class<T> _class);

    /**
     * <p>保存对象</p>
     * <p>判断该实例是否有id,无id则直接插入;然后判断该实例是否有唯一性约束,若有则根据唯一性约束更新,否则根据id更新</p>
     * @param instance 要保存的实例
     * */
    long save(Object instance);

    /**
     * <p>保存对象数组</p>
     * <p>判断该实例是否有id,无id则直接插入;然后判断该实例是否有唯一性约束,若有则根据唯一性约束更新,否则根据id更新</p>
     * @param instances 要保存的实例
     * */
    long save(Object[] instances);

    /**
     * <p>保存对象数组</p>
     * <p>判断该实例是否有id,无id则直接插入;然后判断该实例是否有唯一性约束,若有则根据唯一性约束更新,否则根据id更新</p>
     * @param instanceList 要保存的实例
     * */
    long save(List instanceList);

    /**
     * <p>根据id删除</p>
     * @param _class 类名,对应数据库中的一张表
     * @param id 要删除的id
     * */
    long delete(Class _class, long id);

    /**
     * <p>根据id删除</p>
     * @param _class 类名,对应数据库中的一张表
     * @param field 要删除的字段名
     * @param value 要删除的字段的值
     * */
    long delete(Class _class, String field, Object value);

    /**
     * <p>清空表</p>
     * @param _class 类名,对应数据库中的一张表
     * */
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
