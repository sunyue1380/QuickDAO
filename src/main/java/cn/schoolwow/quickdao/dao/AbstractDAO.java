package cn.schoolwow.quickdao.dao;

import cn.schoolwow.quickdao.QuickDAO;
import cn.schoolwow.quickdao.annotation.*;
import cn.schoolwow.quickdao.condition.AbstractCondition;
import cn.schoolwow.quickdao.condition.Condition;
import cn.schoolwow.quickdao.condition.SqliteCondition;
import cn.schoolwow.quickdao.domain.QuickDAOConfig;
import cn.schoolwow.quickdao.util.ReflectionUtil;
import cn.schoolwow.quickdao.util.SQLUtil;
import cn.schoolwow.quickdao.util.StringUtil;
import cn.schoolwow.quickdao.util.ValidateUtil;
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
import java.sql.*;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class AbstractDAO implements DAO {
    Logger logger = LoggerFactory.getLogger(AbstractDAO.class);
    /**字段映射*/
    protected Map<String, String> fieldMapping = new HashMap<String, String>();
    protected DataSource dataSource;
    private Connection connection;
    private boolean startTranscation = false;

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
        InsertIgnore,
        Comment;
    }

    /**提取各个数据库产品的SQL差异部分的语法*/
    protected abstract String getSyntax(Syntax syntax,Object... values);

    @Override
    public <T> T fetch(Class<T> _class, long id){
        String name = ReflectionUtil.getId(_class).getName();
        return fetch(_class, name, id);
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
            PreparedStatement ps = null;
            int count = -1;
            if(value==null){
                String fetchNullSQL = SQLUtil.fetchNull(_class,property);
                logger.debug("[根据属性{}=>{}获取对象]执行sql:{}",property,value,fetchNullSQL);
                ps = connection.prepareStatement(fetchNullSQL);
                count = (int) query(_class).addNullQuery(property).count();
            }else{
                String fetchSQL = SQLUtil.fetch(_class, property);
                logger.debug("[根据属性{}=>{}获取对象]执行sql:{}",property,value,fetchSQL.replace("?",value.toString()));
                ps = connection.prepareStatement(fetchSQL);
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
                        ps.setString(1, value.toString());
                    }
                    break;
                    default: {
                        ps.setObject(1, value);
                    }
                }
                count = (int) query(_class).addQuery(property,value).count();
            }
            ResultSet resultSet = ps.executeQuery();
            List<T> instanceList = ReflectionUtil.mappingResultSetToJSONArray(resultSet,"t",count).toJavaList(_class);
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
        if(this instanceof SQLiteDAO||this instanceof H2DAO){
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
            Connection connection = getConnection();
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
                    List<Long> ids = condition.getValueList(Long.class,ReflectionUtil.getId(_class).getName());
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
                    if(rs.next()){
                        long id = rs.getLong(1);
                        ReflectionUtil.setId(instance,id);
                    }
                    rs.close();
                }
            }
            ps.close();
            if(!startTranscation){
                connection.close();
            }
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
            Connection connection = getConnection();
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
            if(!startTranscation){
                connection.commit();
                connection.close();
            }
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
        String name = ReflectionUtil.getId(_class).getName();
        return delete(_class, name, id);
    }

    @Override
    public long delete(Class _class, String field, Object value){
        try {
            Connection connection = getConnection();
            String deleteSQL = SQLUtil.delete(_class, field);
            PreparedStatement ps = connection.prepareStatement(deleteSQL);
            ps.setObject(1, value);
            logger.debug("[根据属性{}=>{}删除]执行SQL:{}",field,value,deleteSQL.replace("?",value.toString()));
            long effect = ps.executeUpdate();
            ps.close();
            if(!startTranscation){
                connection.close();
            }
            return effect;
        }catch (Exception e){
            e.printStackTrace();
            return -1;
        }
    }

    @Override
    public long clear(Class _class){
        try {
            Connection connection = getConnection();
            String sql = "delete from `" + SQLUtil.classTableMap.get(_class.getName())+"`";
            logger.debug("[删除{}表]执行SQL:{}", _class.getSimpleName(),sql);
            PreparedStatement ps = connection.prepareStatement(sql);
            long effect = ps.executeUpdate();
            ps.close();
            if(!startTranscation){
                connection.close();
            }
            return effect;
        }catch (Exception e){
            e.printStackTrace();
            return -1;
        }
    }

    /**开启事务*/
    public void startTransaction(){
        startTranscation = true;
    }

    /**设置保存点*/
    public Savepoint setSavePoint(String name){
        if(connection==null){
            return null;
        }
        try {
            return connection.setSavepoint(name);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**事务回滚*/
    public void rollback(){
        if(connection==null){
            return;
        }
        try {
            connection.rollback();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**事务回滚*/
    public void rollback(Savepoint savePoint){
        if(connection==null){
            return;
        }
        try {
            connection.rollback(savePoint);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**事务提交*/
    public void commit(){
        if(connection==null){
            return;
        }
        try {
            connection.commit();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**结束事务*/
    public void endTransaction(){
        startTranscation = false;
        if(connection==null){
            logger.warn("数据库事务连接为空!不做任何操作!");
            return;
        }
        try {
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }finally {
            connection = null;
        }
    }

    private Connection getConnection() throws SQLException {
        //开启事务时使用同一Connection,不开启事务时从线程池中获取
        if(startTranscation){
            synchronized (this){
                if(connection==null){
                    connection = dataSource.getConnection();
                    connection.setAutoCommit(false);
                }
            }
            return connection;
        }else{
            return dataSource.getConnection();
        }
    }

    /**获取实体类信息同时过滤*/
    protected JSONArray getEntityInfo() throws IOException, ClassNotFoundException {
        Set<String> keySet = QuickDAOConfig.packageNameMap.keySet();
        JSONArray entityList = new JSONArray();
        for(String packageName:keySet){
            List<Class> classList = new ArrayList<>();
            String packageNamePath = packageName.replace(".", "/");
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            URL url = classLoader.getResource(packageNamePath);
            if(url==null){
                throw new IllegalArgumentException("无法识别的包路径:"+packageNamePath);
            }

            if("file".equals(url.getProtocol())){
                File file = new File(url.getFile());
                //TODO 对于有空格或者中文路径会无法识别
                logger.info("[类文件路径]{}",file.getAbsolutePath());
                if(!file.isDirectory()){
                    throw new IllegalArgumentException("包名不是合法的文件夹!");
                }
                Stack<File> stack = new Stack<>();
                stack.push(file);

                String indexOfString = packageName.replace(".","/");
                while(!stack.isEmpty()){
                    file = stack.pop();
                    for(File f:file.listFiles()){
                        if(f.isDirectory()){
                            stack.push(f);
                        }else if(f.isFile()&&f.getName().endsWith(".class")){
                            String path = f.getAbsolutePath().replace("\\","/");
                            int startIndex = path.indexOf(indexOfString);
                            String className = path.substring(startIndex,path.length()-6).replace("/",".");
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
                logger.warn("[扫描实体类信息为空]前缀:{},包名:{}",QuickDAOConfig.packageNameMap.get(packageName),packageName);
                continue;
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

            for (Class c : classList) {
                String tableName = null;
                if((packageName.length()+c.getSimpleName().length()+1)==c.getName().length()){
                    //支持实体包多个文件夹
                    tableName = StringUtil.Camel2Underline(c.getSimpleName());
                }else{
                    String prefix = c.getName().substring(packageName.length()+1,c.getName().lastIndexOf(".")).replace(".","_");
                    tableName = prefix+"@"+StringUtil.Camel2Underline(c.getSimpleName());
                }
                SQLUtil.classTableMap.put(c.getName(),QuickDAOConfig.packageNameMap.get(packageName)+tableName);
            }
            for (Class c : classList) {
                JSONObject entity = new JSONObject();
                entity.put("ignore", c.getDeclaredAnnotation(Ignore.class) != null);
                if(entity.getBoolean("ignore")){
                    logger.debug("[忽略实体类=>{}]该类被@Ignore注解修饰,将跳过该实体类!",c.getName());
                    continue;
                }
                entity.put("tableName",SQLUtil.classTableMap.get(c.getName()));
                entity.put("className",c.getSimpleName());
                Field[] fields = ReflectionUtil.getFields(c);
                JSONArray properties = new JSONArray();
                JSONArray uniqueKeyProperties = new JSONArray();
                JSONArray foreignKeyProperties = new JSONArray();
                for (int i = 0; i < fields.length; i++) {
                    JSONObject property = new JSONObject();
                    //Ignore注解或者成员属性为指定包下面的实体类均要忽略
                    boolean ignore = fields[i].getType().getName().contains(packageName)||fields[i].getDeclaredAnnotation(Ignore.class) != null;
                    property.put("ignore",ignore);
                    property.put("column", StringUtil.Camel2Underline(fields[i].getName()));
                    property.put("name", fields[i].getName());
                    property.put("type", fields[i].getType().getSimpleName().toLowerCase());
                    property.put("unique", fields[i].getDeclaredAnnotation(Unique.class) != null);
                    property.put("notNull", fields[i].getDeclaredAnnotation(NotNull.class) != null);
                    property.put("id", fields[i].getDeclaredAnnotation(Id.class) != null||"id".equals(property.getString("column")));
                    if(property.getBoolean("id")){
                        property.put("notNull", true);
                        fields[i].setAccessible(true);
                        ReflectionUtil.idCache.put(c.getName(),fields[i]);
                    }
                    if (fields[i].getDeclaredAnnotation(ColumnType.class) != null) {
                        property.put("columnType", fields[i].getDeclaredAnnotation(ColumnType.class).value());
                    }else{
                        property.put("columnType", fieldMapping.get(property.getString("type")));
                    }
                    if (fields[i].getDeclaredAnnotation(DefaultValue.class) != null) {
                        property.put("default", fields[i].getDeclaredAnnotation(DefaultValue.class).value());
                    }
                    property.put("comment","");
                    if(fields[i].getDeclaredAnnotation(Comment.class)!=null){
                        property.put("comment",fields[i].getDeclaredAnnotation(Comment.class).value());
                    }
                    if(property.getBoolean("unique")){
                        uniqueKeyProperties.add(property.getString("column"));
                    }
                    ForeignKey foreignKey = fields[i].getDeclaredAnnotation(ForeignKey.class);
                    if(foreignKey!=null){
                        String operation = foreignKey.foreignKeyOption().getOperation();
                        property.put("foreignKey","`"+SQLUtil.classTableMap.get(foreignKey.table().getName())+"`(`"+foreignKey.field()+"`) ON DELETE "+operation+" ON UPDATE "+operation);
                        property.put("foreignKeyName","FK_"+entity.getString("tableName")+"_"+foreignKey.field()+"_"+SQLUtil.classTableMap.get(foreignKey.table().getName())+"_"+property.getString("name"));
                        foreignKeyProperties.add(property);
                    }
                    properties.add(property);
                }
                entity.put("properties", properties);
                entity.put("uniqueKeyProperties", uniqueKeyProperties);
                entity.put("foreignKeyProperties", foreignKeyProperties);
                entityList.add(entity);
            }
        }

        logger.debug("[获取实体信息]实体类个数:{}",entityList.size());
        return entityList;
    }

    /**获取数据库信息*/
    protected JSONArray getDatabaseInfo() throws SQLException {
        Connection connection = dataSource.getConnection();
        connection.setAutoCommit(false);
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

    public void autoBuildDatabase(){
        try {
            JSONArray entityList = getEntityInfo();
            logger.debug("[获取实体信息]{}",entityList.size());
            JSONArray dbEntityList = getDatabaseInfo();
            logger.debug("[获取数据库信息]数据库表个数:{}",dbEntityList.size());

            Connection connection = dataSource.getConnection();
            connection.setAutoCommit(false);
            for (int i = 0; i < entityList.size(); i++) {
                JSONObject entity = entityList.getJSONObject(i);
                String tableName = entity.getString("tableName");
                JSONObject dbEntity = getValue(dbEntityList,"tableName",tableName);
                if (dbEntity == null) {
                    //新增数据库表
                    createTable(entity,connection);
                } else {
                    //对比字段
                    compareEntityDatabase(entity,dbEntity,connection);
                }
            }
            if(QuickDAOConfig.openForeignKey){
                createForeignKey(entityList,connection);
            }
            connection.commit();
            connection.setAutoCommit(true);
            connection.close();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    /**创建新表*/
    protected void createTable(JSONObject entity,Connection connection) throws SQLException {
        String tableName = entity.getString("tableName");
        StringBuilder createTableBuilder = new StringBuilder("create table `" + tableName + "`(");
        JSONArray properties = entity.getJSONArray("properties");
        for (int j = 0; j < properties.size(); j++) {
            JSONObject property = properties.getJSONObject(j);
            if (property.getBoolean("ignore")) {
                continue;
            }
            createTableBuilder.append("`" + property.getString("column") + "` " + property.getString("columnType"));
            if (property.getBoolean("id")) {
                //主键新增
                createTableBuilder.append(" primary key " + getSyntax(Syntax.AutoIncrement));
            } else {
                if (property.containsKey("default")) {
                    createTableBuilder.append(" default " + property.getString("default"));
                }
                if (property.getBoolean("notNull")) {
                    createTableBuilder.append(" not null ");
                }
            }
            createTableBuilder.append(" " + getSyntax(Syntax.Comment, property.getString("comment")));
            createTableBuilder.append(",");
        }
        createTableBuilder.deleteCharAt(createTableBuilder.length() - 1);
        createTableBuilder.append(")");
        String sql = createTableBuilder.toString().replaceAll("\\s+", " ");
        logger.debug("[生成新表{}=>{}]执行sql:{}", entity.getString("className"), tableName, sql);
        connection.prepareStatement(sql).executeUpdate();
        createUniqueKey(entity,connection);
    }

    /**对比实体类和数据表*/
    protected void compareEntityDatabase(JSONObject entity,JSONObject dbEntity,Connection connection) throws SQLException {
        String tableName = entity.getString("tableName");
        JSONArray sourceProperties = entity.getJSONArray("properties");
        JSONArray dbEntityProperties = dbEntity.getJSONArray("properties");
        for (int j = 0; j < sourceProperties.size(); j++) {
            JSONObject sourceProperty = sourceProperties.getJSONObject(j);
            if(sourceProperty.getBoolean("ignore")){
                continue;
            }
            String column = sourceProperty.getString("column");
            JSONObject dbProperty = getValue(dbEntityProperties,"column",column);
            if (dbProperty == null) {
                String columnType = sourceProperty.getString("columnType");

                StringBuilder addColumnBuilder = new StringBuilder();
                addColumnBuilder.append("alter table `" + tableName + "` add column " + "`" + column + "` " + columnType+" ");
                if (sourceProperty.containsKey("default")) {
                    addColumnBuilder.append(" default " + sourceProperty.getString("default"));
                }
                addColumnBuilder.append(" "+getSyntax(Syntax.Comment,sourceProperty.getString("comment")));
                String foreignKey = sourceProperty.getString("foreignKey");
                if(foreignKey!=null){
                    addColumnBuilder.append(",constraint `"+sourceProperty.containsKey("foreignKeyName")+"` foreign key(`"+column+"`) references "+foreignKey);
                }
                addColumnBuilder.append(";");
                String sql = addColumnBuilder.toString().replaceAll("\\s+", " ");
                logger.debug("[添加新列]表:{},列名:{},执行SQL:{}",tableName,column+"("+columnType+")",sql);
                connection.prepareStatement(sql).executeUpdate();
                if(sourceProperty.getBoolean("unique")){
                    createUniqueKey(entity,connection);
                }
            }
        }
    }

    /**创建唯一索引*/
    protected void createUniqueKey(JSONObject entity,Connection connection) throws SQLException {
        String tableName = entity.getString("tableName");
        JSONArray uniqueKeyProperties = entity.getJSONArray("uniqueKeyProperties");
        if(uniqueKeyProperties.size()==0){
            return;
        }
        StringBuilder uniqueKeyBuilder = new StringBuilder("alter table `"+tableName+"` add unique index `"+tableName+"_unique_index` (");
        for(int i=0;i<uniqueKeyProperties.size();i++){
            uniqueKeyBuilder.append("`"+uniqueKeyProperties.getString(i)+"`,");
        }
        uniqueKeyBuilder.deleteCharAt(uniqueKeyBuilder.length()-1);
        uniqueKeyBuilder.append(");");
        String uniqueKeySQL = uniqueKeyBuilder.toString().replaceAll("\\s+", " ");
        logger.debug("[添加唯一性约束]表:{},执行SQL:{}",tableName,uniqueKeySQL);
        connection.prepareStatement(uniqueKeySQL).executeUpdate();
    }

    /**创建外键约束*/
    protected void createForeignKey(JSONArray entityList,Connection connection) throws SQLException {
        for(int i=0;i<entityList.size();i++) {
            JSONObject source = entityList.getJSONObject(i);
            JSONArray foreignKeyProperties = source.getJSONArray("foreignKeyProperties");
            for(int j=0;j<foreignKeyProperties.size();j++){
                JSONObject property = foreignKeyProperties.getJSONObject(j);
                String foreignKeyName = property.getString("foreignKeyName");
                ResultSet resultSet = connection.prepareStatement("SELECT count(1) FROM information_schema.KEY_COLUMN_USAGE WHERE CONSTRAINT_NAME='"+foreignKeyName+"'").executeQuery();
                if(resultSet.next()){
                    if(resultSet.getInt(1)==0){
                        String foreignKeySQL = "alter table `"+source.getString("tableName")+"` add constraint `"+foreignKeyName+"` foreign key(`"+property.getString("column")+"`) references "+property.getString("foreignKey");
                        logger.info("[生成外键约束=>{}]执行SQL:{}",foreignKeyName,foreignKeySQL);
                        connection.prepareStatement(foreignKeySQL).executeUpdate();
                    }
                }
                resultSet.close();
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
