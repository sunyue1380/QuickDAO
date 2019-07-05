package cn.schoolwow.quickdao.dao;

import cn.schoolwow.quickdao.condition.AbstractCondition;
import cn.schoolwow.quickdao.condition.Condition;
import cn.schoolwow.quickdao.condition.SqliteCondition;
import cn.schoolwow.quickdao.domain.Entity;
import cn.schoolwow.quickdao.domain.Property;
import cn.schoolwow.quickdao.util.QuickDAOConfig;
import cn.schoolwow.quickdao.util.ReflectionUtil;
import cn.schoolwow.quickdao.util.SQLUtil;
import cn.schoolwow.quickdao.util.ValidateUtil;
import com.alibaba.fastjson.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

public abstract class AbstractDAO implements DAO {
    Logger logger = LoggerFactory.getLogger(AbstractDAO.class);
    /**字段映射*/
    protected Map<String, String> fieldMapping = new HashMap<String, String>();
    protected DataSource dataSource;
    private Connection connection;
    /**是否开启事务*/
    public boolean startTranscation = false;

    public AbstractDAO(DataSource dataSource) {
        this.dataSource = dataSource;
        fieldMapping.put("string", "VARCHAR(255)");
        fieldMapping.put("boolean", "BOOLEAN");
        fieldMapping.put("byte", "TINYINT");
        fieldMapping.put("char", "char(4)");
        fieldMapping.put("short", "SMALLINT");
        fieldMapping.put("int", "INTEGER");
        fieldMapping.put("integer", "INTEGER(11)");
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
    public boolean exist(Object instance){
        try {
            //有id则一定存在
            if(ReflectionUtil.hasId(instance)){
                return true;
            }
            //有唯一性约束则根据唯一性约束查询
            Condition condition = getUniqueCondition(instance);
            if(condition==null){
                return false;
            }
            return condition.count()>0;
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public <T> T fetch(Class<T> _class, long id){
        String property = ReflectionUtil.entityMap.get(_class.getName()).id.name;
        return fetch(_class, property, id);
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
            List<T> instanceList = ReflectionUtil.mappingResultSetToJSONArray(resultSet,count).toJavaList(_class);
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
            return new SqliteCondition(_class,dataSource,this);
        }else{
            return new AbstractCondition<>(_class, dataSource,this);
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

            Entity entity = ReflectionUtil.entityMap.get(_class.getName());
            if(exist(instance)) {
                if(entity.uniqueKeyProperties!=null&&entity.uniqueKeyProperties.length+1!=entity.properties.length){
                    //根据唯一性约束更新
                    String updateByUniqueKey = SQLUtil.updateByUniqueKey(_class);
                    ps = connection.prepareStatement(updateByUniqueKey);
                    logger.debug("[根据唯一性约束更新]执行SQL:{}",ReflectionUtil.setValueWithUpdateByUniqueKey(ps,instance,updateByUniqueKey));
                    effect = ps.executeUpdate();
                    //获取id并设置
                    Condition condition = getUniqueCondition(instance);
                    List<Long> ids = condition.getValueList(Long.class,entity.id.name);
                    if(ids.size()>0){
                        entity.id.field.setLong(instance,ids.get(0));
                    }
                }else if(ReflectionUtil.hasId(instance)){
                    //根据id更新
                    String updateById = SQLUtil.updateById(_class);
                    ps = connection.prepareStatement(updateById);
                    logger.debug("[根据id更新]执行SQL:{}", ReflectionUtil.setValueWithUpdateById(ps, instance, updateById));
                    effect = ps.executeUpdate();
                }
            }else{
                //执行insertIgnore
                String insertIgnore = SQLUtil.insertIgnore(_class,getSyntax(Syntax.InsertIgnore));
                ps = connection.prepareStatement(insertIgnore,PreparedStatement.RETURN_GENERATED_KEYS);
                logger.debug("[执行插入操作]执行SQL:{}",ReflectionUtil.setValueWithInsertIgnore(ps,instance,insertIgnore));
                effect = ps.executeUpdate();
                if(effect>0){
                    //获取主键
                    ResultSet rs = ps.getGeneratedKeys();
                    if(rs.next()){
                        long id = rs.getLong(1);
                        entity.id.field.setLong(instance,id);
                    }
                    rs.close();
                }
            }
            if(ps!=null){
                ps.close();
            }
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

            Class _class = instanceList.get(0).getClass();
            Entity entity = ReflectionUtil.entityMap.get(_class.getName());

            String updateByUniqueKey = SQLUtil.updateByUniqueKey(_class);
            PreparedStatement _updateByUniqueKeyPs = null;
            if(entity.uniqueKeyProperties!=null&&entity.uniqueKeyProperties.length+1!=entity.properties.length){
                //如果有则获取对应语句
                logger.debug("[根据唯一性约束更新]SQL语句:{}",updateByUniqueKey);
                _updateByUniqueKeyPs = connection.prepareStatement(updateByUniqueKey);
            }
            PreparedStatement updateByUniqueKeyPs = _updateByUniqueKeyPs;

            String updateById = SQLUtil.updateById(instanceList.get(0).getClass());
            PreparedStatement updateByIdPs = connection.prepareStatement(updateById);
            String insertIgnore = SQLUtil.insertIgnore(instanceList.get(0).getClass(),getSyntax(Syntax.InsertIgnore));
            PreparedStatement insertIgnorePs = connection.prepareStatement(insertIgnore);
            //根据每个实体类具体情况插入
            for(Object instance:instanceList){
                try {
                    if(exist(instance)){
                        if (_updateByUniqueKeyPs!=null) {
                            //如果有唯一性约束则以唯一性约束更新
                            logger.debug("[根据唯一性约束更新]执行SQL:{}",ReflectionUtil.setValueWithUpdateByUniqueKey(updateByUniqueKeyPs,instance,updateByUniqueKey));
                            updateByUniqueKeyPs.addBatch();
                        }else if(ReflectionUtil.hasId(instance)){
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
            }
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
        String field = ReflectionUtil.entityMap.get(_class.getName()).id.name;
        return delete(_class, field, id);
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
            String sql = "delete from `" + ReflectionUtil.entityMap.get(_class.getName()).tableName+"`";
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
    @Override
    public void startTransaction(){
        startTranscation = true;
        try {
            connection = getConnection();
        } catch (SQLException e) {
            e.printStackTrace();
            try {
                connection.close();
            } catch (SQLException e1) {
                e1.printStackTrace();
            }
        }
    }

    /**设置保存点*/
    @Override
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
    @Override
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
    @Override
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
    @Override
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
    @Override
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

    /**建表*/
    @Override
    public void create(Class _class){
        Entity entity = ReflectionUtil.entityMap.get(_class.getName());
        try {
            Connection connection = dataSource.getConnection();
            createTable(entity,connection);
            connection.close();
        }catch (SQLException e){
            e.printStackTrace();
        }
    }

    /**删表*/
    @Override
    public void drop(Class _class){
        Entity entity = ReflectionUtil.entityMap.get(_class.getName());
        String sql = "drop table if exists `"+entity.tableName+"`;";
        logger.debug("[删除表=>{}]执行SQL:{}",_class.getSimpleName(),sql);
        Connection connection = null;
        try {
            connection = getConnection();
            connection.prepareStatement(sql).execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }finally {
            try {
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    /**删表*/
    @Override
    public void rebuild(Class _class){
        drop(_class);
        create(_class);
    }

    /**仅供Condition类调用*/
    public Connection getConnection() throws SQLException {
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

    /**获取实例的唯一性约束查询条件*/
    private <T> Condition<T> getUniqueCondition(Object instance) throws IllegalAccessException {
        Property[] properties = ReflectionUtil.entityMap.get(instance.getClass().getName()).uniqueKeyProperties;
        if(properties==null||properties.length==0){
            return null;
        }
        Condition condition = query(instance.getClass());
        for(Property property:properties){
            condition.addQuery(property.name,property.field.get(instance));
        }
        return condition;
    }

    /**获取数据库信息*/
    protected Entity[] getDatabaseInfo() throws SQLException {
        Connection connection = dataSource.getConnection();
        connection.setAutoCommit(false);
        PreparedStatement tablePs = connection.prepareStatement("show tables;");
        ResultSet tableRs = tablePs.executeQuery();
        List<Entity> entityList = new ArrayList<>();
        while (tableRs.next()) {
            Entity entity = new Entity();
            entity.tableName = tableRs.getString(1);

            List<Property> propertyList = new ArrayList<>();
            PreparedStatement propertyPs = connection.prepareStatement("show columns from `" + tableRs.getString(1)+"`");
            ResultSet propertiesRs = propertyPs.executeQuery();
            while (propertiesRs.next()) {
                Property property = new Property();
                property.column = propertiesRs.getString("Field");
                property.columnType = propertiesRs.getString("Type");
                property.notNull = "NO".equals(propertiesRs.getString("Null"));
                property.unique = "UNI".equals(propertiesRs.getString("Key"));
                if(null != propertiesRs.getString("Default")){
                    property.defaultValue = propertiesRs.getString("Default");
                }
                propertyList.add(property);
            }
            entity.properties = propertyList.toArray(new Property[0]);
            entityList.add(entity);
            propertiesRs.close();
            propertyPs.close();
        }
        tableRs.close();
        connection.close();
        return entityList.toArray(new Entity[0]);
    }

    public void autoBuildDatabase() throws SQLException {
        if(!QuickDAOConfig.autoCreateTable){
            return;
        }
        Collection<Entity> entityList = ReflectionUtil.entityMap.values();
        for(Entity entity:entityList){
            Property[] properties = entity.properties;
            for(Property property:properties){
                if(property.columnType==null){
                    property.columnType = fieldMapping.get(property.type);
                }
            }
        }
        //对比实体类信息与数据库信息
        Entity[] dbEntityList = getDatabaseInfo();
        logger.debug("[获取数据库信息]数据库表个数:{}",dbEntityList.length);

        Connection connection = dataSource.getConnection();
        connection.setAutoCommit(false);
        for(Entity entity:entityList){
            boolean entityExist = false;
            for(Entity dbEntity:dbEntityList){
                if(entity.tableName.equals(dbEntity.tableName)){
                    //对比字段
                    compareEntityDatabase(entity,dbEntity,connection);
                    entityExist = true;
                    break;
                }
            }
            if(!entityExist){
                //新增数据库表
                createTable(entity,connection);
            }
        }
        if(QuickDAOConfig.openForeignKey){
            createForeignKey(entityList,connection);
        }
        connection.commit();
        connection.setAutoCommit(true);
        connection.close();
    }

    /**创建新表*/
    protected void createTable(Entity entity,Connection connection) throws SQLException {
        StringBuilder createTableBuilder = new StringBuilder("create table if not exists `" + entity.tableName + "`(");
        Property[] properties = entity.properties;
        for(Property property:properties){
            createTableBuilder.append("`" + property.column + "` " + property.columnType);
            if(property.id){
                createTableBuilder.append(" primary key " + getSyntax(Syntax.AutoIncrement));
            }else{
                if(property.defaultValue!=null){
                    createTableBuilder.append(" default '" + property.defaultValue+"'");
                }
                if(property.notNull){
                    createTableBuilder.append(" not null ");
                }
            }
            if(null!=property.comment){
                createTableBuilder.append(" " + getSyntax(Syntax.Comment, property.comment));
            }
            createTableBuilder.append(",");
        }
        createTableBuilder.deleteCharAt(createTableBuilder.length() - 1);
        createTableBuilder.append(")");
        if(null!=entity.comment){
            createTableBuilder.append("comment='"+entity.comment+"';");
        }
        String sql = createTableBuilder.toString().replaceAll("\\s+", " ");
        logger.debug("[生成新表]类名:{},表名:{},执行SQL:{},", entity.className, entity.tableName, sql);
        connection.prepareStatement(sql).executeUpdate();
        createUniqueKey(entity,connection);
    }

    /**对比实体类和数据表*/
    protected void compareEntityDatabase(Entity entity,Entity dbEntity,Connection connection) throws SQLException {
        Property[] entityProperties = entity.properties;
        Property[] dbEntityProperties = dbEntity.properties;
        for(Property entityProperty:entityProperties){
            boolean columnExist = false;
            for(Property dbEntityProperty:dbEntityProperties){
                if(dbEntityProperty.column.equals(entityProperty.column)){
                    columnExist = true;
                    break;
                }
            }
            if(!columnExist){
                StringBuilder addColumnBuilder = new StringBuilder();
                addColumnBuilder.append("alter table `" + entity.tableName + "` add column " + "`" + entityProperty.column + "` " + entityProperty.columnType+" ");
                if(null!=entityProperty.defaultValue){
                    addColumnBuilder.append(" default " + entityProperty.defaultValue);
                }
                if(null!=entityProperty.comment){
                    addColumnBuilder.append(" "+getSyntax(Syntax.Comment,entityProperty.comment));
                }
                if(null!=entityProperty.foreignKey){
                    addColumnBuilder.append(",constraint `"+entityProperty.foreignKeyName+"` foreign key(`"+entityProperty.column+"`) references "+entityProperty.foreignKey);
                }
                addColumnBuilder.append(";");
                String sql = addColumnBuilder.toString().replaceAll("\\s+", " ");
                logger.debug("[添加新列]表:{},列名:{},执行SQL:{}",entity.tableName,entityProperty.column+"("+entityProperty.columnType+")",sql);
                connection.prepareStatement(sql).executeUpdate();
                if(entityProperty.unique){
                    createUniqueKey(entity,connection);
                }
            }
        }
    }

    /**创建唯一索引*/
    protected void createUniqueKey(Entity entity,Connection connection) throws SQLException {
        if(null==entity.uniqueKeyProperties||entity.uniqueKeyProperties.length==0){
            return;
        }
        StringBuilder uniqueKeyBuilder = new StringBuilder("alter table `"+entity.tableName+"` add unique index `"+entity.tableName+"_unique_index` (");
        for(Property property:entity.uniqueKeyProperties){
            uniqueKeyBuilder.append("`"+property.column+"`,");
        }
        uniqueKeyBuilder.deleteCharAt(uniqueKeyBuilder.length()-1);
        uniqueKeyBuilder.append(");");
        String uniqueKeySQL = uniqueKeyBuilder.toString().replaceAll("\\s+", " ");
        logger.debug("[添加唯一性约束]表:{},执行SQL:{}",entity.tableName,uniqueKeySQL);
        connection.prepareStatement(uniqueKeySQL).executeUpdate();
    }

    /**创建外键约束*/
    protected void createForeignKey(Collection<Entity> entityList,Connection connection) throws SQLException {
        for(Entity entity:entityList){
            Property[] foreignKeyProperties = entity.foreignKeyProperties;
            for(Property property:foreignKeyProperties){
                ResultSet resultSet = connection.prepareStatement("SELECT count(1) FROM information_schema.KEY_COLUMN_USAGE WHERE CONSTRAINT_NAME='"+property.foreignKeyName+"'").executeQuery();
                if(resultSet.next()){
                    if(resultSet.getInt(1)==0){
                        String foreignKeySQL = "alter table `"+entity.tableName+"` add constraint `"+property.foreignKeyName+"` foreign key(`"+property.column+"`) references "+property.foreignKey;
                        logger.info("[生成外键约束]约束名:{},执行SQL:{}",property.foreignKeyName,foreignKeySQL);
                        connection.prepareStatement(foreignKeySQL).executeUpdate();
                    }
                }
                resultSet.close();
            }
        }
    }
}
