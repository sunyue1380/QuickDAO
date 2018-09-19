package com.schoolwow.quickdao.dao;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.schoolwow.quickdao.util.StatementUtil;
import com.schoolwow.quickdao.util.StringUtil;

import javax.sql.DataSource;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class SQLiteDAO extends AbstractDAO{
    protected String insertIgnoreSQL = "insert or ignore into ";

    public SQLiteDAO(DataSource dataSource) {
        super(dataSource);
        fieldMapping.put("long","INTEGER");
    }

    protected void setLastInsertId(Connection connection,Object instance,Field id) throws IllegalAccessException, SQLException {
        String tableName = StringUtil.Camel2Underline(instance.getClass().getSimpleName());
        ResultSet resultSet = connection.prepareStatement("select last_insert_rowid() from "+tableName).executeQuery();
        if(resultSet.next()){
            id.setLong(instance,resultSet.getLong(1));
        }
        resultSet.close();
    }

    protected String getInsertIgnoreSQL(){
        return this.insertIgnoreSQL;
    };

    @Override
    public void updateDatabase(JSONArray entityList,JSONArray dbEntityList) throws SQLException {
        Connection connection = dataSource.getConnection();
        connection.setAutoCommit(false);
        //判断表是否存在,存在则判断字段,不存在则创建
        for(int i=0;i<entityList.size();i++){
            JSONObject source = entityList.getJSONObject(i);
            String tableName = source.getString("tableName");
            JSONObject target = getValue(dbEntityList,"tableName",tableName);

            StringBuilder uniqueColumns = new StringBuilder();
            if(target==null&&!source.getBoolean("ignore")){
                //新增数据库表
                StringBuilder builder = new StringBuilder("create table if not exists "+tableName+"(");
                JSONArray properties = source.getJSONArray("properties");
                for(int j=0;j<properties.size();j++){
                    JSONObject property = properties.getJSONObject(j);
                    String column = property.getString("column");
                    String columnType = property.containsKey("columnType")?property.getString("columnType"):fieldMapping.get(property.getString("type"));
                    if("id".equals(column)){
                        //主键新增
                        builder.append(column+" "+columnType+" primary key autoincrement ");
                    }else{
                        builder.append("`"+column+"` "+columnType);
                        if(property.containsKey("default")){
                            builder.append(" default "+property.getString("default"));
                        }
                        if(property.getBoolean("notNull")){
                            builder.append(" not null ");
                        }

                        if(property.getBoolean("unique")){
                            //统一添加unique约束
                            uniqueColumns.append(column+",");
                        }
                    }
                    if(j!=properties.size()-1){
                        builder.append(",");
                    }
                }
                builder.append(")");
                String sql = builder.toString().replaceAll("\\s+"," ");
                logger.debug("generate table:"+tableName+",sql:"+sql+", result:"+connection.prepareStatement(sql).executeUpdate());
            }else {
                //对比字段
                JSONArray sourceProperties = source.getJSONArray("properties");
                JSONArray targetProperties = target.getJSONArray("properties");
                addNewColumn(connection, tableName, uniqueColumns, sourceProperties, targetProperties);
            }
            //统一处理unique约束
            if(uniqueColumns.length()>0){
                uniqueColumns.deleteCharAt(uniqueColumns.length()-1);
                String uniqueSQL = "create index "+tableName+"_"+uniqueColumns.toString().replace(",","_")+" on "+tableName+"("+uniqueColumns.toString()+");";
                logger.debug("unique sql:"+uniqueSQL+",result:"+connection.prepareStatement(uniqueSQL).executeUpdate());
            }
        }

        connection.commit();
        connection.setAutoCommit(true);
        connection.close();
    }

    @Override
    public JSONArray getDatabaseInfo() throws SQLException {
        Connection connection = dataSource.getConnection();
        PreparedStatement tablePs = connection.prepareStatement("select name from sqlite_master where type='table';");
        ResultSet tableRs = tablePs.executeQuery();
        //(1)获取所有表
        JSONArray entityList = new JSONArray();
        while (tableRs.next()) {
            JSONObject entity = new JSONObject();
            entity.put("tableName",tableRs.getString(1));

            JSONArray properties = new JSONArray();
            PreparedStatement propertyPs = connection.prepareStatement("PRAGMA table_info(" + tableRs.getString(1) + ")");
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
