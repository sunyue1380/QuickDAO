package com.schoolwow.quickdao.util;

import com.schoolwow.quickdao.annotation.Ignore;
import com.schoolwow.quickdao.annotation.Unique;

import java.lang.reflect.Field;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

public class StatementUtil {
    public static void getSingleField(ResultSet resultSet, Object instance, Field field, String columnName) throws IllegalAccessException, SQLException {
        //根据类型进行映射
        switch(field.getType().getSimpleName().toLowerCase()){
            case "int":{field.setInt(instance, resultSet.getInt(columnName));}break;
            case "integer":{field.setInt(instance, Integer.valueOf(resultSet.getInt(columnName)));}break;
            case "long":{
                if(field.getType().isPrimitive()){
                    field.setLong(instance,resultSet.getLong(columnName));
                }else{
                    field.setLong(instance, Long.valueOf(resultSet.getLong(columnName)));
                }
            };break;
            case "boolean":{
                if(field.getType().isPrimitive()){
                    field.setBoolean(instance,resultSet.getBoolean(columnName));
                }else{
                    field.setBoolean(instance, Boolean.valueOf(resultSet.getBoolean(columnName)));
                }
            };break;
            case "date":{
                Object o = resultSet.getObject(columnName);
                if(o instanceof Long){
                    Date date = new Date(((Long)o).longValue());
                    field.set(instance,date);
                }else if(o instanceof Date){
                    field.set(instance,o);
                }
            };break;
            default:{
                field.set(instance,resultSet.getObject(columnName));
            }
        }
    }

    public static void addUpdateByIdBatch(Object instance, Field[] fields,Field id,PreparedStatement ps) throws SQLException, IllegalAccessException {
        int parameterIndex = 1;
        for(int i=0;i<fields.length;i++){
            if(fields[i].getName().equals("id")||fields[i].getAnnotation(Ignore.class)!=null){
                continue;
            }
            setParameter(instance,ps,parameterIndex,fields[i]);
            parameterIndex++;
        }
        switch(id.getType().getSimpleName().toLowerCase()){
            case "int":{
                ps.setInt(parameterIndex,id.getInt(instance));
            }break;
            case "integer":{
                ps.setInt(parameterIndex,id.getInt(instance));
            }break;
            case "long":{
                ps.setLong(parameterIndex,id.getLong(instance));
            }break;
            case "string":{
                ps.setString(parameterIndex,id.get(instance).toString());
            }break;
            default:{
                throw new IllegalArgumentException("无法识别的主键类型:"+id.getType().getSimpleName().toLowerCase());
            }
        }
        ps.addBatch();
    }

    public static void addUpdateByUniqueKeyBatch(Object instance, Field[] fields, PreparedStatement ps) throws SQLException, IllegalAccessException {
        int parameterIndex = 1;
        for(int i=0;i<fields.length;i++){
            if(fields[i].getName().equals("id")||fields[i].getAnnotation(Ignore.class)!=null||fields[i].getAnnotation(Unique.class)!=null){
                continue;
            }
            setParameter(instance, ps, parameterIndex, fields[i]);
            parameterIndex++;
        }
        for(int i=0;i<fields.length;i++){
            if(fields[i].getAnnotation(Unique.class)!=null){
                setParameter(instance, ps, parameterIndex, fields[i]);
                parameterIndex++;
            }
        }
        ps.addBatch();
    }

    public static void addInsertIgnoreBatch(Object instance, Field[] fields, PreparedStatement ps) throws SQLException, IllegalAccessException {
        int parameterIndex = 1;
        for(int i=0;i<fields.length;i++){
            if(fields[i].getName().equals("id")||fields[i].getAnnotation(Ignore.class)!=null){
                continue;
            }
            setParameter(instance, ps, parameterIndex, fields[i]);
            parameterIndex++;
        }
        ps.addBatch();
    }

    private static void setParameter(Object instance, PreparedStatement ps, int parameterIndex, Field field) throws SQLException, IllegalAccessException {
        switch (field.getType().getSimpleName().toLowerCase()) {
            case "int": {
                ps.setInt(parameterIndex, field.getInt(instance));
            }
            break;
            case "integer": {
                ps.setObject(parameterIndex, field.get(instance));
            }
            break;
            case "long": {
                if(field.getType().isPrimitive()){
                    ps.setLong(parameterIndex, field.getLong(instance));
                }else{
                    ps.setObject(parameterIndex, field.get(instance));
                }
            }
            break;
            case "boolean": {
                if(field.getType().isPrimitive()){
                    ps.setBoolean(parameterIndex, field.getBoolean(instance));
                }else{
                    ps.setObject(parameterIndex, field.get(instance));
                }
            }
            break;
            case "string": {
                ps.setString(parameterIndex, field.get(instance)==null?null:field.get(instance).toString());
            }
            break;
            default: {
                ps.setObject(parameterIndex, field.get(instance));
            }
        }
    }
}
