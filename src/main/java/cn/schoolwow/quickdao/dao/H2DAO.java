package cn.schoolwow.quickdao.dao;

import cn.schoolwow.quickdao.condition.Condition;
import cn.schoolwow.quickdao.condition.H2Condition;
import cn.schoolwow.quickdao.helper.SQLHelper;
import cn.schoolwow.quickdao.syntax.H2SyntaxHandler;

import javax.sql.DataSource;

public class H2DAO extends MySQLDAO {
    public H2DAO(DataSource dataSource) {
        super(dataSource);
        fieldMapping.put("long", "INTEGER");
        fieldMapping.put("float", "REAL");
        fieldMapping.put("double", "DOUBLE");
        syntaxHandler = new H2SyntaxHandler();
        sqlHelper = new SQLHelper(syntaxHandler);
    }

    @Override
    public <T> Condition<T> query(Class<T> _class) {
        return new H2Condition(_class, dataSource, this, syntaxHandler, sqlHelper);
    }
}
