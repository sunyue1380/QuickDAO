package cn.schoolwow.quickdao.dao;

import cn.schoolwow.quickdao.condition.Condition;
import cn.schoolwow.quickdao.condition.MySQLCondition;
import cn.schoolwow.quickdao.domain.Entity;
import cn.schoolwow.quickdao.domain.Property;
import cn.schoolwow.quickdao.helper.SQLHelper;
import cn.schoolwow.quickdao.syntax.MySQLSyntaxHandler;
import cn.schoolwow.quickdao.syntax.Syntax;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class MySQLDAO extends AbstractDAO {

    public MySQLDAO(DataSource dataSource) {
        super(dataSource);
        fieldMapping.put("long", "INTEGER");
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
            PreparedStatement propertyPs = connection.prepareStatement("show columns from `" + tableRs.getString(1) + "`");
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
        StringBuilder createTableBuilder = new StringBuilder("create table if not exists `" + entity.tableName + "`(");
        Property[] properties = entity.properties;
        for (Property property : properties) {
            createTableBuilder.append("`" + property.column + "` " + property.columnType);
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
                createTableBuilder.append(" " + syntaxHandler.getSyntax(Syntax.Comment, property.comment));
            }
            createTableBuilder.append(",");
        }
        createTableBuilder.deleteCharAt(createTableBuilder.length() - 1);
        createTableBuilder.append(")");
        if (null != entity.comment) {
            createTableBuilder.append("comment='" + entity.comment + "';");
        }
        String sql = createTableBuilder.toString().replaceAll("\\s+", " ");
        logger.debug("[生成新表]类名:{},表名:{},执行SQL:{},", entity.className, entity.tableName, sql);
        connection.prepareStatement(sql).executeUpdate();
        createUniqueKey(entity);
    }

    @Override
    protected void compareEntityDatabase(Entity entity, Entity dbEntity) throws SQLException {
        Property[] entityProperties = entity.properties;
        Property[] dbEntityProperties = dbEntity.properties;
        for (Property entityProperty : entityProperties) {
            boolean columnExist = false;
            for (Property dbEntityProperty : dbEntityProperties) {
                if (dbEntityProperty.column.equals(entityProperty.column)) {
                    columnExist = true;
                    break;
                }
            }
            if (!columnExist) {
                StringBuilder addColumnBuilder = new StringBuilder();
                addColumnBuilder.append("alter table `" + entity.tableName + "` add column " + "`" + entityProperty.column + "` " + entityProperty.columnType + " ");
                if (null != entityProperty.defaultValue) {
                    addColumnBuilder.append(" default " + entityProperty.defaultValue);
                }
                if (null != entityProperty.comment) {
                    addColumnBuilder.append(" " + syntaxHandler.getSyntax(Syntax.Comment, entityProperty.comment));
                }
                if (null != entityProperty.foreignKey) {
                    addColumnBuilder.append(",constraint `" + entityProperty.foreignKeyName + "` foreign key(`" + entityProperty.column + "`) references " + entityProperty.foreignKey);
                }
                addColumnBuilder.append(";");
                String sql = addColumnBuilder.toString().replaceAll("\\s+", " ");
                logger.debug("[添加新列]表:{},列名:{},执行SQL:{}", entity.tableName, entityProperty.column + "(" + entityProperty.columnType + ")", sql);
                connection.prepareStatement(sql).executeUpdate();
                if (entityProperty.unique) {
                    createUniqueKey(entity);
                }
            }
        }
    }

    @Override
    protected void createUniqueKey(Entity entity) throws SQLException {
        if (null == entity.uniqueKeyProperties || entity.uniqueKeyProperties.length == 0) {
            return;
        }
        String uniqueKeyIndexName = entity.tableName + "_unique_index";
        if (isConstraintExist(uniqueKeyIndexName)) {
            return;
        }
        StringBuilder uniqueKeyBuilder = new StringBuilder("alter table `" + entity.tableName + "` add unique index `" + uniqueKeyIndexName + "` (");
        for (Property property : entity.uniqueKeyProperties) {
            uniqueKeyBuilder.append("`" + property.column + "`,");
        }
        uniqueKeyBuilder.deleteCharAt(uniqueKeyBuilder.length() - 1);
        uniqueKeyBuilder.append(");");
        String uniqueKeySQL = uniqueKeyBuilder.toString().replaceAll("\\s+", " ");
        logger.debug("[添加唯一性约束]表:{},执行SQL:{}", entity.tableName, uniqueKeySQL);
        connection.prepareStatement(uniqueKeySQL).executeUpdate();
    }

    @Override
    protected void createForeignKey(Collection<Entity> entityList) throws SQLException {
        for (Entity entity : entityList) {
            Property[] foreignKeyProperties = entity.foreignKeyProperties;
            for (Property property : foreignKeyProperties) {
                if (isConstraintExist(property.foreignKeyName)) {
                    continue;
                }
                String foreignKeySQL = "alter table " + syntaxHandler.getSyntax(Syntax.Escape, entity.tableName) + " add constraint " + syntaxHandler.getSyntax(Syntax.Escape, property.foreignKeyName) + " foreign key(" + syntaxHandler.getSyntax(Syntax.Escape, property.column) + ") references " + property.foreignKey;
                logger.info("[生成外键约束]约束名:{},执行SQL:{}", property.foreignKeyName, foreignKeySQL);
                connection.prepareStatement(foreignKeySQL).executeUpdate();
            }
        }
    }

    @Override
    protected boolean isConstraintExist(String constraintName) throws SQLException {
        ResultSet resultSet = connection.prepareStatement("select count(1) from information_schema.KEY_COLUMN_USAGE where constraint_name='" + constraintName + "'").executeQuery();
        boolean result = false;
        if (resultSet.next()) {
            result = resultSet.getInt(1) > 0;
        }
        resultSet.close();
        return result;
    }
}
