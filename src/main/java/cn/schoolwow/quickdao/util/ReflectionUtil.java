package cn.schoolwow.quickdao.util;

import cn.schoolwow.quickdao.annotation.*;
import cn.schoolwow.quickdao.domain.Entity;
import cn.schoolwow.quickdao.domain.Property;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.JarURLConnection;
import java.net.URL;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ReflectionUtil {
    private static Logger logger = LoggerFactory.getLogger(ReflectionUtil.class);
    private static String placeHolder = "** NOT SPECIFIED **";
    /**记录实体类信息*/
    public static Map<String, Entity> entityMap = new HashMap<>();

    /**对象是否存在id*/
    public static boolean hasId(Object instance) throws IllegalAccessException {
        Property property = entityMap.get(instance.getClass().getName()).id;
        String type = property.type;
        switch (type) {
            case "int": {
                return property.field.getInt(instance) >0;
            }
            case "integer": {
                return (property.field.get(instance) != null && property.field.getInt(instance) > 0);
            }
            case "long": {
                if(property.field.getType().isPrimitive()){
                    return property.field.getLong(instance)>0;
                }else{
                    return property.field.get(instance) == null;
                }
            }
            default: {
                throw new IllegalArgumentException("无法识别的主键类型:" + type);
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
        Property[] properties = ReflectionUtil.entityMap.get(instance.getClass().getName()).properties;
        StringBuilder sqlBuilder = new StringBuilder(sql.replace("?",placeHolder));
        for(Property property:properties){
            if(property.id){
                continue;
            }
            String parameter = setParameter(instance, ps, parameterIndex, property.field);
            int indexOf = sqlBuilder.indexOf(placeHolder);
            if(indexOf>=0){
                sqlBuilder.replace(indexOf,indexOf+placeHolder.length(),parameter);
            }
            parameterIndex++;
        }
        return sqlBuilder.toString();
    }

    /**根据id更新
     * 为prepareStatement赋值
     * */
    public static String setValueWithUpdateById(PreparedStatement ps,Object instance,String sql) throws SQLException, IllegalAccessException, NoSuchFieldException {
        int parameterIndex = 1;
        Property[] properties = ReflectionUtil.entityMap.get(instance.getClass().getName()).properties;
        Property id = null;
        StringBuilder sqlBuilder = new StringBuilder(sql.replace("?",placeHolder));
        for(Property property:properties){
            if(property.id){
                id = property;
                continue;
            }
            String parameter = setParameter(instance, ps, parameterIndex, property.field);
            int indexOf = sqlBuilder.indexOf(placeHolder);
            if(indexOf>=0){
                sqlBuilder.replace(indexOf,indexOf+placeHolder.length(),parameter);
            }
            parameterIndex++;
        }
        //再设置id属性
        String parameter = setParameter(instance, ps, parameterIndex, id.field);
        int indexOf = sqlBuilder.indexOf(placeHolder);
        if(indexOf>=0){
            sqlBuilder.replace(indexOf,indexOf+placeHolder.length(),parameter);
        }
        return sqlBuilder.toString();
    }

    /**
     * 根据UniqueKey更新
     * 为prepareStatement赋值*/
    public static String setValueWithUpdateByUniqueKey(PreparedStatement ps,Object instance,String sql) throws SQLException, IllegalAccessException {
        int parameterIndex = 1;
        Property[] properties = ReflectionUtil.entityMap.get(instance.getClass().getName()).properties;
        StringBuilder sqlBuilder = new StringBuilder(sql.replace("?",placeHolder));
        for(Property property:properties){
            //先设置非id和Unique字段
            if(property.id||property.unique){
                continue;
            }
            String parameter = setParameter(instance, ps, parameterIndex, property.field);
            int indexOf = sqlBuilder.indexOf(placeHolder);
            if(indexOf>=0){
                sqlBuilder.replace(indexOf,indexOf+placeHolder.length(),parameter);
            }
            parameterIndex++;
        }
        for(Property property:properties){
            //再设置Unique字段查询条件
            if(property.unique){
                String parameter = setParameter(instance, ps, parameterIndex, property.field);
                int indexOf = sqlBuilder.indexOf(placeHolder);
                if(indexOf>=0){
                    sqlBuilder.replace(indexOf,indexOf+placeHolder.length(),parameter);
                }
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
    public static JSONArray mappingResultSetToJSONArray(ResultSet resultSet,int count) throws SQLException {
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

    /**获取实体类信息*/
    public static void getEntityInfo() throws ClassNotFoundException, IOException {
        Set<String> keySet = QuickDAOConfig.packageNameMap.keySet();
        for(String packageName:keySet) {
            List<Class> classList = scanEntity(packageName);
            for (Class c : classList) {
                String tableName = null;
                if((packageName.length()+c.getSimpleName().length()+1)==c.getName().length()){
                    tableName = StringUtil.Camel2Underline(c.getSimpleName());
                }else{
                    String prefix = c.getName().substring(packageName.length()+1,c.getName().lastIndexOf(".")).replace(".","_");
                    tableName = prefix+"@"+StringUtil.Camel2Underline(c.getSimpleName());
                }
                Entity entity = new Entity();
                entity._class = c;
                entity.tableName = QuickDAOConfig.packageNameMap.get(packageName)+tableName;
                entityMap.put(c.getName(),entity);
            }
            for (Class c : classList) {
                Entity entity = entityMap.get(c.getName());
                entity.ignore = c.getDeclaredAnnotation(Ignore.class) != null;
                if(entity.ignore){
                    logger.debug("[忽略实体类]类名:{},该类被@Ignore注解修饰,将跳过该实体类!",c.getName());
                    continue;
                }
                entity.className = c.getSimpleName();
                if(c.getDeclaredAnnotation(Comment.class)!=null){
                    Comment comment = (Comment) c.getDeclaredAnnotation(Comment.class);
                    entity.comment = comment.value();
                }
                /**属性列表*/
                List<Property> propertyList = new ArrayList<>();
                /**唯一约束属性列表*/
                List<Property> uniqueFieldPropertyList = new ArrayList<>();
                /**外键约束属性列表*/
                List<Property> foreignKeyPropertyList = new ArrayList<>();
                /**实体包类列表*/
                List<Field> compositFieldList = new ArrayList<>();
                //添加字段信息
                {
                    Field[] fields = c.getDeclaredFields();
                    Field.setAccessible(fields,true);
                    for (int i = 0; i < fields.length; i++) {
                        if(fields[i].getDeclaredAnnotation(Ignore.class)!= null){
                            logger.debug("[跳过实体属性]{},该属性被Ignore注解修饰!",fields[i].getName());
                            continue;
                        }
                        //记录实体包类
                        if(isCompositProperty(fields[i].getType())){
                            compositFieldList.add(fields[i]);
                            continue;
                        }
                        Property property = new Property();
                        property.column = StringUtil.Camel2Underline(fields[i].getName());
                        if(fields[i].getAnnotation(ColumnType.class)!=null){
                            property.columnType = fields[i].getAnnotation(ColumnType.class).value();
                        }
                        property.name = fields[i].getName();
                        property.type = fields[i].getType().getSimpleName().toLowerCase();
                        property.unique = fields[i].getDeclaredAnnotation(Unique.class) != null;
                        if(property.unique){
                            uniqueFieldPropertyList.add(property);
                        }
                        property.notNull = fields[i].getDeclaredAnnotation(NotNull.class) != null;
                        property.id = fields[i].getDeclaredAnnotation(Id.class) != null||"id".equals(property.column);
                        if(property.id){
                            property.notNull = true;
                            entity.id = property;
                        }
                        if (fields[i].getDeclaredAnnotation(DefaultValue.class) != null) {
                            property.defaultValue = fields[i].getDeclaredAnnotation(DefaultValue.class).value();
                        }
                        if(fields[i].getDeclaredAnnotation(Comment.class)!=null){
                            property.comment = fields[i].getDeclaredAnnotation(Comment.class).value();
                        }
                        ForeignKey foreignKey = fields[i].getDeclaredAnnotation(ForeignKey.class);
                        if(foreignKey!=null){
                            String operation = foreignKey.foreignKeyOption().getOperation();
                            property.foreignKey = "`"+entityMap.get(foreignKey.table().getName()).tableName+"`(`"+foreignKey.field()+"`) ON DELETE "+operation+" ON UPDATE "+operation;
                            property.foreignKeyName = "FK_"+entity.tableName+"_"+foreignKey.field()+"_"+entityMap.get(foreignKey.table().getName()).tableName+"_"+property.name;
                            foreignKeyPropertyList.add(property);
                        }
                        property.field =fields[i];
                        propertyList.add(property);
                    }
                }
                entity.properties = propertyList.toArray(new Property[0]);
                Field[] fields = new Field[entity.properties.length];
                for(int i=0;i<entity.properties.length;i++){
                    fields[i] = entity.properties[i].field;
                }
                entity.fields = fields;
                if(compositFieldList.size()>0){
                    entity.compositFields = compositFieldList.toArray(new Field[0]);
                }
                if(uniqueFieldPropertyList.size()>0){
                    entity.uniqueKeyProperties = uniqueFieldPropertyList.toArray(new Property[0]);
                }
                if(foreignKeyPropertyList.size()>0){
                    entity.foreignKeyProperties = foreignKeyPropertyList.toArray(new Property[0]);
                }
            }
        }
        logger.debug("[获取实体信息]实体类个数:{}",entityMap.size());
    }

    /**扫描实体包*/
    private static List<Class> scanEntity(String packageName) throws ClassNotFoundException, IOException {
        String packageNamePath = packageName.replace(".", "/");
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        URL url = classLoader.getResource(packageNamePath);
        if (url == null) {
            throw new IllegalArgumentException("无法识别的包路径:" + packageNamePath);
        }
        List<Class> classList = new ArrayList<>();
        switch (url.getProtocol()) {
            case "file": {
                File file = new File(url.getFile());
                //TODO 对于有空格或者中文路径会无法识别
                logger.info("[类文件路径]{}", file.getAbsolutePath());
                if (!file.isDirectory()) {
                    throw new IllegalArgumentException("包名不是合法的文件夹!" + url.getFile());
                }
                Stack<File> stack = new Stack<>();
                stack.push(file);

                String indexOfString = packageName.replace(".", "/");
                while (!stack.isEmpty()) {
                    file = stack.pop();
                    for (File f : file.listFiles()) {
                        if (f.isDirectory()) {
                            stack.push(f);
                        } else if (f.isFile() && f.getName().endsWith(".class")) {
                            String path = f.getAbsolutePath().replace("\\", "/");
                            int startIndex = path.indexOf(indexOfString);
                            String className = path.substring(startIndex, path.length() - 6).replace("/", ".");
                            classList.add(Class.forName(className));
                        }
                    }
                }
            }
            break;
            case "jar": {
                JarURLConnection jarURLConnection = (JarURLConnection) url.openConnection();
                if (null != jarURLConnection) {
                    JarFile jarFile = jarURLConnection.getJarFile();
                    if (null != jarFile) {
                        Enumeration<JarEntry> jarEntries = jarFile.entries();
                        while (jarEntries.hasMoreElements()) {
                            JarEntry jarEntry = jarEntries.nextElement();
                            String jarEntryName = jarEntry.getName();
                            if (jarEntryName.contains(packageNamePath) && jarEntryName.endsWith(".class")) { //是否是类,是类进行加载
                                String className = jarEntryName.substring(0, jarEntryName.lastIndexOf(".")).replaceAll("/", ".");
                                classList.add(Class.forName(className));
                            }
                        }
                    }
                }
            }
            break;
        }
        if (classList.size() == 0) {
            logger.warn("[扫描实体类信息为空]前缀:{},包名:{}", QuickDAOConfig.packageNameMap.get(packageName), packageName);
            return classList;
        }
        Stream<Class> stream = classList.stream().filter((_class)->{
            boolean result = true;
            //根据类过滤
            if(QuickDAOConfig.ignoreClassList!=null){
                if(QuickDAOConfig.ignoreClassList.contains(_class)){
                    logger.warn("[忽略类名]类名:{}!",_class.getName());
                    result = false;
                }
            }
            //根据包名过滤
            if(QuickDAOConfig.ignorePackageNameList!=null){
                for(String ignorePackageName:QuickDAOConfig.ignorePackageNameList){
                    if(_class.getName().contains(ignorePackageName)){
                        logger.warn("[忽略包名]包名:{}类名:{}",ignorePackageName,_class.getName());
                        result = false;
                    }
                }
            }
            return result;
        });
        if(QuickDAOConfig.predicate!=null){
            stream.filter(QuickDAOConfig.predicate);
        }
        classList = stream.collect(Collectors.toList());
        return classList;
    }

    /**判断是否是实体包类**/
    private static boolean isCompositProperty(Class _class){
        Set<String> packageNameSet = QuickDAOConfig.packageNameMap.keySet();
        for(String packageName:packageNameSet){
            if(_class.getName().contains(packageName)){
                return true;
            }
        }
        return false;
    }
}
