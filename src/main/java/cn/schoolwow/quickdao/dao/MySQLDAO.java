package cn.schoolwow.quickdao.dao;

import cn.schoolwow.quickdao.condition.Condition;
import cn.schoolwow.quickdao.condition.MySQLCondition;
import cn.schoolwow.quickdao.domain.Entity;
import cn.schoolwow.quickdao.domain.Property;
import cn.schoolwow.quickdao.helper.SQLHelper;
import cn.schoolwow.quickdao.syntax.MySQLSyntaxHandler;
import cn.schoolwow.quickdao.syntax.Syntax;
import cn.schoolwow.quickdao.util.ReflectionUtil;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MySQLDAO extends AbstractDAO {

    public MySQLDAO(DataSource dataSource) {
        super(dataSource);
        fieldMapping.put("char", "char(4)");
        fieldMapping.put("integer", "integer(11)");
        fieldMapping.put("long", "INTEGER");
        fieldMapping.put("float", "float(4,2)");
        fieldMapping.put("double", "double(5,2)");
        syntaxHandler = new MySQLSyntaxHandler();
        sqlHelper = new SQLHelper(syntaxHandler);
    }

    @Override
    public <T> Condition<T> query(Class<T> _class) {
        return new MySQLCondition(_class, dataSource, this, syntaxHandler, sqlHelper);
    }

    @Override
    protected Entity[] getDatabaseInfo() throws SQLException {
        Connection connection = dataSource.getConnection();
        connection.setAutoCommit(false);
        PreparedStatement tablePs = connection.prepareStatement("show tables;");
        ResultSet tableRs = tablePs.executeQuery();
        List<Entity> entityList = new ArrayList<>();
        while (tableRs.next()) {
            Entity entity = new Entity();
            entity.tableName = tableRs.getString(1);

            List<Property> propertyList = new ArrayList<>();
            PreparedStatement propertyPs = connection.prepareStatement("show columns from " + syntaxHandler.getSyntax(Syntax.Escape,tableRs.getString(1)));
            ResultSet propertiesRs = propertyPs.executeQuery();
            while (propertiesRs.next()) {
                Property property = new Property();
                property.column = propertiesRs.getString("Field");
                property.columnType = propertiesRs.getString("Type");
                property.notNull = "NO".equals(propertiesRs.getString("Null"));
                property.unique = "UNI".equals(propertiesRs.getString("Key"));
                if (null != propertiesRs.getString("Default")) {
                    property.defaultValue = propertiesRs.getString("Default");
                }
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
        StringBuilder createTableBuilder = new StringBuilder("create table if not exists " + syntaxHandler.getSyntax(Syntax.Escape,entity.tableName) + "(");
        Property[] properties = entity.properties;
        for (Property property : properties) {
            createTableBuilder.append(syntaxHandler.getSyntax(Syntax.Escape,property.column) + " " + property.columnType);
            if (property.id) {
                createTableBuilder.append(" primary key " + syntaxHandler.getSyntax(Syntax.AutoIncrement));
            } else {
                if (property.defaultValue != null) {
                    createTableBuilder.append(" default '" + property.defaultValue + "'");
                }
                if (property.notNull) {
                    createTableBuilder.append(" not null ");
                }
            }
            if (null != property.comment) {
                createTableBuilder.append(" "+syntaxHandler.getSyntax(Syntax.Comment, property.comment));
            }
            createTableBuilder.append(",");
        }
        createTableBuilder.deleteCharAt(createTableBuilder.length() - 1);
        createTableBuilder.append(")");
        if (null != entity.comment) {
            createTableBuilder.append(syntaxHandler.getSyntax(Syntax.Comment, entity.comment));
        }
        String sql = createTableBuilder.toString().replaceAll("\\s+", " ");
        logger.debug("[生成新表]类名:{},表名:{},执行SQL:{},", entity.className, entity.tableName, sql);
        connection.prepareStatement(sql).executeUpdate();
    }

    @Override
    protected void createForeignKey() throws SQLException {
        for (Entity entity : ReflectionUtil.entityMap.values()) {
            Property[] foreignKeyProperties = entity.foreignKeyProperties;
            if(null==foreignKeyProperties){
                continue;
            }
            for (Property property : foreignKeyProperties) {
                String operation = property.foreignKey.foreignKeyOption().getOperation();
                String reference = syntaxHandler.getSyntax(Syntax.Escape,ReflectionUtil.entityMap.get(property.foreignKey.table().getName()).tableName) + "(" + syntaxHandler.getSyntax(Syntax.Escape,property.foreignKey.field()) + ") ON DELETE " + operation + " ON UPDATE " + operation;
                String foreignKeyName = "FK_" + entity.tableName + "_" + property.foreignKey.field() + "_" + ReflectionUtil.entityMap.get(property.foreignKey.table().getName()).tableName + "_" + property.name;
                if (isConstraintExists(entity.tableName,foreignKeyName)) {
                    continue;
                }
                String foreignKeySQL = "alter table " + syntaxHandler.getSyntax(Syntax.Escape, entity.tableName) + " add constraint " + syntaxHandler.getSyntax(Syntax.Escape, foreignKeyName) + " foreign key(" + syntaxHandler.getSyntax(Syntax.Escape, property.column) + ") references " + reference;
                logger.info("[生成外键约束]约束名:{},执行SQL:{}", foreignKeyName, foreignKeySQL);
                connection.prepareStatement(foreignKeySQL).executeUpdate();
            }
        }
    }

    @Override
    protected boolean isIndexExists(String tableName,String indexName) throws SQLException {
        String indexExistsSQL = "show index from "+syntaxHandler.getSyntax(Syntax.Escape,tableName)+" where key_name = '"+indexName+"'";
        logger.debug("[查看索引]表名:{},执行SQL:{}",tableName,indexExistsSQL);
        ResultSet resultSet = connection.prepareStatement(indexExistsSQL).executeQuery();
        boolean result = false;
        if (resultSet.next()) {
            result = true;
        }
        resultSet.close();
        return result;
    }

    @Override
    protected boolean isConstraintExists(String tableName, String constraintName) throws SQLException {
        ResultSet resultSet = connection.prepareStatement("select count(1) from information_schema.KEY_COLUMN_USAGE where constraint_name='" + constraintName + "'").executeQuery();
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
            String dropIndexSQL = "drop index "+syntaxHandler.getSyntax(Syntax.Escape,indexName)+" on "+syntaxHandler.getSyntax(Syntax.Escape,tableName)+";";
            logger.debug("[删除索引]表:{},执行SQL:{}", tableName, dropIndexSQL);
            connection.prepareStatement(dropIndexSQL).executeUpdate();
        }
    }
}
