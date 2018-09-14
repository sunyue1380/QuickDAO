package com.schoolwow.quickdao.dao;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.schoolwow.quickdao.annotation.*;
import com.schoolwow.quickdao.util.SQLUtil;
import com.schoolwow.quickdao.util.StatementUtil;
import com.schoolwow.quickdao.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.JarURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class AbstractDAO implements DAO {
    Logger logger = LoggerFactory.getLogger(AbstractDAO.class);
    protected Map<String, String> fieldMapping = new HashMap<String, String>();
    protected DataSource dataSource;
    protected String insertIgnoreSQL = "insert ignore into ";

    public AbstractDAO(DataSource dataSource) {
        this.dataSource = dataSource;
        fieldMapping.put("string", "VARCHAR(40)");
        fieldMapping.put("boolean", "BOOLEAN");
        fieldMapping.put("byte", "TINYINT");
        fieldMapping.put("short", "SMALLINT");
        fieldMapping.put("int", "INTEGER");
        fieldMapping.put("integer", "INTEGER");
        fieldMapping.put("long", "BIGINT");
        fieldMapping.put("float", "FLOAT");
        fieldMapping.put("double", "DOUBLE");
        fieldMapping.put("date", "DATETIME");
        fieldMapping.put("time", "TIME");
        fieldMapping.put("timestamp", "TIMESTAMP");
    }

    @Override
    public <T> T fetch(Class<T> _class, long id) throws SQLException, IllegalAccessException, InstantiationException {
        return fetch(_class, "id", id);
    }

    @Override
    public <T> T fetch(Class<T> _class, String property, Object value) throws SQLException, IllegalAccessException, InstantiationException {
        Connection connection = dataSource.getConnection();
        String fetchSQL = SQLUtil.fetch(_class, property);
        PreparedStatement ps = connection.prepareStatement(fetchSQL);
        switch (value.getClass().getSimpleName().toLowerCase()) {
            case "int": {
                ps.setInt(1, (int) value);
            }
            break;
            case "integer": {
                ps.setObject(1, (Integer) value);
            }
            break;
            case "long": {
                if (value.getClass().isPrimitive()) {
                    ps.setLong(1, (long) value);
                } else {
                    ps.setObject(1, value);
                }
            }
            break;
            case "boolean": {
                if (value.getClass().isPrimitive()) {
                    ps.setBoolean(1, (boolean) value);
                } else {
                    ps.setObject(1, value);
                }
            }
            break;
            case "string": {
                ps.setString(1, value == null ? "" : value.toString());
            }
            break;
            default: {
                ps.setObject(1, value);
            }
        }
        ResultSet resultSet = ps.executeQuery();
        T instance = null;
        if (resultSet.next()) {
            instance = _class.newInstance();
            Field[] fields = _class.getDeclaredFields();
            Field.setAccessible(fields, true);
            for (Field field : fields) {
                if (field.getAnnotation(Ignore.class) != null) {
                    continue;
                }
                StatementUtil.getSingleField(resultSet, instance, field, StringUtil.Camel2Underline(field.getName()));
            }
        }
        resultSet.close();
        ps.close();
        connection.close();
        return instance;
    }

    @Override
    public <T> Condition<T> query(Class<T> _class) {
        return new AbstractCondition<>(_class, dataSource);
    }

    protected String getInsertIgnoreSQL(){
        return this.insertIgnoreSQL;
    };

    @Override
    public long save(Object instance) throws SQLException, IllegalAccessException, NoSuchFieldException {
        if (instance == null) {
            return 0;
        }
        Connection connection = dataSource.getConnection();
        connection.setAutoCommit(false);
        PreparedStatement ps = null;

        //判断id是否存在
        Field id = instance.getClass().getDeclaredField("id");
        id.setAccessible(true);

        Field[] fields = instance.getClass().getDeclaredFields();
        Field.setAccessible(fields, true);
        long effect = 0;
        if (hasId(instance, id)) {
            //判断有无UniqueKey
            if (SQLUtil.hasUniqueKey(instance.getClass())) {
                String updateByUniqueKey = SQLUtil.updateByUniqueKey(instance.getClass());
                ps = connection.prepareStatement(updateByUniqueKey);
                //根据UniqueKey更新
                StatementUtil.addUpdateByUniqueKeyBatch(instance, fields, ps);
                effect = ps.executeBatch()[0];
            } else {
                //根据id更新
                String updateById = SQLUtil.updateById(instance.getClass());
                ps = connection.prepareStatement(updateById);
                StatementUtil.addUpdateByIdBatch(instance, fields, id, ps);
                effect = ps.executeBatch()[0];
            }
        } else {
            //insert ignore
            String insertIgnore = SQLUtil.insertIgnore(instance.getClass(),getInsertIgnoreSQL());
            ps = connection.prepareStatement(insertIgnore);
            StatementUtil.addInsertIgnoreBatch(instance, fields, ps);
            effect = ps.executeBatch()[0];
            if (effect == 0 && SQLUtil.hasUniqueKey(instance.getClass())) {
                //有Unique Key则根据Unique Key更新
                ps.close();
                String updateByUniqueKey = SQLUtil.updateByUniqueKey(instance.getClass());
                ps = connection.prepareStatement(updateByUniqueKey);
                //根据UniqueKey更新
                StatementUtil.addUpdateByUniqueKeyBatch(instance, fields, ps);
                effect = ps.executeBatch()[0];
            }
        }
        //setLastInsertId(connection,instance,id);
        ps.close();
        connection.commit();
        connection.setAutoCommit(true);
        connection.close();
        return effect;
    }

    @Override
    public long save(Object[] instances) throws SQLException, IllegalAccessException, NoSuchFieldException {
        if (instances == null || instances.length == 0) {
            return 0;
        }
        Connection connection = dataSource.getConnection();
        connection.setAutoCommit(false);
        String updateByUniqueKey = SQLUtil.updateByUniqueKey(instances[0].getClass());
        PreparedStatement updateByUniqueKeyPs = connection.prepareStatement(updateByUniqueKey);
        String updateById = SQLUtil.updateById(instances[0].getClass());
        PreparedStatement updateByIdPs = connection.prepareStatement(updateById);
        String insertIgnore = SQLUtil.insertIgnore(instances[0].getClass(),getInsertIgnoreSQL());
        PreparedStatement insertIgnorePs = connection.prepareStatement(insertIgnore);

        Field[] fields = instances[0].getClass().getDeclaredFields();
        Field.setAccessible(fields, true);
        boolean hasUniqueKey = SQLUtil.hasUniqueKey(instances[0].getClass());

        List<Object> insertInoreInstanceList = new ArrayList<>();
        for (Object instance : instances) {
            //判断id是否存在
            Field id = instance.getClass().getDeclaredField("id");
            id.setAccessible(true);

            if (hasId(instance, id)) {
                //判断有无UniqueKey
                if (hasUniqueKey) {
                    //根据UniqueKey更新
                    StatementUtil.addUpdateByUniqueKeyBatch(instance, fields, updateByUniqueKeyPs);
                } else {
                    //根据id更新
                    StatementUtil.addUpdateByIdBatch(instance, fields, id, updateByIdPs);
                }
            } else {
                //insert ignore
                StatementUtil.addInsertIgnoreBatch(instance, fields, insertIgnorePs);
                insertInoreInstanceList.add(instance);
            }
        }

        long effect = 0;

        int[] results = insertIgnorePs.executeBatch();
        for (int i = 0; i < results.length; i++) {
            if (results[i] == 0 && hasUniqueKey) {
                //添加更新操作
                StatementUtil.addUpdateByUniqueKeyBatch(insertInoreInstanceList.get(i), fields, updateByUniqueKeyPs);
            } else {
                effect++;
            }
        }
        //执行所有的操作相加
        results = updateByUniqueKeyPs.executeBatch();
        for (long result : results) {
            effect += result;
        }
        results = updateByIdPs.executeBatch();
        for (long result : results) {
            effect += result;
        }

        insertIgnorePs.close();
        updateByIdPs.close();
        updateByUniqueKeyPs.close();
        connection.commit();
        connection.setAutoCommit(true);
        connection.close();
        return effect;
    }

    @Override
    public long save(List instanceList) throws SQLException, IllegalAccessException, NoSuchFieldException {
        return save(instanceList.toArray(new Object[instanceList.size()]));
    }

    @Override
    public long delete(Class _class, long id) throws SQLException {
        return delete(_class, "id", id);
    }

    @Override
    public long delete(Class _class, String property, Object value) throws SQLException {
        Connection connection = dataSource.getConnection();
        PreparedStatement ps = connection.prepareStatement(SQLUtil.delete(_class, property));
        ps.setObject(1, value);
        long effect = ps.executeUpdate();
        return effect;
    }

    @Override
    public long clear(Class _class) throws SQLException {
        Connection connection = dataSource.getConnection();
        PreparedStatement ps = connection.prepareStatement("delete from " + StringUtil.Camel2Underline(_class.getSimpleName()));
        long effect = ps.executeUpdate();
        return effect;
    }

    private boolean hasId(Object instance, Field id) throws IllegalAccessException {
        //判断主键类型
        int type = -1;//0-insert,1-updateById
        switch (id.getType().getSimpleName().toLowerCase()) {
            case "int": {
                type = id.getInt(instance) <= 0 ? 0 : 1;
            }
            break;
            case "integer": {
                type = (id.get(instance) == null || id.getInt(instance) <= 0) ? 0 : 1;
            }
            break;
            case "long": {
                type = ((!id.getType().isPrimitive() && id.get(instance) == null) || id.getLong(instance) <= 0) ? 0 : 1;
            }
            break;
            case "string": {
                type = id.get(instance) == null ? 0 : 1;
            }
            break;
            default: {
                throw new IllegalArgumentException("无法识别的主键类型:" + id.getType().getSimpleName().toLowerCase());
            }
        }
        if (type == 0) {
            return false;
        } else {
            return true;
        }
    }

    @Override
    public JSONArray getEntityInfo(String packageName, String[] filterPackageName) throws IOException, ClassNotFoundException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        Enumeration<URL> enumeration = classLoader.getResources(packageName.replace(".", "/"));
        List<Class> classes = new ArrayList<>();
        String packageNamePath = packageName.replace(".", "/");
        while (enumeration.hasMoreElements()) {
            URL url = enumeration.nextElement();
            //判断协议
            if ("file".equals(url.getProtocol())) {
                String packagePath = url.getPath().replaceAll("%20", " ");
                File root = new File(packagePath);
                if (root.isDirectory()) {
                    //默认只加一级文件夹下的实体类
                    for (File file : root.listFiles(file -> file.isFile() && file.getName().endsWith(".class"))) {
                        String className = packageName + "." + file.getName().substring(0, file.getName().length() - 6);
                        if (filterPackageName == null || !filter(filterPackageName, className)) {
                            classes.add(classLoader.loadClass(className));
                        }
                    }
                }
            } else if ("jar".equals(url.getProtocol())) {
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
                                if (filterPackageName == null || !filter(filterPackageName, className)) {
                                    classes.add(classLoader.loadClass(className));
                                }
                            }
                        }
                    }
                }
            }
        }
        if (classes.size() == 0) {
            return new JSONArray();
        }
        JSONArray entityList = getEntityInfo(classes);
        return entityList;
    }

    @Override
    public JSONArray getDatabaseInfo() throws SQLException {
        Connection connection = dataSource.getConnection();
        PreparedStatement tablePs = connection.prepareStatement("show tables;");
        ResultSet tableRs = tablePs.executeQuery();
        //(1)获取所有表
        JSONArray entityList = new JSONArray();
        while (tableRs.next()) {
            JSONObject entity = new JSONObject();
            entity.put("tableName",tableRs.getString(1));

            JSONArray properties = new JSONArray();
            PreparedStatement propertyPs = connection.prepareStatement("show columns from " + tableRs.getString(1));
            ResultSet propertiesRs = propertyPs.executeQuery();
            while (propertiesRs.next()) {
                JSONObject property = new JSONObject();
                property.put("column", propertiesRs.getString("Field"));
                property.put("columnType", propertiesRs.getString("Type"));
                property.put("notNull","NO".equals(propertiesRs.getString("Null")));
                property.put("unique","UNI".equals(propertiesRs.getString("Key")));
                if (null != propertiesRs.getString("Default")) {
                    property.put("default", propertiesRs.getString("Default"));
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

    @Override
    public void updateDatabase(JSONArray entityList, JSONArray dbEntityList) throws SQLException {
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
                StringBuilder builder = new StringBuilder("create table "+tableName+"(");
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
                String uniqueSQL = "alter table "+tableName+" add unique index "+tableName+"_"+uniqueColumns.toString().replace(",","_")+"_unique_index "+"("+uniqueColumns.toString()+");";
                logger.debug("unique sql:"+uniqueSQL+",result:"+connection.prepareStatement(uniqueSQL).executeUpdate());
            }
        }
        connection.commit();
        connection.setAutoCommit(true);
        connection.close();
    }

    protected void addNewColumn(Connection connection, String tableName, StringBuilder uniqueColumns, JSONArray sourceProperties, JSONArray targetProperties) throws SQLException {
        for (int j = 0; j < sourceProperties.size(); j++) {
            JSONObject sourceProperty = sourceProperties.getJSONObject(j);
            JSONObject targetProperty = getValue(targetProperties, "column", sourceProperty.getString("column"));
            if (targetProperty == null) {
                //新增属性
                String column = sourceProperty.getString("column");
                String columnType = sourceProperty.containsKey("columnType") ? sourceProperty.getString("columnType") : fieldMapping.get(sourceProperty.getString("type"));

                StringBuilder builder = new StringBuilder();
                builder.append("alter table " + tableName + " add column " + "`" + column + "` " + columnType);
                if (sourceProperty.containsKey("default")) {
                    builder.append(" default " + sourceProperty.getString("default"));
                }
                String sql = builder.toString().replaceAll("\\s+", " ");
                logger.debug("add column:" + sql + ",result:" + connection.prepareStatement(sql).executeUpdate());

                if (sourceProperty.getBoolean("unique")) {
                    //统一添加unique约束
                    uniqueColumns.append(column + ",");
                }
            }
        }
    }

    protected JSONObject getValue(JSONArray array, String propertyName, String value) {
        for (int i = 0; i < array.size(); i++) {
            if (array.getJSONObject(i).getString(propertyName).equals(value)) {
                return array.getJSONObject(i);
            }
        }
        return null;
    }

    private boolean filter(String[] values, String value) {
        for (int i = 0; i < values.length; i++) {
            if (value.startsWith(values[i])) {
                return true;
            }
        }
        return false;
    }

    private JSONArray getEntityInfo(List<Class> classes){
        JSONArray entityList = new JSONArray();
        for (Class c : classes) {
            JSONObject entity = new JSONObject();
            entity.put("ignore", c.getDeclaredAnnotation(Ignore.class) != null);
            entity.put("tableName", StringUtil.Camel2Underline(c.getSimpleName()));

            //添加表属性
            Field[] fields = c.getDeclaredFields();
            Field.setAccessible(fields, true);
            JSONArray properties = new JSONArray();
            for (int i = 0; i < fields.length; i++) {
                JSONObject property = new JSONObject();
                property.put("ignore", fields[i].getDeclaredAnnotation(Ignore.class) != null);
                property.put("column", StringUtil.Camel2Underline(fields[i].getName()));
                property.put("unique", fields[i].getDeclaredAnnotation(Unique.class) != null);
                property.put("notNull", fields[i].getDeclaredAnnotation(NotNull.class) != null);
                if ("id".equals(property.getString("column"))) {
                    property.put("unique", true);
                    property.put("notNull", true);
                }
                if (fields[i].getDeclaredAnnotation(ColumnType.class) != null) {
                    property.put("columnType", fields[i].getDeclaredAnnotation(ColumnType.class).value());
                }
                if (fields[i].getDeclaredAnnotation(DefaultValue.class) != null) {
                    property.put("default", fields[i].getDeclaredAnnotation(DefaultValue.class).value());
                }
                property.put("type", fields[i].getType().getSimpleName().toLowerCase());
                properties.add(property);
            }
            entity.put("properties", properties);
            entityList.add(entity);
        }
        return entityList;
    }
}
