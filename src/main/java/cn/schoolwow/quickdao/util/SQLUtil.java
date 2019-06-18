package cn.schoolwow.quickdao.util;

import cn.schoolwow.quickdao.domain.Entity;
import cn.schoolwow.quickdao.domain.Property;

import java.util.concurrent.ConcurrentHashMap;

public class SQLUtil {
    /**缓存SQL语句*/
    private static ConcurrentHashMap<String,String> sqlCache = new ConcurrentHashMap<>();

    /**返回fetch语句*/
    public static String fetch(Class _class,String property) {
        String key = "fetch_" + _class.getName()+"_"+property;
        if (!sqlCache.containsKey(key)) {
            String tableName = ReflectionUtil.entityMap.get(_class.getName()).tableName;
            String fetchSQL = "select " + columns(_class,"t") + " from `" + tableName + "` as t where t.`"+StringUtil.Camel2Underline(property)+"` = ?";
            sqlCache.put(key, fetchSQL);
        }
        return sqlCache.get(key);
    }

    /**返回fetch语句*/
    public static String fetchNull(Class _class,String property) {
        String key = "fetch_" + _class.getName()+"_"+property;
        if (!sqlCache.containsKey(key)) {
            String tableName = ReflectionUtil.entityMap.get(_class.getName()).tableName;
            String fetchSQL = "select " + columns(_class,"t") + " from `" + tableName + "` as t where t.`"+StringUtil.Camel2Underline(property)+"` is null";
            sqlCache.put(key, fetchSQL);
        }
        return sqlCache.get(key);
    }

    /**返回根据属性删除的语句*/
    public static String delete(Class _class,String property) {
        String key = "delete_" + _class.getName()+"_"+property;
        if (!sqlCache.containsKey(key)) {
            String tableName = ReflectionUtil.entityMap.get(_class.getName()).tableName;
            String fetchSQL = "delete from `" + tableName + "` where `"+StringUtil.Camel2Underline(property)+"` = ?";
            sqlCache.put(key, fetchSQL);
        }
        return sqlCache.get(key);
    }

    /**返回insertIgnore语句*/
    public static String insertIgnore(Class _class,String insertIgnoreSQL) {
        String key = "insertIgnore_" + insertIgnoreSQL+"_"+_class.getName();
        if (!sqlCache.containsKey(key)) {
            StringBuilder builder = new StringBuilder();
            Entity entity = ReflectionUtil.entityMap.get(_class.getName());
            builder.append(insertIgnoreSQL+" `" + entity.tableName+"`(");

            for(Property property:entity.properties){
                if(property.id){
                    continue;
                }
                builder.append("`"+StringUtil.Camel2Underline(property.name) + "`,");
            }
            builder.deleteCharAt(builder.length()-1);
            builder.append(") values(");
            for(Property property:entity.properties){
                if(property.id){
                    continue;
                }
                builder.append("?,");
            }
            builder.deleteCharAt(builder.length()-1);
            builder.append(")");
            sqlCache.put(key, builder.toString());
        }
        return sqlCache.get(key);
    }

    /**
     * 返回根据UniqueKey更新的SQL语句
     * */
    public static String updateByUniqueKey(Class _class) {
        String key = "updateByUniqueKey_" + _class.getName();
        if (!sqlCache.containsKey(key)) {
            StringBuilder builder = new StringBuilder();
            Entity entity = ReflectionUtil.entityMap.get(_class.getName());
            builder.append("update `" + entity.tableName+"` set ");

            for(Property property:entity.properties){
                if(property.id||property.unique){
                    continue;
                }
                builder.append("`"+StringUtil.Camel2Underline(property.name) + "`=?,");
            }
            builder.deleteCharAt(builder.length()-1);
            builder.append(" where ");
            for(Property property:entity.properties){
                if(property.unique){
                    builder.append("`"+StringUtil.Camel2Underline(property.name) + "`=? and ");
                }
            }
            builder.delete(builder.length()-5,builder.length());
            sqlCache.put(key, builder.toString());
        }
        return sqlCache.get(key);
    }

    /**返回根据id更新的语句*/
    public static String updateById(Class _class){
        String key = "updateById_"+_class.getName();
        if(!sqlCache.containsKey(key)){
            StringBuilder builder = new StringBuilder();
            Entity entity = ReflectionUtil.entityMap.get(_class.getName());
            builder.append("update `" + entity.tableName + "` set ");

            for(Property property:entity.properties){
                if(property.id){
                    continue;
                }
                builder.append("`"+StringUtil.Camel2Underline(property.name) + "`=?,");
            }
            builder.deleteCharAt(builder.length() - 1);
            builder.append(" where `"+entity.id.name+"` = ?");
            sqlCache.put(key,builder.toString());
        }
        return sqlCache.get(key);
    }

    /**返回列名的SQL语句*/
    public static String columns(Class _class,String tableAlias){
        String key = "columnTable_"+_class.getName()+"_"+tableAlias;
        if (!sqlCache.containsKey(key)){
            StringBuilder builder = new StringBuilder();
            Property[] properties = ReflectionUtil.entityMap.get(_class.getName()).properties;
            for(Property property:properties){
                builder.append(tableAlias+".`"+property.column+"` as "+tableAlias+"_"+property.column+",");
            }
            builder.deleteCharAt(builder.length()-1);
            sqlCache.put(key, builder.toString());
        }
        return sqlCache.get(key);
    }
}
