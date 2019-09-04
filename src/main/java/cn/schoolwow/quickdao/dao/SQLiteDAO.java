package cn.schoolwow.quickdao.dao;

import cn.schoolwow.quickdao.condition.Condition;
import cn.schoolwow.quickdao.condition.SQLiteCondition;
import cn.schoolwow.quickdao.domain.Entity;
import cn.schoolwow.quickdao.domain.Property;
import cn.schoolwow.quickdao.helper.SQLHelper;
import cn.schoolwow.quickdao.syntax.SQLiteSyntaxHandler;
import cn.schoolwow.quickdao.syntax.Syntax;
import cn.schoolwow.quickdao.util.QuickDAOConfig;
import cn.schoolwow.quickdao.util.ReflectionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class SQLiteDAO extends MySQLDAO {
    Logger logger = LoggerFactory.getLogger(SQLiteDAO.class);

    public SQLiteDAO(DataSource dataSource) {
        super(dataSource);
        fieldMapping.put("long", "INTEGER");
        syntaxHandler = new SQLiteSyntaxHandler();
        sqlHelper = new SQLHelper(syntaxHandler);
    }

    @Override
    public <T> Condition<T> query(Class<T> _class) {
        return new SQLiteCondition(_class, dataSource, this, syntaxHandler, sqlHelper);
    }

    @Override
    protected Entity[] getDatabaseInfo() throws SQLException {
        Connection connection = dataSource.getConnection();
        connection.setAutoCommit(false);
        PreparedStatement tablePs = connection.prepareStatement("select name from sqlite_master where type='table';");
        ResultSet tableRs = tablePs.executeQuery();
        List<Entity> entityList = new ArrayList<>();
        while (tableRs.next()) {
            Entity entity = new Entity();
            entity.tableName = tableRs.getString(1);

            List<Property> propertyList = new ArrayList<>();
            PreparedStatement propertyPs = connection.prepareStatement("PRAGMA table_info(`" + tableRs.getString(1) + "`)");
            ResultSet propertiesRs = propertyPs.executeQuery();
            while (propertiesRs.next()) {
                Property property = new Property();
                property.column = propertiesRs.getString("name");
                property.columnType = propertiesRs.getString("type");
                property.notNull = "1".equals(propertiesRs.getString("notnull"));
                if (null != propertiesRs.getString("dflt_value")) {
                    property.defaultValue = propertiesRs.getString("dflt_value");
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

    /**
     * 创建新表
     */
    @Override
    protected void createTable(Entity entity) throws SQLException {
        StringBuilder createTableBuilder = new StringBuilder("create table " + syntaxHandler.getSyntax(Syntax.Escape,entity.tableName) + "(");
        Property[] properties = entity.properties;
        for (Property property : properties) {
            createTableBuilder.append(syntaxHandler.getSyntax(Syntax.Escape,property.column) + " " + property.columnType);
            if (property.id) {
                createTableBuilder.append(" primary key " + syntaxHandler.getSyntax(Syntax.AutoIncrement));
            } else {
                if (null != property.defaultValue) {
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
        if (QuickDAOConfig.openForeignKey&&null!=entity.foreignKeyProperties) {
            Property[] foreignKeyProperties = entity.foreignKeyProperties;
            for (Property property : foreignKeyProperties) {
                createTableBuilder.append("foreign key(" + syntaxHandler.getSyntax(Syntax.Escape,property.column) + ") references ");
                String operation = property.foreignKey.foreignKeyOption().getOperation();
                createTableBuilder.append(syntaxHandler.getSyntax(Syntax.Escape,ReflectionUtil.entityMap.get(property.foreignKey.table().getName()).tableName) + "(" + syntaxHandler.getSyntax(Syntax.Escape,property.foreignKey.field()) + ") ON DELETE " + operation+ " ON UPDATE " + operation);
                createTableBuilder.append(",");
            }
            //手动开启外键约束
            connection.prepareStatement("PRAGMA foreign_keys = ON;").executeUpdate();
        }
        createTableBuilder.deleteCharAt(createTableBuilder.length() - 1);
        if (null != entity.comment) {
            createTableBuilder.append(syntaxHandler.getSyntax(Syntax.Comment,entity.comment));
        }
        createTableBuilder.append(")");
        String sql = createTableBuilder.toString().replaceAll("\\s+", " ");
        logger.debug("[生成新表]类名:{},表名:{},执行sql:{}", entity.className, entity.tableName, sql);
        connection.prepareStatement(sql).executeUpdate();
    }

    @Override
    protected void createForeignKey() throws SQLException {

    }

    @Override
    protected boolean isIndexExists(String tableName,String indexName) throws SQLException {
        String indexExistsSQL = "select count(1) from sqlite_master where type = 'index' and name = '"+indexName+"'";
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
}
