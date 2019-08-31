package cn.schoolwow.quickdao.condition.subCondition;

import cn.schoolwow.quickdao.condition.Condition;

import java.util.List;

public interface SubCondition<T> {
    SubCondition leftJoin();

    SubCondition rightJoin();

    <T> SubCondition<T> joinTable(Class<T> _class, String primaryField, String joinTableField);

    <T> SubCondition<T> joinTable(Class<T> _class, String primaryField, String joinTableField, String compositField);

    SubCondition addNullQuery(String field);

    SubCondition addNotNullQuery(String field);

    SubCondition addNotEmptyQuery(String field);

    SubCondition addInQuery(String field, Object[] values);

    SubCondition addInQuery(String field, List values);

    SubCondition addNotInQuery(String field, Object[] values);

    SubCondition addNotInQuery(String field, List values);

    SubCondition addQuery(String query);

    SubCondition addQuery(String property, Object value);

    SubCondition addQuery(String property, String operator, Object value);

    /**
     * 根据指定字段升序排列
     *
     * @param field 升序排列字段名
     */
    SubCondition orderBy(String field);

    /**
     * 根据指定字段降序排列
     *
     * @param field 降序排列字段名
     */
    SubCondition orderByDesc(String field);

    SubCondition doneSubCondition();

    Condition done();
}
