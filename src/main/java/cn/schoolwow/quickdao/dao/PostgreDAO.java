package cn.schoolwow.quickdao.dao;

import cn.schoolwow.quickdao.condition.Condition;
import cn.schoolwow.quickdao.condition.PostgreCondition;
import cn.schoolwow.quickdao.domain.Entity;
import cn.schoolwow.quickdao.domain.Property;
import cn.schoolwow.quickdao.helper.SQLHelper;
import cn.schoolwow.quickdao.syntax.PostgreSyntaxHandler;
import cn.schoolwow.quickdao.util.ReflectionUtil;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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
    public Entity[] getDatabaseInfo() throws SQLException {
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
        StringBuilder createTableBuilder = new StringBuilder("create table if not exists \"" + entity.tableName + "\"(");
        Property[] properties = entity.properties;
        for (Property property : properties) {
            createTableBuilder.append("\"" + property.column + "\"");
            if (property.id) {
                createTableBuilder.append(" SERIAL ");
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
        createUniqueKey(entity);
    }

    @Override
    protected void createUniqueKey(Entity entity) throws SQLException {
        if (null == entity.uniqueKeyProperties || entity.uniqueKeyProperties.length == 0) {
            return;
        }
        StringBuilder uniqueKeyBuilder = new StringBuilder("alter table \"" + entity.tableName + "\" add constraint \"" + entity.tableName + "_unique_index\" unique (");
        for (Property property : entity.uniqueKeyProperties) {
            uniqueKeyBuilder.append("\"" + property.column + "\",");
        }
        uniqueKeyBuilder.deleteCharAt(uniqueKeyBuilder.length() - 1);
        uniqueKeyBuilder.append(");");
        String uniqueKeySQL = uniqueKeyBuilder.toString().replaceAll("\\s+", " ");
        logger.debug("[添加唯一性约束]表:{},执行SQL:{}", entity.tableName, uniqueKeySQL);
        connection.prepareStatement(uniqueKeySQL).executeUpdate();
    }

    @Override
    protected boolean isConstraintExist(String constraintName) throws SQLException {
        ResultSet resultSet = connection.prepareStatement("select count(1) from pg_constraint where conname='" + constraintName + "'").executeQuery();
        boolean result = false;
        if (resultSet.next()) {
            result = resultSet.getInt(1) > 0;
        }
        resultSet.close();
        return result;
    }

    @Override
    public void autoBuildDatabase() throws SQLException {
        super.autoBuildDatabase();
        startTransaction();
        Set<String> keySet = ReflectionUtil.entityMap.keySet();
        for(String key:keySet){
            Entity entity = ReflectionUtil.entityMap.get(key);
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
        commit();
        endTransaction();
    }
}
