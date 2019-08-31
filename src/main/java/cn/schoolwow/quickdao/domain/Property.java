package cn.schoolwow.quickdao.domain;

import java.lang.reflect.Field;

/**
 * 实体类属性信息
 */
public class Property {
    /**
     * 列名
     */
    public String column;
    /**
     * 数据库类型
     */
    public String columnType;
    /**
     * 属性名
     */
    public String name;
    /**
     * 类型(简单类名)
     */
    public String type;
    /**
     * 是否唯一
     */
    public boolean unique;
    /**
     * 是否非空
     */
    public boolean notNull;
    /**
     * 是否是id
     */
    public boolean id;
    /**
     * 默认值
     */
    public String defaultValue;
    /**
     * 注释
     */
    public String comment;
    /**
     * 外键关联
     */
    public String foreignKey;
    /**
     * 外键约束名称
     */
    public String foreignKeyName;
    /**
     * Field对象
     */
    public Field field;
}
