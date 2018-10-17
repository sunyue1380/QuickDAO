package com.schoolwow.quickdao.dao;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class MySQLDAO extends AbstractDAO{
    public MySQLDAO(DataSource dataSource) {
        super(dataSource);
        fieldMapping.put("long","INTEGER");
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
                StringBuilder builder = new StringBuilder("create table `"+tableName+"`(");
                JSONArray properties = source.getJSONArray("properties");
                for(int j=0;j<properties.size();j++){
                    JSONObject property = properties.getJSONObject(j);
                    String column = property.getString("column");
                    String columnType = property.containsKey("columnType")?property.getString("columnType"):fieldMapping.get(property.getString("type"));
                    if("id".equals(column)){
                        //主键新增
                        builder.append(column+" "+columnType+" primary key auto_increment ");
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
                builder.append(")engine= InnoDB,character set utf8");
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
                String uniqueSQL = "alter table "+tableName+" add unique index "+tableName+"_"+uniqueColumns.toString().replace(",","_")+"_unique_index "+"("+uniqueColumns.toString()+");";
                logger.debug("unique sql:"+uniqueSQL+",result:"+connection.prepareStatement(uniqueSQL).executeUpdate());
            }
        }
        connection.commit();
        connection.setAutoCommit(true);
        connection.close();
    }
}
