package cn.schoolwow.quickdao.util;

import cn.schoolwow.quickdao.annotation.Ignore;
import cn.schoolwow.quickdao.annotation.Unique;
import com.alibaba.fastjson.JSONObject;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

public class SQLUtil {
    private static JSONObject sqlCache = new JSONObject();

    /**返回fetch语句*/
    public static String fetch(Class _class,String property) {
        String key = "fetch_" + _class.getName()+"_"+property;
        if (!sqlCache.containsKey(key)) {
            String fetchSQL = "select " + columns(_class,"t") + " from `" + StringUtil.Camel2Underline(_class.getSimpleName()) + "` as t where t.`"+property+"` = ?";
            sqlCache.put(key, fetchSQL);
        }
        return sqlCache.getString(key);
    }

    /**返回根据属性删除的语句*/
    public static String delete(Class _class,String property) {
        String key = "delete_" + _class.getName()+"_"+property;
        if (!sqlCache.containsKey(key)) {
            String fetchSQL = "delete from `" + StringUtil.Camel2Underline(_class.getSimpleName()) + "` where `"+property+"` = ?";
            sqlCache.put(key, fetchSQL);
        }
        return sqlCache.getString(key);
    }

    /**返回insertIgnore语句*/
    public static String insertIgnore(Class _class,String insertIgnoreSQL) {
        String key = "insertIgnore_" + insertIgnoreSQL+"_"+_class.getName();
        if (!sqlCache.containsKey(key)) {
            StringBuilder builder = new StringBuilder();
            builder.append(insertIgnoreSQL+" `" + StringUtil.Camel2Underline(_class.getSimpleName())+"`(");
            Field[] fields = _class.getDeclaredFields();
            Field.setAccessible(fields,true);
            for(int i=0;i<fields.length;i++){
                if(fields[i].getName().equals("id")||fields[i].getAnnotation(Ignore.class)!=null){
                    continue;
                }
                builder.append("`"+StringUtil.Camel2Underline(fields[i].getName()) + "`,");
            }
            builder.deleteCharAt(builder.length()-1);
            builder.append(") values(");
            for(int i=0;i<fields.length;i++){
                if(fields[i].getName().equals("id")||fields[i].getAnnotation(Ignore.class)!=null){
                    continue;
                }
                builder.append("?,");
            }
            builder.deleteCharAt(builder.length()-1);
            builder.append(")");
            sqlCache.put(key, builder.toString());
        }
        return sqlCache.getString(key);
    }

    /**
     * 返回根据UniqueKey更新的SQL语句
     * */
    public static String updateByUniqueKey(Class _class) {
        String key = "updateByUniqueKey_" + _class.getName();
        if (!sqlCache.containsKey(key)) {
            StringBuilder builder = new StringBuilder();
            builder.append("update " + StringUtil.Camel2Underline(_class.getSimpleName())+" set ");
            Field[] fields = _class.getDeclaredFields();
            Field.setAccessible(fields,true);
            for(int i=0;i<fields.length;i++){
                if(fields[i].getName().equals("id")||fields[i].getAnnotation(Ignore.class)!=null||fields[i].getAnnotation(Unique.class)!=null){
                    continue;
                }
                builder.append("`"+StringUtil.Camel2Underline(fields[i].getName()) + "`=?,");
            }
            builder.deleteCharAt(builder.length()-1);
            builder.append(" where ");
            for(int i=0;i<fields.length;i++){
                if(fields[i].getAnnotation(Unique.class)!=null){
                    builder.append("`"+StringUtil.Camel2Underline(fields[i].getName()) + "`=? and ");
                }
            }
            builder.delete(builder.length()-5,builder.length());
            sqlCache.put(key, builder.toString());
        }
        return sqlCache.getString(key);
    }

    /**返回根据id更新的语句*/
    public static String updateById(Class _class){
        String key = "updateById_"+_class.getName();
        if(!sqlCache.containsKey(key)){
            StringBuilder builder = new StringBuilder();
            builder.append("update " + StringUtil.Camel2Underline(_class.getSimpleName()) + " set ");
            Field[] fields = _class.getDeclaredFields();
            Field.setAccessible(fields,true);
            for(int i=0;i<fields.length;i++){
                if(fields[i].getName().equals("id")||fields[i].getAnnotation(Ignore.class)!=null){
                    continue;
                }
                builder.append("`"+StringUtil.Camel2Underline(fields[i].getName()) + "`=?,");
            }
            builder.deleteCharAt(builder.length() - 1);
            builder.append(" where id = ?");
            sqlCache.put(key,builder.toString());
        }
        return sqlCache.getString(key);
    }

    /**返回列名的SQL语句*/
    public static String columns(Class _class,String tableAlias){
        String key = "columnTable_"+_class.getName();
        if (!sqlCache.containsKey(key)){
            StringBuilder builder = new StringBuilder();
            //排除ignore注解在字段
            Field[] fields = _class.getDeclaredFields();
            Field.setAccessible(fields,true);
            for(Field field:fields){
                if(field.getDeclaredAnnotation(Ignore.class)==null){
                    String columnName = StringUtil.Camel2Underline(field.getName());
                    builder.append(tableAlias+".`"+columnName+"` as "+tableAlias+"_"+columnName+",");
                }
            }
            builder.deleteCharAt(builder.length()-1);
            sqlCache.put(key, builder.toString());
        }
        return sqlCache.getString(key);
    }
}
