package cn.schoolwow.quickdao.condition;

import java.util.List;

public interface Condition<T> {
    /**添加空查询*/
    Condition addNullQuery(String field);
    /**添加非空查询*/
    Condition addNotNullQuery(String field);
    /**添加非空查询*/
    Condition addNotEmptyQuery(String field);
    /**添加范围语句*/
    Condition addInQuery(String field, Object[] values);
    /**添加范围语句*/
    Condition addInQuery(String field, List values);
    /**添加自定义查询条件*/
    Condition addQuery(String query);
    /**添加属性查询*/
    Condition addQuery(String property, Object value);
    /**添加属性查询*/
    Condition addQuery(String property, String operator, Object value);
    /**添加更新字段*/
    Condition addUpdate(String property, Object value);
//    Condition groupBy(String field);
//    Condition having(String query);
    /**关联表*/
    <T> SubCondition<T> joinTable(Class<T> _class, String primaryField, String joinTableField);

    /**排序字段(升序)*/
    Condition orderBy(String field);
    /**排序字段(降序)*/
    Condition orderByDesc(String field);
    /**分页操作*/
    Condition limit(long offset, long limit);
    /**分页操作*/
    Condition page(int pageNum,int pageSize);
    Condition addColumn(String field);

//    Condition done();

    long count();
    long update();
    long delete();
    List<T> getList();
    List<T> getValueList(Class<T> _class, String column);
}
