package cn.schoolwow.quickdao.util;

import cn.schoolwow.quickdao.annotation.Ignore;
import cn.schoolwow.quickdao.annotation.Unique;
import com.alibaba.fastjson.JSONObject;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class ReflectionUtil {
    private static JSONObject sqlCache = new JSONObject();
    /**获取id属性*/
    public static Field getId(Class _class) throws NoSuchFieldException {
        Field id = _class.getDeclaredField("id");
        id.setAccessible(true);
        return id;
    }
    /**获取id属性*/
    public static void setId(Object instance,long value) throws NoSuchFieldException, IllegalAccessException {
        Field id = getId(instance.getClass());
        id.setLong(instance,value);
    }
    /**获取类属性*/
    public static Field[] getFields(Class _class){
        Field[] fields = _class.getDeclaredFields();
        Field.setAccessible(fields,true);
        return fields;
    }
    /**该类是否有唯一性约束*/
    public static boolean hasUniqueKey(Class _class){
        String key = "hasUniqueKey_"+_class.getName();
        if(!sqlCache.containsKey(key)){
            List<Field> fieldList = Arrays.asList(ReflectionUtil.getFields(_class));
            sqlCache.put(key,fieldList.stream().anyMatch((field)->field.getAnnotation(Unique.class)!=null));
        }
        return sqlCache.getBoolean(key);
    }
    /**对象是否存在id*/
    public static boolean hasId(Object instance) throws NoSuchFieldException, IllegalAccessException {
        Class _class = instance.getClass();
        Field id = _class.getDeclaredField("id");
        id.setAccessible(true);
        switch (id.getType().getSimpleName().toLowerCase()) {
            case "int": {
                return id.getInt(instance) <= 0 ? false : true;
            }
            case "integer": {
                return (id.get(instance) == null || id.getInt(instance) <= 0) ? false : true;
            }
            case "long": {
                return ((!id.getType().isPrimitive() && id.get(instance) == null) || id.getLong(instance) <= 0) ? false : true;
            }
            case "string": {
                return id.get(instance) == null ? false : true;
            }
            default: {
                throw new IllegalArgumentException("无法识别的主键类型:" + id.getType().getSimpleName().toLowerCase());
            }
        }
    }

    /**
     * 直接插入
     * 为prepareStatement赋值
     * 返回实际执行的SQL语句
     * */
    public static String setValueWithInsertIgnore(PreparedStatement ps,Object instance,String sql) throws SQLException, IllegalAccessException {
        int parameterIndex = 1;
        Field[] fields = getFields(instance.getClass());
        for(int i=0;i<fields.length;i++){
            if(fields[i].getName().equals("id")||fields[i].getAnnotation(Ignore.class)!=null){
                continue;
            }
            sql = sql.replaceFirst("\\?",setParameter(instance, ps, parameterIndex, fields[i]));
            parameterIndex++;
        }
        return sql;
    }

    /**根据id更新
     * 为prepareStatement赋值
     * */
    public static String setValueWithUpdateById(PreparedStatement ps,Object instance,String sql) throws SQLException, IllegalAccessException, NoSuchFieldException {
        int parameterIndex = 1;
        Field[] fields = getFields(instance.getClass());
        //先设置非id属性
        for(int i=0;i<fields.length;i++){
            if(fields[i].getName().equals("id")||fields[i].getAnnotation(Ignore.class)!=null){
                continue;
            }
            sql.replaceFirst("\\?",setParameter(instance,ps,parameterIndex,fields[i]));
            parameterIndex++;
        }
        //再设置id属性
        sql.replaceFirst("\\?",setParameter(instance,ps,parameterIndex,getId(instance.getClass())));
        return sql;
    }

    /**
     * 根据UniqueKey更新
     * 为prepareStatement赋值*/
    public static String setValueWithUpdateByUniqueKey(PreparedStatement ps,Object instance,String sql) throws SQLException, IllegalAccessException {
        int parameterIndex = 1;
        Field[] fields = getFields(instance.getClass());
        //先设置非Unique字段
        for(int i=0;i<fields.length;i++){
            if(fields[i].getName().equals("id")||fields[i].getAnnotation(Ignore.class)!=null||fields[i].getAnnotation(Unique.class)!=null){
                continue;
            }
            sql = sql.replaceFirst("\\?",setParameter(instance, ps, parameterIndex, fields[i]));
            parameterIndex++;
        }
        //再设置Unique字段查询条件
        for(int i=0;i<fields.length;i++){
            if(fields[i].getAnnotation(Unique.class)!=null){
                sql = sql.replaceFirst("\\?",setParameter(instance, ps, parameterIndex, fields[i]));
                parameterIndex++;
            }
        }
        return sql;
    }

    /**
     * 将ResultSet映射到List中
     * @return 结果集的映射
     * */
    public static <T> void mappingResultToList(ResultSet resultSet,List<T> instanceList,Class<T> _class,String column) throws SQLException, IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
        while(resultSet.next()){
            T instance = _class.getConstructor(String.class).newInstance(resultSet.getString(column));
            instanceList.add(instance);
        }
        resultSet.close();
    }

    /**
     * 将ResultSet映射到List中
     * @return 结果集的映射
     * */
    public static <T> List<T> mappingResultToList(ResultSet resultSet,List<T> instanceList,Class<T> _class) throws SQLException, IllegalAccessException, InstantiationException {
        while(resultSet.next()){
            T instance = _class.newInstance();
            Field[] fields = getFields(_class);
            for(Field field:fields){
                if(field.getAnnotation(Ignore.class)!=null){
                    continue;
                }
                String columnName = "t_"+StringUtil.Camel2Underline(field.getName());
                String type = field.getType().getSimpleName().toLowerCase();
                //根据类型进行映射
                switch(type){
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
            instanceList.add(instance);
        }
        resultSet.close();
        return instanceList;
    }

    /**
     * 设置参数
     * 返回设置的参数值
     * */
    private static String setParameter(Object instance, PreparedStatement ps, int parameterIndex, Field field) throws SQLException, IllegalAccessException {
        switch (field.getType().getSimpleName().toLowerCase()) {
            case "int": {
                ps.setInt(parameterIndex, field.getInt(instance));
                return ""+field.getInt(instance);
            }
            case "integer": {
                ps.setObject(parameterIndex, field.get(instance));
                return ""+field.get(instance);
            }
            case "long": {
                if(field.getType().isPrimitive()){
                    ps.setLong(parameterIndex, field.getLong(instance));
                    return ""+field.getLong(instance);
                }else{
                    ps.setObject(parameterIndex, field.get(instance));
                    return ""+field.get(instance);
                }

            }
            case "boolean": {
                if(field.getType().isPrimitive()){
                    ps.setBoolean(parameterIndex, field.getBoolean(instance));
                    return ""+field.getBoolean(instance);
                }else{
                    ps.setObject(parameterIndex, field.get(instance));
                    return ""+field.get(instance);
                }
            }
            case "string": {
                ps.setString(parameterIndex, field.get(instance)==null?null:field.get(instance).toString());
                return "'"+(field.get(instance)==null?"":field.get(instance).toString())+"'";
            }
            default: {
                //Date类型使用默认策略
                ps.setObject(parameterIndex, field.get(instance));
                return "'"+field.get(instance)+"'";
            }
        }
    }
}
