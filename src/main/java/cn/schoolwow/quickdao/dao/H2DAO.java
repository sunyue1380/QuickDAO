package cn.schoolwow.quickdao.dao;

import cn.schoolwow.quickdao.condition.Condition;
import cn.schoolwow.quickdao.condition.H2Condition;
import cn.schoolwow.quickdao.helper.SQLHelper;
import cn.schoolwow.quickdao.syntax.H2SyntaxHandler;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;

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

    @Override
    protected void createForeignKey() throws SQLException {

    }

    @Override
    protected boolean isIndexExists(String tableName,String indexName) throws SQLException {
        String indexExistsSQL = "select count(1) from information_schema.CONSTRAINTS where CONSTRAINT_NAME = '"+indexName+"'";
        logger.debug("[查看索引]表名:{},执行SQL:{}",tableName,indexExistsSQL);
        ResultSet resultSet = connection.prepareStatement(indexExistsSQL).executeQuery();
        boolean result = false;
        if (resultSet.next()) {
            result = resultSet.getInt(1) > 0;
        }
        resultSet.close();
        return result;
    }
}
