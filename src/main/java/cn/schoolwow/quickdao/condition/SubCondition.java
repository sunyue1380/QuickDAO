package cn.schoolwow.quickdao.condition;

import java.util.List;

public interface SubCondition<T> {
    SubCondition addNullQuery(String field);
    SubCondition addNotNullQuery(String field);
    SubCondition addNotEmptyQuery(String field);
    SubCondition addInQuery(String field, Object[] values);
    SubCondition addInQuery(String field, List values);
    SubCondition addQuery(String query);
    SubCondition addQuery(String property, Object value);
    SubCondition addQuery(String property, String operator, Object value);
    Condition done();
}
