package cn.schoolwow.quickdao.util;

import cn.schoolwow.quickdao.annotation.*;
import cn.schoolwow.quickdao.condition.AbstractCondition;
import cn.schoolwow.quickdao.condition.SubCondition;
import cn.schoolwow.quickdao.domain.QuickDAOConfig;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializeConfig;
import com.alibaba.fastjson.support.config.FastJsonConfig;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class ReflectionUtil {
    /**记录sql语句*/
    private static JSONObject sqlCache = new JSONObject();
    /**记录类对应字段数组*/
    private static Map<String,Field[]> classFieldsCache = new HashMap<>();
    /**记录类的复杂字段*/
    private static Map<String,Field[]> classCompositFieldsCache = new HashMap<>();
    /**记录复杂对象对应字段*/
    private static Map<String,Field> compositFieldCache = new HashMap<>();
    /**记录类对应主键字段*/
    public static Map<String,Field> idCache = new HashMap<>();
    /**记录类的唯一性约束字段*/
    public static Map<String,Field[]> uniqueFieldsCache = new HashMap<>();

    /**获取id属性*/
    public static boolean isIdField(Field field) {
        return idCache.get(field.getDeclaringClass().getName()).getName().equals(field.getName());
    }
    /**获取id属性*/
    public static Field getId(Class _class) {
        return idCache.get(_class.getName());
    }
    /**获取id属性*/
    public static void setId(Object instance,long value) throws IllegalAccessException {
        Field id = getId(instance.getClass());
        id.setLong(instance,value);
    }
    /**
     * 获取类属性
     * */
    public static Field[] getFields(Class _class){
        if(!classFieldsCache.containsKey(_class.getName())){
            Field[] fields = _class.getDeclaredFields();
            Field.setAccessible(fields,true);
            List<Field> fieldList = new ArrayList<>(fields.length);
            Set<String> packageNameSet = QuickDAOConfig.packageNameMap.keySet();
            for(Field field:fields){
                //排除复杂对象字段
                boolean contains = false;
                for(String packageName:packageNameSet){
                    if(field.getType().getName().contains(packageName)){
                        contains = true;
                        break;
                    }
                }
                if(contains){
                    compositFieldCache.put(_class.getName()+"_"+field.getType().getName()+"_"+field.getName(),field);
                }else if(field.getDeclaredAnnotation(Ignore.class)==null){
                    fieldList.add(field);
                }
            }
            fields = fieldList.toArray(new Field[fieldList.size()]);
            classFieldsCache.put(_class.getName(),fields);
        }
        return classFieldsCache.get(_class.getName());
    }

    public static Field[] getCompositField(Class _class,Class fieldType){
        String target = _class.getName()+"_"+fieldType.getName();
        if(classCompositFieldsCache.containsKey(target)) {
            return classCompositFieldsCache.get(target);
        }
        List<Field> fieldList = new ArrayList<>();
        Set<String> keySet = compositFieldCache.keySet();
        for(String key:keySet){
            if(key.contains(target)){
                fieldList.add(compositFieldCache.get(key));
            }
        }
        if(fieldList.size()==0){
            return null;
        }else{
            Field[] fields = fieldList.toArray(new Field[fieldList.size()]);
            classCompositFieldsCache.put(target,fields);
            return fields;
        }
    }

    /**该类是否有唯一性约束*/
    public static boolean hasUniqueKey(Class _class){
        String key = "hasUniqueKey_"+_class.getName();
        if(!sqlCache.containsKey(key)){
            Field[] fields = getUniqueFields(_class);
            return fields!=null&&fields.length>0;
        }
        return sqlCache.getBoolean(key);
    }

    /**获取该类的唯一性约束字段*/
    public static Field[] getUniqueFields(Class _class){
        return uniqueFieldsCache.get(_class.getName());
    }

    /**对象是否存在id*/
    public static boolean hasId(Object instance) throws IllegalAccessException {
        Class _class = instance.getClass();
        Field id = getId(_class);
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
            if(ReflectionUtil.isIdField(fields[i])){
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
            if(ReflectionUtil.isIdField(fields[i])){
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
            if(fields[i].getAnnotation(Unique.class)!=null){
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
    public static <T> List<T> mappingSingleResultToList(ResultSet resultSet,int count,Class<T> _class) throws SQLException, IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
        JSONArray array = new JSONArray(count);
        while(resultSet.next()){
            array.add(resultSet.getString(1));
        }
        resultSet.close();
        return array.toJavaList(_class);
    }

    /**
     * 映射结果集到JSONArray中
     * */
    public static JSONArray mappingResultSetToJSONArray(ResultSet resultSet,String tableNameAlias,int count) throws SQLException {
        ResultSetMetaData metaData = resultSet.getMetaData();
        int columnCount = metaData.getColumnCount();
        String[] columnNames = new String[columnCount];
        for(int i=1;i<=columnNames.length;i++){
            String label = metaData.getColumnLabel(i);
            columnNames[i-1] = StringUtil.Underline2Camel(label.substring(label.indexOf("_")+1));
        }
        JSONArray array = new JSONArray(count);
        while(resultSet.next()){
            JSONObject o = new JSONObject();
            for(int i=1;i<=columnCount;i++){
                o.put(columnNames[i-1],resultSet.getString(i));
            }
            array.add(o);
        }
        resultSet.close();
        return array;
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
                ps.setObject(parameterIndex, field.get(instance));
                return "'"+field.get(instance)+"'";
            }
        }
    }
}
