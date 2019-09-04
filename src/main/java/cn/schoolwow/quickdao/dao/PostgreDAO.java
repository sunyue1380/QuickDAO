package cn.schoolwow.quickdao.dao;

import cn.schoolwow.quickdao.condition.Condition;
import cn.schoolwow.quickdao.condition.PostgreCondition;
import cn.schoolwow.quickdao.domain.Entity;
import cn.schoolwow.quickdao.domain.Property;
import cn.schoolwow.quickdao.helper.SQLHelper;
import cn.schoolwow.quickdao.syntax.PostgreSyntaxHandler;
import cn.schoolwow.quickdao.syntax.Syntax;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class PostgreDAO extends MySQLDAO {
    public PostgreDAO(DataSource dataSource) {
        super(dataSource);
        fieldMapping.put("byte", "smallint");
        fieldMapping.put("date", "timestamp");
        fieldMapping.put("float", "real");
        fieldMapping.put("double", "double precision");
        syntaxHandler = new PostgreSyntaxHandler();
        sqlHelper = new SQLHelper(syntaxHandler);
    }

    @Override
    public <T> Condition<T> query(Class<T> _class) {
        return new PostgreCondition(_class, dataSource, this, syntaxHandler, sqlHelper);
    }

    @Override
    protected Entity[] getDatabaseInfo() throws SQLException {
        Connection connection = dataSource.getConnection();
        connection.setAutoCommit(false);
        PreparedStatement tablePs = connection.prepareStatement("select tablename from pg_tables where schemaname='public';");
        ResultSet tableRs = tablePs.executeQuery();
        List<Entity> entityList = new ArrayList<>();
        while (tableRs.next()) {
            Entity entity = new Entity();
            entity.tableName = tableRs.getString(1);

            List<Property> propertyList = new ArrayList<>();
            PreparedStatement propertyPs = connection.prepareStatement("select column_name,column_default,is_nullable,udt_name from information_schema.columns where table_name = '" + tableRs.getString(1) + "'");
            ResultSet propertiesRs = propertyPs.executeQuery();
            while (propertiesRs.next()) {
                Property property = new Property();
                property.column = propertiesRs.getString("column_name");
                property.columnType = propertiesRs.getString("udt_name");
                property.notNull = "NO".equals(propertiesRs.getString("is_nullable"));
                if (null != propertiesRs.getString("column_default")) {
                    property.defaultValue = propertiesRs.getString("column_default");
                }
                propertyList.add(property);
            }
            entity.properties = propertyList.toArray(new Property[0]);
            entityList.add(entity);
            propertiesRs.close();
            propertyPs.close();
        }
        tableRs.close();
        tablePs.close();
        connection.close();
        return entityList.toArray(new Entity[0]);
    }

    @Override
    protected void createTable(Entity entity) throws SQLException {
        StringBuilder createTableBuilder = new StringBuilder("create table if not exists " + syntaxHandler.getSyntax(Syntax.Escape,entity.tableName) + "(");
        Property[] properties = entity.properties;
        for (Property property : properties) {
            createTableBuilder.append(syntaxHandler.getSyntax(Syntax.Escape,property.column));
            if (property.id) {
                createTableBuilder.append(" SERIAL unique ");
            } else {
                createTableBuilder.append(" " + property.columnType + " ");
            }
            if (null != property.defaultValue) {
                createTableBuilder.append(" default '" + property.defaultValue + "'");
            }
            if (property.notNull) {
                createTableBuilder.append(" not null ");
            }
            createTableBuilder.append(",");
        }
        createTableBuilder.deleteCharAt(createTableBuilder.length() - 1);
        createTableBuilder.append(")");
        String sql = createTableBuilder.toString().replaceAll("\\s+", " ");
        logger.debug("[生成新表]类名:{},表名:{},执行SQL:{}", entity.className, entity.tableName, sql);
        connection.prepareStatement(sql).executeUpdate();
        createComment(entity);
    }

    @Override
    protected boolean isIndexExists(String tableName,String indexName) throws SQLException {
        String indexExistsSQL = "select count(1) from pg_indexes where tablename = '"+tableName+"' and indexname = '"+indexName+"'";
        logger.debug("[查看索引]表名:{},执行SQL:{}",tableName,indexExistsSQL);
        ResultSet resultSet = connection.prepareStatement(indexExistsSQL).executeQuery();
        boolean result = false;
        if (resultSet.next()) {
            result = resultSet.getInt(1) > 0;
        }
        resultSet.close();
        return result;
    }

    @Override
    protected void dropIndex(String tableName, String indexName) throws SQLException {
        if (isIndexExists(tableName,indexName)) {
            String dropIndexSQL = "drop index "+syntaxHandler.getSyntax(Syntax.Escape,indexName);
            logger.debug("[删除索引]表:{},执行SQL:{}", tableName, dropIndexSQL);
            connection.prepareStatement(dropIndexSQL).executeUpdate();
        }
    }

    private void createComment(Entity entity) throws SQLException {
        String sql = "comment on table \""+entity.tableName+"\" is '"+entity.comment+"'";
        logger.debug("[执行SQL]{}",sql);
        connection.prepareStatement(sql).executeUpdate();
        for(Property property:entity.properties){
            if(property.comment==null){
                continue;
            }
            sql = "comment on column \""+entity.tableName+"\".\""+property.column+"\" is '"+property.comment+"'";
            logger.debug("[执行SQL]{}",sql);
            connection.prepareStatement(sql).executeUpdate();
        }
    }
}
