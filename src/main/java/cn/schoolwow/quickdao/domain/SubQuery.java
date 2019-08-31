package cn.schoolwow.quickdao.domain;

import cn.schoolwow.quickdao.condition.AbstractCondition;
import cn.schoolwow.quickdao.condition.subCondition.SubCondition;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 子查询
 */
public class SubQuery<T> implements Serializable {
    /**
     * 关联类
     */
    public Class _class;
    /**
     * 关联类名
     */
    public String className;
    /**
     * 表别名
     */
    public String tableAliasName;
    /**
     * 主表字段
     */
    public String primaryField;
    /**
     * 子表字段
     */
    public String joinTableField;
    /**
     * 对象变量名
     */
    public String compositField;
    /**
     * 连接方式
     */
    public String join = "join";
    /**
     * 查询条件
     */
    public StringBuilder whereBuilder = new StringBuilder();
    /**
     * 查询参数
     */
    public List parameterList = new ArrayList();
    /**
     * 上级子查询
     */
    public SubQuery parentSubQuery;
    /**
     * 上级子查询
     */
    public transient SubCondition parentSubCondition;
    /**
     * 主表
     */
    public Query query;
    /**
     * 父表
     */
    public AbstractCondition condition;
}
