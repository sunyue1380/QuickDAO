package cn.schoolwow.quickdao.dao;

import cn.schoolwow.quickdao.condition.Condition;
import cn.schoolwow.quickdao.condition.SQLServerCondition;
import cn.schoolwow.quickdao.domain.Entity;
import cn.schoolwow.quickdao.domain.Property;
import cn.schoolwow.quickdao.helper.SQLHelper;
import cn.schoolwow.quickdao.syntax.SQLServerSyntaxHandler;
import cn.schoolwow.quickdao.syntax.Syntax;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class SQLServerDAO extends MySQLDAO{
    public SQLServerDAO(DataSource dataSource) {
        super(dataSource);
        fieldMapping.put("boolean","bit");
        fieldMapping.put("float", "float(24)");
        fieldMapping.put("double", "float(53)");
        fieldMapping.put("date", "datetime");
        syntaxHandler = new SQLServerSyntaxHandler();
        sqlHelper = new SQLHelper(syntaxHandler);
    }

    @Override
    public <T> Condition<T> query(Class<T> _class) {
        return new SQLServerCondition(_class, dataSource, this, syntaxHandler, sqlHelper);
    }

    @Override
    protected Entity[] getDatabaseInfo() throws SQLException {
        Connection connection = dataSource.getConnection();
        connection.setAutoCommit(false);
        PreparedStatement tablePs = connection.prepareStatement("select name from sysobjects where xtype='u' order by name;");
        ResultSet tableRs = tablePs.executeQuery();
        List<Entity> entityList = new ArrayList<>();
        while (tableRs.next()) {
            Entity entity = new Entity();
            entity.tableName = tableRs.getString(1);

            List<Property> propertyList = new ArrayList<>();
            PreparedStatement propertyPs = connection.prepareStatement("select column_name,data_type,is_nullable from information_schema.columns where table_name = '"+entity.tableName+"'");
            ResultSet propertiesRs = propertyPs.executeQuery();
            while (propertiesRs.next()) {
                Property property = new Property();
                property.column = propertiesRs.getString("column_name");
                property.columnType = propertiesRs.getString("data_type");
                property.notNull = "NO".equals(propertiesRs.getString("is_nullable"));
                propertyList.add(property);
            }
            entity.properties = propertyList.toArray(new Property[0]);
            entityList.add(entity);
            propertiesRs.close();
            propertyPs.close();
        }
        tableRs.close();
        connection.close();
        return entityList.toArray(new Entity[0]);
    }

    @Override
    protected void createTable(Entity entity) throws SQLException {
        StringBuilder createTableBuilder = new StringBuilder("create table " + syntaxHandler.getSyntax(Syntax.Escape,entity.tableName) + "(");
        Property[] properties = entity.properties;
        for (Property property : properties) {
            createTableBuilder.append(syntaxHandler.getSyntax(Syntax.Escape,property.column) + " " + property.columnType);
            if (property.id) {
                createTableBuilder.append(" identity(1,1) unique ");
            } else {
                if (property.defaultValue != null) {
                    createTableBuilder.append(" default '" + property.defaultValue + "'");
                }
                if (property.notNull) {
                    createTableBuilder.append(" not null ");
                }
            }
            createTableBuilder.append(",");
        }
        createTableBuilder.deleteCharAt(createTableBuilder.length() - 1);
        createTableBuilder.append(")");
        String sql = createTableBuilder.toString().replaceAll("\\s+", " ");
        logger.debug("[生成新表]类名:{},表名:{},执行SQL:{},", entity.className, entity.tableName, sql);
        connection.prepareStatement(sql).executeUpdate();
    }

    @Override
    protected boolean isIndexExists(String tableName,String indexName) throws SQLException {
        String indexExistsSQL = "EXEC Sp_helpindex '"+tableName+"'";
        logger.debug("[查看索引]表名:{},执行SQL:{}",tableName,indexExistsSQL);
        ResultSet resultSet = connection.prepareStatement(indexExistsSQL).executeQuery();
        boolean result = false;
        while(resultSet.next()) {
            if(indexName.equals(resultSet.getString("index_name"))){
                result = true;
                break;
            }
        }
        resultSet.close();
        return result;
    }
}
