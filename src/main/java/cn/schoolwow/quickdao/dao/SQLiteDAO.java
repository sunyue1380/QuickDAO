package cn.schoolwow.quickdao.dao;

import cn.schoolwow.quickdao.QuickDAO;
import cn.schoolwow.quickdao.domain.QuickDAOConfig;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

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
    protected void createTable(JSONObject entity,Connection connection) throws SQLException {
        String tableName = entity.getString("tableName");
        StringBuilder createTableBuilder = new StringBuilder("create table `" + tableName + "`(");
        JSONArray properties = entity.getJSONArray("properties");
        for (int j = 0; j < properties.size(); j++) {
            JSONObject property = properties.getJSONObject(j);
            if (property.getBoolean("ignore")) {
                continue;
            }
            createTableBuilder.append("`" + property.getString("column") + "` " + property.getString("columnType"));
            if (property.getBoolean("id")) {
                //主键新增
                createTableBuilder.append(" primary key " + getSyntax(Syntax.AutoIncrement));
            } else {
                if (property.containsKey("default")) {
                    createTableBuilder.append(" default " + property.getString("default"));
                }
                if (property.getBoolean("notNull")) {
                    createTableBuilder.append(" not null ");
                }
            }
            createTableBuilder.append(" " + getSyntax(Syntax.Comment, property.getString("comment")));
            createTableBuilder.append(",");
        }
        if(QuickDAOConfig.openForeignKey){
            JSONArray foreignKeyProperties = entity.getJSONArray("foreignKeyProperties");
            for(int j=0;j<foreignKeyProperties.size();j++){
                JSONObject property = foreignKeyProperties.getJSONObject(j);
                createTableBuilder.append("foreign key(`"+property.getString("column")+"`) references "+property.getString("foreignKey")+",");
            }
            //手动开启外键约束
            connection.prepareStatement("PRAGMA foreign_keys = ON;").executeUpdate();
        }
        createTableBuilder.deleteCharAt(createTableBuilder.length() - 1);
        createTableBuilder.append(")");
        String sql = createTableBuilder.toString().replaceAll("\\s+", " ");
        logger.debug("[生成新表{}=>{}]执行sql:{}", entity.getString("className"), tableName, sql);
        connection.prepareStatement(sql).executeUpdate();
        createUniqueKey(entity,connection);
    }

    /**创建唯一索引*/
    protected void createUniqueKey(JSONObject entity,Connection connection) throws SQLException {
        String tableName = entity.getString("tableName");
        JSONArray uniqueKeyProperties = entity.getJSONArray("uniqueKeyProperties");
        if(uniqueKeyProperties.size()==0){
            return;
        }
        StringBuilder uniqueKeyBuilder = new StringBuilder("create unique index `"+tableName+"_unique_index` on `"+tableName+"` (");
        for(int i=0;i<uniqueKeyProperties.size();i++){
            uniqueKeyBuilder.append("`"+uniqueKeyProperties.getString(i)+"`,");
        }
        uniqueKeyBuilder.deleteCharAt(uniqueKeyBuilder.length()-1);
        uniqueKeyBuilder.append(");");
        String uniqueKeySQL = uniqueKeyBuilder.toString().replaceAll("\\s+", " ");
        logger.debug("[添加唯一性约束]表:{},执行SQL:{}",tableName,uniqueKeySQL);
        connection.prepareStatement(uniqueKeySQL).executeUpdate();
    }

    protected void createForeignKey(JSONArray entityList,Connection connection) throws SQLException {

    }

    @Override
    public JSONArray getDatabaseInfo() throws SQLException {
        Connection connection = dataSource.getConnection();
        connection.setAutoCommit(false);
        PreparedStatement tablePs = connection.prepareStatement("select name from sqlite_master where type='table';");
        ResultSet tableRs = tablePs.executeQuery();
        //(1)获取所有表
        JSONArray entityList = new JSONArray();
        while (tableRs.next()) {
            JSONObject entity = new JSONObject();
            entity.put("tableName",tableRs.getString(1));

            JSONArray properties = new JSONArray();
            PreparedStatement propertyPs = connection.prepareStatement("PRAGMA table_info(`" + tableRs.getString(1) + "`)");
            ResultSet propertiesRs = propertyPs.executeQuery();
            while (propertiesRs.next()) {
                JSONObject property = new JSONObject();
                property.put("column", propertiesRs.getString("name"));
                property.put("columnType", propertiesRs.getString("type"));
                property.put("notNull","1".equals(propertiesRs.getString("notnull")));
                if (null != propertiesRs.getString("dflt_value")) {
                    property.put("default", propertiesRs.getString("dflt_value"));
                }
                properties.add(property);
            }
            entity.put("properties",properties);
            entityList.add(entity);

            propertiesRs.close();
            propertyPs.close();
        }
        tableRs.close();
        tablePs.close();
        connection.close();
        return entityList;
    }
}
