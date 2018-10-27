package com.schoolwow.quickdao.dao;

import java.util.List;

public interface Condition<T> {
    Condition addNullQuery(String field);
    Condition addNotNullQuery(String field);
    Condition addInQuery(String field, Object[] values);
    Condition addInQuery(String field, List values);
    Condition addQuery(String query);
    Condition addQuery(String property, Object value);
    Condition addQuery(String property, String operator, Object value);
    Condition addUpdate(String property, Object value);
//    Condition groupBy(String field);
//    Condition having(String query);
    <T> SubCondition<T> joinTable(Class<T> _class, String primaryField, String joinTableField);

    Condition orderBy(String field);
    Condition orderByDesc(String field);
    Condition limit(long offset, long limit);
    Condition addColumn(String field);

    Condition done();

    long count();
    long update();
    long delete();
    List<T> getList();
    List<T> getValueList(Class<T> _class, String column);
}
