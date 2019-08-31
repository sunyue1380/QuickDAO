package cn.schoolwow.quickdao.condition.subCondition;

import cn.schoolwow.quickdao.condition.AbstractCondition;
import cn.schoolwow.quickdao.domain.Query;

public class SQLiteSubCondition<T> extends AbstractSubCondition<T> {
    public SQLiteSubCondition(Class<T> _class, String tableAliasName, String primaryField, String joinTableField, String compositField, AbstractCondition condition, Query query) {
        super(_class, tableAliasName, primaryField, joinTableField, compositField, condition, query);
    }

    @Override
    public SubCondition rightJoin() {
        throw new UnsupportedOperationException("RIGHT and FULL OUTER JOINs are not currently supported");
    }
}
