package cn.schoolwow.quickdao.dao;

import cn.schoolwow.quickdao.annotation.*;
import cn.schoolwow.quickdao.condition.AbstractCondition;
import cn.schoolwow.quickdao.condition.Condition;
import cn.schoolwow.quickdao.condition.SqliteCondition;
import cn.schoolwow.quickdao.util.*;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
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
import java.util.function.Predicate;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public abstract class AbstractDAO implements DAO {
    Logger logger = LoggerFactory.getLogger(AbstractDAO.class);
    /**字段映射*/
    protected Map<String, String> fieldMapping = new HashMap<String, String>();
    private DataSource dataSource;

    public AbstractDAO(DataSource dataSource) {
        this.dataSource = dataSource;
        fieldMapping.put("string", "VARCHAR(255)");
        fieldMapping.put("boolean", "BOOLEAN");
        fieldMapping.put("byte", "TINYINT");
        fieldMapping.put("short", "SMALLINT");
        fieldMapping.put("int", "INTEGER");
        fieldMapping.put("integer", "INTEGER");
        fieldMapping.put("long", "BIGINT");
        fieldMapping.put("float", "FLOAT(4,2)");
        fieldMapping.put("double", "DOUBLE(5,2)");
        fieldMapping.put("date", "DATETIME");
        fieldMapping.put("time", "TIME");
        fieldMapping.put("timestamp", "TIMESTAMP");
    }

    protected enum Syntax{
        AutoIncrement,
        InsertIgnore;
    }

    /**提取各个数据库产品的SQL差异部分的语法*/
    protected abstract String getSyntax(Syntax syntax);

    /**获取唯一约束SQL语句*/
    protected abstract String getUniqueStatement(String tableName,List<String> columns);

    @Override
    public <T> T fetch(Class<T> _class, long id){
        return fetch(_class, "id", id);
    }

    @Override
    public <T> T fetch(Class<T> _class, String property, Object value){
        List<T> instanceList = fetchList(_class,property,value);
        if(ValidateUtil.isNotEmpty(instanceList)){
            return instanceList.get(0);
        }else{
            return null;
        }
    }

    @Override
    public <T> List<T> fetchList(Class<T> _class, String property, Object value){
        try {
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
            int count = (int) query(_class).addQuery(property,value).count();
            List<T> instanceList = new ArrayList(count);
            ResultSet resultSet = ps.executeQuery();
            logger.debug("[根据属性{}=>{}获取对象]执行sql:{}",property,value,fetchSQL.replace("?",value.toString()));
            ReflectionUtil.mappingResultToList(resultSet,instanceList,_class);
            ps.close();
            connection.close();
            return instanceList;
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public <T> Condition<T> query(Class<T> _class) {
        //根据类型返回对应
        if(this instanceof SQLiteDAO){
            return new SqliteCondition(_class,dataSource);
        }else{
            return new AbstractCondition<>(_class, dataSource);
        }
    }

    @Override
    public long save(Object instance){
        if (instance == null) {
            return 0;
        }
        try {
            Connection connection = dataSource.getConnection();
            Class _class = instance.getClass();
            PreparedStatement ps = null;
            long effect = 0;

            //先判断有无id,无id则直接插入,有id则再判断是否有唯一性约束
            if(ReflectionUtil.hasId(instance)) {
                if (ReflectionUtil.hasUniqueKey(_class)) {
                    //如果有唯一性约束则以唯一性约束更新
                    String updateByUniqueKey = SQLUtil.updateByUniqueKey(instance.getClass());
                    ps = connection.prepareStatement(updateByUniqueKey);
                    logger.debug("[根据唯一性约束更新]执行SQL:{}",ReflectionUtil.setValueWithUpdateByUniqueKey(ps,instance,updateByUniqueKey));
                    effect = ps.executeUpdate();
                    //获取id并设置
                    Condition condition = query(instance.getClass());
                    Field[] fields = ReflectionUtil.getFields(_class);
                    for(Field field:fields){
                        if(field.getAnnotation(Unique.class)!=null){
                            condition.addQuery(StringUtil.Camel2Underline(field.getName()),field.get(instance));
                        }
                    }
                    List<Long> ids = condition.getValueList(Long.class,"id");
                    if(ids.size()>0){
                        ReflectionUtil.setId(instance,ids.get(0));
                    }
                }else{
                    //根据id更新
                    String updateById = SQLUtil.updateById(instance.getClass());
                    ps = connection.prepareStatement(updateById);
                    logger.debug("[根据id更新]执行SQL:{}", ReflectionUtil.setValueWithUpdateById(ps, instance, updateById));
                    effect = ps.executeUpdate();
                }
            }else{
                //执行insertIgnore
                String insertIgnore = SQLUtil.insertIgnore(instance.getClass(),getSyntax(Syntax.InsertIgnore));
                ps = connection.prepareStatement(insertIgnore,PreparedStatement.RETURN_GENERATED_KEYS);
                logger.debug("[执行插入操作]执行SQL:{}",ReflectionUtil.setValueWithInsertIgnore(ps,instance,insertIgnore));
                effect = ps.executeUpdate();
                if(effect>0){
                    //获取主键
                    ResultSet rs = ps.getGeneratedKeys();
                    rs.next();
                    long id = rs.getLong(1);
                    ReflectionUtil.setId(instance,id);
                    rs.close();
                }
            }
            ps.close();
            connection.close();
            return effect;
        }catch (Exception e){
            e.printStackTrace();
            return -1;
        }
    }

    @Override
    public long save(List instanceList) {
        if (instanceList == null || instanceList.size() == 0) {
            return 0;
        }
        try {
            Connection connection = dataSource.getConnection();
            connection.setAutoCommit(false);

            String updateByUniqueKey = SQLUtil.updateByUniqueKey(instanceList.get(0).getClass());
            PreparedStatement _updateByUniqueKeyPs = null;
            if (ReflectionUtil.hasUniqueKey(instanceList.get(0).getClass())) {
                //如果有则获取对应语句
                _updateByUniqueKeyPs = connection.prepareStatement(updateByUniqueKey);
            }
            PreparedStatement updateByUniqueKeyPs = _updateByUniqueKeyPs;

            String updateById = SQLUtil.updateById(instanceList.get(0).getClass());
            PreparedStatement updateByIdPs = connection.prepareStatement(updateById);
            String insertIgnore = SQLUtil.insertIgnore(instanceList.get(0).getClass(),getSyntax(Syntax.InsertIgnore));
            PreparedStatement insertIgnorePs = connection.prepareStatement(insertIgnore);
            //根据每个实体类具体情况插入
            instanceList.stream().forEach((instance)->{
                try {
                    if(ReflectionUtil.hasId(instance)){
                        if (ReflectionUtil.hasUniqueKey(instance.getClass())) {
                            //如果有唯一性约束则以唯一性约束更新
                            logger.debug("[根据唯一性约束更新]执行SQL:{}",ReflectionUtil.setValueWithUpdateByUniqueKey(updateByUniqueKeyPs,instance,updateByUniqueKey));
                            updateByUniqueKeyPs.addBatch();
                        }else{
                            //根据id更新
                            logger.debug("[根据id更新]执行SQL:{}",ReflectionUtil.setValueWithUpdateById(updateByIdPs,instance,updateById));
                            updateByIdPs.addBatch();
                        }
                    }else{
                        //执行insertIgnore
                        logger.debug("[执行插入操作]执行SQL:{}",ReflectionUtil.setValueWithInsertIgnore(insertIgnorePs,instance,insertIgnore));
                        insertIgnorePs.addBatch();
                    }
                }catch (Exception e){
                    logger.warn("[插入单个记录失败]{}",JSON.toJSONString(instance));
                    e.printStackTrace();
                }
            });

            //执行Batch并将所有结果添加
            long effect = 0;
            PreparedStatement[] preparedStatements = {updateByUniqueKeyPs,updateByIdPs,insertIgnorePs};
            for(int i=0;i<preparedStatements.length;i++){
                if(preparedStatements[i]==null){
                    continue;
                }
                int[] results = preparedStatements[i].executeBatch();
                for (long result : results) {
                    effect += result;
                }
                preparedStatements[i].close();
            }
            connection.commit();
            connection.setAutoCommit(true);
            connection.close();
            return effect;
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return -1;
    }

    @Override
    public long save(Object[] instances) {
        return save(Arrays.asList(instances));
    }

    @Override
    public long delete(Class _class, long id) {
        return delete(_class, "id", id);
    }

    @Override
    public long delete(Class _class, String property, Object value){
        try {
            Connection connection = dataSource.getConnection();
            String deleteSQL = SQLUtil.delete(_class, property);
            PreparedStatement ps = connection.prepareStatement(deleteSQL);
            ps.setObject(1, value);
            logger.debug("[根据属性{}=>{}删除]执行SQL:{}",property,value,deleteSQL.replace("?",value.toString()));
            long effect = ps.executeUpdate();
            ps.close();
            connection.close();
            return effect;
        }catch (Exception e){
            e.printStackTrace();
            return -1;
        }
    }

    @Override
    public long clear(Class _class){
        try {
            Connection connection = dataSource.getConnection();
            String sql = "delete from `" + SQLUtil.classTableMap.get(_class)+"`";
            logger.debug("[删除{}表]执行SQL:{}", _class.getSimpleName(),sql);
            PreparedStatement ps = connection.prepareStatement(sql);
            long effect = ps.executeUpdate();
            ps.close();
            connection.close();
            return effect;
        }catch (Exception e){
            e.printStackTrace();
            return -1;
        }
    }

    /**获取实体类信息同时过滤*/
    protected JSONArray getEntityInfo(String packageName,Predicate<Class> predicate) throws IOException, ClassNotFoundException {
        String packageNamePath = packageName.replace(".", "/");
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        URL url = classLoader.getResource(packageNamePath);
        List<Class> classList = new ArrayList<>();
        if("file".equals(url.getProtocol())){
            File file = new File(url.getFile());
            logger.info("[类文件路径]{}",file.getAbsolutePath());
            if(!file.isDirectory()){
                throw new IllegalArgumentException("包名不是合法的文件夹!");
            }
            Stack<File> stack = new Stack<>();
            stack.push(file);

            String indexOfString = packageName.replace(".","\\");
            while(!stack.isEmpty()){
                file = stack.pop();
                for(File f:file.listFiles()){
                    if(f.isDirectory()){
                        stack.push(f);
                    }else if(f.isFile()&&f.getName().endsWith(".class")){
                        String path = f.getAbsolutePath();
                        int startIndex = path.indexOf(indexOfString);
                        String className = path.substring(startIndex,path.length()-6).replace("\\",".");
                        classList.add(Class.forName(className));
                    }
                }
            }
        }else if("jar".equals(url.getProtocol())){
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
        if (classList.size() == 0) {
            return new JSONArray();
        }
        //过滤类名或者包名
        if(predicate!=null){
            classList = classList.stream().filter(predicate).collect(Collectors.toList());
        }
        JSONArray entityList = new JSONArray();
        for (Class c : classList) {
            JSONObject entity = new JSONObject();
            entity.put("ignore", c.getDeclaredAnnotation(Ignore.class) != null);
            //支持实体包多个文件夹
            if((packageName.length()+c.getSimpleName().length()+1)==c.getName().length()){
                entity.put("tableName",StringUtil.Camel2Underline(c.getSimpleName()));
            }else{
                String prefix = c.getName().substring(packageName.length()+1,c.getName().lastIndexOf(".")).replace(".","_");
                entity.put("tableName",prefix+"@"+StringUtil.Camel2Underline(c.getSimpleName()));
            }
            SQLUtil.classTableMap.put(c,entity.getString("tableName"));
            entity.put("className",c.getSimpleName());
            //添加表属性
            Field[] fields = c.getDeclaredFields();
            Field.setAccessible(fields, true);
            JSONArray properties = new JSONArray();
            for (int i = 0; i < fields.length; i++) {
                JSONObject property = new JSONObject();
                //Ignore注解或者成员属性为指定包下面的实体类均要忽略
                boolean ignore = fields[i].getType().getName().contains(packageName)||fields[i].getDeclaredAnnotation(Ignore.class) != null;
                property.put("ignore",ignore);
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

    protected JSONArray getDatabaseInfo(Connection connection) throws SQLException {
        PreparedStatement tablePs = connection.prepareStatement("show tables;");
        ResultSet tableRs = tablePs.executeQuery();
        //(1)获取所有表
        JSONArray entityList = new JSONArray();
        while (tableRs.next()) {
            JSONObject entity = new JSONObject();
            entity.put("tableName",tableRs.getString(1));

            JSONArray properties = new JSONArray();
            PreparedStatement propertyPs = connection.prepareStatement("show columns from `" + tableRs.getString(1)+"`");
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

    public void autoBuildDatabase(String packageName){
        Predicate predicate = null;
        autoBuildDatabase(packageName,predicate);
    }

    public void autoBuildDatabase(String packageName,String regexPattern){
        if(ValidateUtil.isEmpty(regexPattern)){
            throw new IllegalArgumentException("regexPattern不能为空!");
        }
        Pattern pattern = Pattern.compile(regexPattern);
        Predicate<Class> predicate = (c) ->{
            Matcher m = pattern.matcher(c.getName());
            if(m.matches()){
                return false;
            }
            return true;
        };
        logger.info("[根据正则过滤实体类]{}",regexPattern);
        autoBuildDatabase(packageName,predicate);
    }

    public void autoBuildDatabase(String packageName,String[] excludePackageNames){
        if(ValidateUtil.isEmpty(excludePackageNames)){
            throw new IllegalArgumentException("excludePackageNames不能为空!");
        }
        Predicate<Class> predicate = (c) ->{
            boolean result = true;
            for(String excludePackageName:excludePackageNames){
                if(c.getName().contains(excludePackageName)){
                    result = false;
                    break;
                }
            }
            return result;
        };
        logger.info("[根据包名排除]{}", JSON.toJSON(excludePackageNames));
        autoBuildDatabase(packageName,predicate);
    }

    public void autoBuildDatabase(String packageName, Predicate predicate){
        logger.trace("[自动建表开始执行]");
        if(ValidateUtil.isEmpty(packageName)){
            throw new IllegalArgumentException("packageName不能为空!");
        }
        try {
            JSONArray dbEntityList = getDatabaseInfo(dataSource.getConnection());
            logger.debug("[获取数据库信息]{}",dbEntityList.size());
            JSONArray entityList = getEntityInfo(packageName,predicate);
            logger.debug("[获取实体信息]{}",entityList.size());
            Connection connection = dataSource.getConnection();
            connection.setAutoCommit(false);

            //判断表是否存在,存在则判断字段,不存在则创建
            for(int i=0;i<entityList.size();i++){
                //忽略注解为ignore的类
                JSONObject source = entityList.getJSONObject(i);
                String className = source.getString("className");
                if(source.getBoolean("ignore")){
                    logger.debug("[忽略实体类{}]该实体类被Ignore注解修饰!",className);
                    continue;
                }
                String tableName = source.getString("tableName");
                JSONObject target = getValue(dbEntityList,"tableName",tableName);

                List<String> uniqueColumnList = new ArrayList<>();
                if(target==null&&!source.getBoolean("ignore")){
                    //新增数据库表
                    StringBuilder builder = new StringBuilder("create table `"+tableName+"`(");
                    JSONArray properties = source.getJSONArray("properties");
                    for(int j=0;j<properties.size();j++){
                        JSONObject property = properties.getJSONObject(j);
                        if(property.getBoolean("ignore")){
                            continue;
                        }
                        String column = property.getString("column");
                        String columnType = property.containsKey("columnType")?property.getString("columnType"):fieldMapping.get(property.getString("type"));
                        if("id".equals(column)){
                            //主键新增
                            builder.append(column+" "+columnType+" primary key "+getSyntax(Syntax.AutoIncrement));
                        }else{
                            builder.append("`"+column+"` "+columnType);
                            if(property.containsKey("default")){
                                builder.append(" default "+property.getString("default"));
                            }
                            if(property.getBoolean("notNull")){
                                builder.append(" not null ");
                            }

                            if(property.getBoolean("unique")){
                                uniqueColumnList.add(column);
                            }
                        }
                        builder.append(",");
                    }
                    builder.deleteCharAt(builder.length()-1);
                    builder.append(")");
                    String sql = builder.toString().replaceAll("\\s+"," ");
                    logger.debug("[生成新表{}=>{}]执行sql:{}",className,tableName,sql);
                    connection.prepareStatement(sql).executeUpdate();
                }else {
                    //对比字段
                    JSONArray sourceProperties = source.getJSONArray("properties");
                    JSONArray targetProperties = target.getJSONArray("properties");
                    addNewColumn(connection, tableName, uniqueColumnList, sourceProperties, targetProperties);
                }
                if(uniqueColumnList.size()>0){
                    String uniqueSQL = getUniqueStatement(tableName,uniqueColumnList);
                    logger.debug("[为表{}生成唯一约束]执行sql:{}",tableName,uniqueSQL);
                    connection.prepareStatement(uniqueSQL).executeUpdate();
                }
            }
            connection.commit();
            connection.setAutoCommit(true);
            connection.close();
            logger.trace("[自动建表完成]");
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    protected void addNewColumn(Connection connection, String tableName, List<String> uniqueColumnList, JSONArray sourceProperties, JSONArray targetProperties) throws SQLException {
        for (int j = 0; j < sourceProperties.size(); j++) {
            JSONObject sourceProperty = sourceProperties.getJSONObject(j);
            if(sourceProperty.getBoolean("ignore")){
                continue;
            }
            JSONObject targetProperty = getValue(targetProperties, "column", sourceProperty.getString("column"));
            if (targetProperty == null) {
                //新增属性
                String column = sourceProperty.getString("column");
                String columnType = sourceProperty.containsKey("columnType") ? sourceProperty.getString("columnType") : fieldMapping.get(sourceProperty.getString("type"));

                StringBuilder builder = new StringBuilder();
                builder.append("alter table `" + tableName + "` add column " + "`" + column + "` " + columnType+";");
                if (sourceProperty.containsKey("default")) {
                    builder.append(" default " + sourceProperty.getString("default"));
                }
                String sql = builder.toString().replaceAll("\\s+", " ");
                logger.debug("[添加新列]表:{},列名:{},执行SQL:{}",tableName,column+"("+columnType+")",sql);
                connection.prepareStatement(sql).executeUpdate();
                if (sourceProperty.getBoolean("unique")) {
                    uniqueColumnList.add(column);
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
}
