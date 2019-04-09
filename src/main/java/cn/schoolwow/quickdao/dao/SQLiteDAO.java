package cn.schoolwow.quickdao.dao;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class SQLiteDAO extends AbstractDAO{
    public SQLiteDAO(DataSource dataSource) {
        super(dataSource);
        fieldMapping.put("long","INTEGER");
    }

    protected String getSyntax(Syntax syntax){
        switch(syntax){
            case AutoIncrement:{
                return "autoincrement";
            }
            case InsertIgnore:{
                return "insert or ignore into ";
            }
            default:return null;
        }
    }

    @Override
    protected String getUniqueStatement(String tableName, List<String> columns) {
        StringBuilder uniqueSQLBuilder = new StringBuilder("create index `"+tableName+"_");
        columns.stream().forEach((column)->{
            uniqueSQLBuilder.append(column+"_");
        });
        uniqueSQLBuilder.deleteCharAt(uniqueSQLBuilder.length()-1);
        uniqueSQLBuilder.append("` on `"+tableName+"`(");
        columns.stream().forEach((column)->{
            uniqueSQLBuilder.append("`"+column+"`,");
        });
        uniqueSQLBuilder.deleteCharAt(uniqueSQLBuilder.length()-1);
        uniqueSQLBuilder.append(");");
        return uniqueSQLBuilder.toString();
    }

    @Override
    public JSONArray getDatabaseInfo(Connection connection) throws SQLException {
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
                //property.put("unique","UNI".equals(propertiesRs.getString("Key")));
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
