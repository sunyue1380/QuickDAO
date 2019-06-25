package cn.schoolwow.quickdao.domain;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**查询*/
public class Query implements Serializable {
    /**关联类*/
    public Class _class;
    /**关联类名*/
    public String className;
    /**表名*/
    public String tableName;
    /**distinct*/
    public String distinct = "";
    /**列名*/
    public StringBuilder columnBuilder = new StringBuilder();
    /**聚合函数列名*/
    public StringBuilder aggregateColumnBuilder = new StringBuilder();
    /**字段更新*/
    public StringBuilder setBuilder = new StringBuilder();
    /**查询条件*/
    public StringBuilder whereBuilder = new StringBuilder();
    /**分组查询*/
    public StringBuilder groupByBuilder = new StringBuilder("group by ");
    /**排序*/
    public StringBuilder orderByBuilder = new StringBuilder();
    /**分页*/
    public String limit = "";
    /**参数索引*/
    public int parameterIndex = 1;
    /**查询参数*/
    public List parameterList = new ArrayList();
    /**更新参数*/
    public List updateParameterList = new ArrayList();
    /**关联Entity*/
    public transient Entity entity;
    /**状态*/
    public boolean hasDone;
    /**关联子查询*/
    public List<SubQuery> subQueryList = new ArrayList<>();
}
