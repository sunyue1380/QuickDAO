package cn.schoolwow.quickdao.dao;

import cn.schoolwow.quickdao.domain.Entity;
import cn.schoolwow.quickdao.domain.Property;
import cn.schoolwow.quickdao.util.QuickDAOConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class SQLiteDAO extends AbstractDAO{
    Logger logger = LoggerFactory.getLogger(SQLiteDAO.class);
    public SQLiteDAO(DataSource dataSource) {
        super(dataSource);
        fieldMapping.put("long","INTEGER");
    }

    protected String getSyntax(Syntax syntax,Object... values){
        switch(syntax){
            case AutoIncrement:{
                return "autoincrement";
            }
            case InsertIgnore:{
                return "insert or ignore into ";
            }
            case Comment:{
                return "/*"+values[0]+"*/";
            }
            default:return "";
        }
    }

    /**创建新表*/
    protected void createTable(Entity entity,Connection connection) throws SQLException {
        StringBuilder createTableBuilder = new StringBuilder("create table `" + entity.tableName + "`(");
        if(null!=entity.comment){
            createTableBuilder.append("/*"+entity.comment+"*/");
        }
        Property[] properties = entity.properties;
        for(Property property:properties){
            createTableBuilder.append("`" + property.column + "` " + property.columnType);
            if(property.id){
                createTableBuilder.append(" primary key " + getSyntax(Syntax.AutoIncrement));
            }else{
                if(null!=property.defaultValue){
                    createTableBuilder.append(" default '" + property.defaultValue+"'");
                }
                if(property.notNull){
                    createTableBuilder.append(" not null ");
                }
            }
            if(null!=property.comment){
                createTableBuilder.append(" " + getSyntax(Syntax.Comment, property.comment));
            }
            createTableBuilder.append(",");
        }
        if(QuickDAOConfig.openForeignKey){
            Property[] foreignKeyProperties = entity.foreignKeyProperties;
            for(Property property:foreignKeyProperties){
                createTableBuilder.append("foreign key(`"+property.column+"`) references "+property.foreignKey+",");
            }
            //手动开启外键约束
            connection.prepareStatement("PRAGMA foreign_keys = ON;").executeUpdate();
        }
        createTableBuilder.deleteCharAt(createTableBuilder.length() - 1);
        createTableBuilder.append(")");
        String sql = createTableBuilder.toString().replaceAll("\\s+", " ");
        logger.debug("[生成新表]类名:{},表名:{},执行sql:{}", entity.className, entity.tableName, sql);
        connection.prepareStatement(sql).executeUpdate();
        createUniqueKey(entity,connection);
    }

    /**创建唯一索引*/
    protected void createUniqueKey(Entity entity,Connection connection) throws SQLException {
        Property[] uniqueKeyProperties = entity.uniqueKeyProperties;
        if(null==uniqueKeyProperties||uniqueKeyProperties.length==0){
            return;
        }
        StringBuilder uniqueKeyBuilder = new StringBuilder("create unique index `"+entity.tableName+"_unique_index` on `"+entity.tableName+"` (");
        for(Property property:uniqueKeyProperties){
            uniqueKeyBuilder.append("`"+property.column+"`,");
        }
        uniqueKeyBuilder.deleteCharAt(uniqueKeyBuilder.length()-1);
        uniqueKeyBuilder.append(");");
        String uniqueKeySQL = uniqueKeyBuilder.toString().replaceAll("\\s+", " ");
        logger.debug("[添加唯一性约束]表:{},执行SQL:{}",entity.tableName,uniqueKeySQL);
        connection.prepareStatement(uniqueKeySQL).executeUpdate();
    }

    protected void createForeignKey(Collection entityList, Connection connection) throws SQLException {

    }

    @Override
    public Entity[] getDatabaseInfo() throws SQLException {
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
                if(null!=propertiesRs.getString("dflt_value")){
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
}
