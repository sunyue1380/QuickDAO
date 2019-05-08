package cn.schoolwow.quickdao.condition;

import cn.schoolwow.quickdao.annotation.Ignore;
import cn.schoolwow.quickdao.domain.PageVo;
import cn.schoolwow.quickdao.util.ReflectionUtil;
import cn.schoolwow.quickdao.util.SQLUtil;
import cn.schoolwow.quickdao.util.StringUtil;
import cn.schoolwow.quickdao.util.ValidateUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.*;
import java.lang.reflect.Field;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class AbstractCondition<T> implements Condition<T>, Serializable {
    Logger logger = LoggerFactory.getLogger(AbstractCondition.class);
    protected String distinct = "";
    /**
     * 列名
     */
    protected StringBuilder columnBuilder = new StringBuilder();
    /**
     * 聚合函数
     */
    protected StringBuilder aggerateColumnBuilder = new StringBuilder();
    /**
     * 字段更新
     */
    protected StringBuilder setBuilder = new StringBuilder();
    /**
     * 查询条件构建
     */
    protected StringBuilder whereBuilder = new StringBuilder();
    /**
     * 分组
     */
    protected StringBuilder groupByBuilder = new StringBuilder("group by ");
    /**
     * 分组过滤
     */
    protected StringBuilder havingBuilder = new StringBuilder("having ");
    /**
     * 排序
     */
    protected StringBuilder orderByBuilder = new StringBuilder();
    /**
     * 分页
     */
    protected String limit = "";
    /**
     * 存放查询参数
     */
    protected List parameterList = new ArrayList();
    /**
     * 存在当前待设置下标处
     */
    protected int parameterIndex = 1;
    /**
     * 存放更新参数
     */
    protected List updateParameterList;
    /**
     * 类名
     */
    protected Class<T> _class;
    /**
     * 数据源
     */
    protected transient DataSource dataSource;

    /**
     * 构建sql语句
     */
    protected StringBuilder sqlBuilder = new StringBuilder();
    /**
     * 用于记录sql日志
     */
    protected String sql;

    /**
     * 关联表计数
     */
    private int joinTableIndex = 1;
    /**
     * 关联查询条件
     */
    private List<AbstractSubCondition> subConditionList = new ArrayList<>();
    /**
     * 表名
     */
    protected String tableName = null;
    /**
     * 是否已经完成条件构建
     */
    protected boolean hasDone = false;

    protected PageVo<T> pageVo = null;

    private static String[] patterns = new String[]{"%", "_", "[", "[^", "[!", "]"};

    public AbstractCondition(Class<T> _class, DataSource dataSource) {
        this._class = _class;
        this.tableName = "`" + SQLUtil.classTableMap.get(_class.getName()) + "`";
        this.dataSource = dataSource;
    }

    @Override
    public Condition distinct() {
        distinct = "distinct";
        return this;
    }

    @Override
    public Condition addNullQuery(String field) {
        whereBuilder.append("(t.`" + StringUtil.Camel2Underline(field) + "` is null) and ");
        return this;
    }

    @Override
    public Condition addNotNullQuery(String field) {
        //判断字段是否是String类型
        whereBuilder.append("(t.`" + StringUtil.Camel2Underline(field) + "` is not null) and ");
        return this;
    }

    @Override
    public Condition addNotEmptyQuery(String field) {
        //判断字段是否是String类型
        whereBuilder.append("(t.`" + StringUtil.Camel2Underline(field) + "` is not null and t.`" + StringUtil.Camel2Underline(field) + "` != '') and ");
        return this;
    }

    @Override
    public Condition addInQuery(String field, Object[] values) {
        if (values == null || values.length == 0) {
            return this;
        }
        addINQuery("t", field, values, "in");
        return this;
    }

    @Override
    public Condition addInQuery(String field, List values) {
        return addInQuery(field, values.toArray(new Object[values.size()]));
    }

    @Override
    public Condition addNotInQuery(String field, Object[] values) {
        if (values == null || values.length == 0) {
            return this;
        }
        addINQuery("t", field, values, "not in");
        return this;
    }

    @Override
    public Condition addNotInQuery(String field, List values) {
        return addNotInQuery(field, values.toArray(new Object[values.size()]));
    }

    @Override
    public Condition addQuery(String query) {
        whereBuilder.append("(" + query + ") and ");
        return this;
    }

    @Override
    public Condition addQuery(String field, Object value) {
        if (value == null || value.toString().equals("")) {
            return this;
        }
        if (value instanceof String) {
            addQuery(field, "like", value);
        } else {
            addQuery(field, "=", value);
        }
        return this;
    }

    @Override
    public Condition addQuery(String field, String operator, Object value) {
        if (value instanceof String) {
            whereBuilder.append("(t.`" + StringUtil.Camel2Underline(field) + "` " + operator + " ?) and ");
            boolean hasContains = false;
            for (String pattern : patterns) {
                if (((String) value).contains(pattern)) {
                    parameterList.add(value);
                    hasContains = true;
                    break;
                }
            }
            if (!hasContains) {
                parameterList.add("%" + value + "%");
            }
        } else {
            whereBuilder.append("(t.`" + StringUtil.Camel2Underline(field) + "` " + operator + " ?) and ");
            parameterList.add(value);
        }
        return this;
    }

    @Override
    public Condition addInstanceQuery(Object instance) {
        addInstanceQuery(instance, true);
        return this;
    }

    @Override
    public Condition addInstanceQuery(Object instance, boolean userBasicDataType) {
        Field[] fields = ReflectionUtil.getFields(instance.getClass());
        for (Field field : fields) {
            //判断是否是基本数据类型
            if (field.getType().isPrimitive() && !userBasicDataType) {
                continue;
            }
            try {
                switch (field.getType().getSimpleName().toLowerCase()) {
                    case "int": {
                    }
                    case "integer": {
                        addQuery(field.getName(), field.getInt(instance));
                    }
                    break;
                    case "long": {
                        //排除id为0的情况
                        if (!ReflectionUtil.isIdField(field) || field.getLong(instance) != 0) {
                            addQuery(field.getName(), field.getLong(instance));
                        }
                    }
                    break;
                    case "boolean": {
                        addQuery(field.getName(), field.getBoolean(instance));
                    }
                    break;
                    default: {
                        addQuery(field.getName(), field.get(instance));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return this;
    }

    @Override
    public Condition addQuery(JSONObject queryCondition) {
        Field[] fields = ReflectionUtil.getFields(_class);
        for (Field field : fields) {
            if (queryCondition.containsKey(field.getName())) {
                addQuery(field.getName(), queryCondition.get(field.getName()));
            }
            if (queryCondition.containsKey(field.getName() + "Start")) {
                addQuery(field.getName(), ">=", queryCondition.get(field.getName() + "Start"));
            }
            if (queryCondition.containsKey(field.getName() + "End")) {
                addQuery(field.getName(), "<=", queryCondition.get(field.getName() + "End"));
            }
            if (queryCondition.containsKey(field.getName() + "IN")) {
                addInQuery(field.getName(), queryCondition.getJSONArray(field.getName() + "IN"));
            }
            if (queryCondition.containsKey(field.getName() + "NOTNULL")) {
                addNotNullQuery(field.getName());
            }
            if (queryCondition.containsKey(field.getName() + "NULL")) {
                addNullQuery(field.getName());
            }
            if (queryCondition.containsKey(field.getName() + "NOTEMPTY")) {
                addNotEmptyQuery(field.getName());
            }
        }
        String[] orders = {"_orderBy", "_orderByDesc"};
        for (String order : orders) {
            if (queryCondition.containsKey(order)) {
                if (queryCondition.get(order) instanceof String) {
                    if ("_orderBy".equals(order)) {
                        orderBy(queryCondition.getString(order));
                    } else {
                        orderByDesc(queryCondition.getString(order));
                    }
                } else if (queryCondition.get(order) instanceof String) {
                    JSONArray array = queryCondition.getJSONArray(order);
                    for (int i = 0; i < array.size(); i++) {
                        if ("_orderBy".equals(order)) {
                            orderBy(queryCondition.getString(order));
                        } else {
                            orderByDesc(queryCondition.getString(order));
                        }
                    }
                }
            }
        }
        if (queryCondition.containsKey("_pageNumber") && queryCondition.containsKey("_pageSize")) {
            page(queryCondition.getInteger("_pageNumber"), queryCondition.getInteger("_pageSize"));
        }
        return this;
    }

    @Override
    public Condition addUpdate(String field, Object value) {
        if (updateParameterList == null) {
            updateParameterList = new ArrayList();
        }
        setBuilder.append("t.`" + StringUtil.Camel2Underline(field) + "`=?,");
        updateParameterList.add(value);
        return this;
    }

    @Override
    public Condition addAggerate(String aggerate, String field) {
        field = StringUtil.Camel2Underline(field);
        aggerateColumnBuilder.append(aggerate + "(t.`" + field + "`) as `" + aggerate + "(" + field + ")`,");
        return this;
    }

    @Override
    public Condition addAggerate(String aggerate, String field, String alias) {
        field = StringUtil.Camel2Underline(field);
        aggerateColumnBuilder.append(aggerate + "(t.`" + field + "`) as `" + alias + "`,");
        return this;
    }

    @Override
    public Condition groupBy(String field) {
        groupByBuilder.append("t.`" + StringUtil.Camel2Underline(field) + "`,");
        return this;
    }

//    @Override
//    public Condition having(String query) {
//        havingBuilder.append("("+query+") and ");
//        return this;
//    }

    @Override
    public <T> SubCondition<T> joinTable(Class<T> _class, String primaryField, String joinTableField) {
        String tableNameAlias = "t" + (joinTableIndex++);
        //获取第一个类型为_class的字段名
        Field[] fields = ReflectionUtil.getCompositField(this._class, _class);
        String fieldName = null;
        if (fields != null && fields.length > 0) {
            if (fields.length == 1) {
                fieldName = fields[0].getName();
            } else {
                throw new IllegalArgumentException("类[" + this._class.getName() + "]存在[" + fields.length + "]个类型为[" + _class.getName() + "]的成员变量!");
            }
        }
        SubCondition<T> subCondition = new AbstractSubCondition<T>(_class, tableNameAlias, primaryField, joinTableField, fieldName, this);
        subConditionList.add((AbstractSubCondition) subCondition);
        return subCondition;
    }

    @Override
    public <T> SubCondition<T> joinTable(Class<T> _class, String primaryField, String joinTableField, String compositField) {
        String tableNameAlias = "t" + (joinTableIndex++);
        SubCondition<T> subCondition = new AbstractSubCondition<T>(_class, tableNameAlias, primaryField, joinTableField, compositField, this);
        subConditionList.add((AbstractSubCondition) subCondition);
        return subCondition;
    }

    @Override
    public Condition orderBy(String field) {
        orderByBuilder.append("t.`" + StringUtil.Camel2Underline(field) + "` asc,");
        return this;
    }

    @Override
    public Condition orderByDesc(String field) {
        orderByBuilder.append("t.`" + StringUtil.Camel2Underline(field) + "` desc,");
        return this;
    }

    @Override
    public Condition limit(long offset, long limit) {
        this.limit = "limit " + offset + "," + limit;
        return this;
    }

    @Override
    public Condition page(int pageNum, int pageSize) {
        this.limit = "limit " + (pageNum - 1) * pageSize + "," + pageSize;
        pageVo = new PageVo<>();
        pageVo.setPageSize(pageSize);
        pageVo.setCurrentPage(pageNum);
        return this;
    }

    @Override
    public Condition addColumn(String field) {
        columnBuilder.append("t.`" + StringUtil.Camel2Underline(field) + "`,");
        return this;
    }

    protected Condition done() {
        if (columnBuilder.length() > 0) {
            columnBuilder.deleteCharAt(columnBuilder.length() - 1);
        }
        if (aggerateColumnBuilder.length() > 0) {
            aggerateColumnBuilder.deleteCharAt(aggerateColumnBuilder.length() - 1);
        }
        if (setBuilder.length() > 0) {
            setBuilder.deleteCharAt(setBuilder.length() - 1);
            setBuilder.insert(0, "set ");
        }
        if (whereBuilder.length() > 0) {
            whereBuilder.delete(whereBuilder.length() - 5, whereBuilder.length());
            whereBuilder.insert(0, "where ");
        }
        if ("group by ".equals(groupByBuilder.toString())) {
            groupByBuilder.setLength(0);
        } else {
            groupByBuilder.deleteCharAt(groupByBuilder.length() - 1);
        }
        if ("having ".equals(havingBuilder.toString())) {
            havingBuilder.setLength(0);
        } else {
            havingBuilder.delete(havingBuilder.length() - 5, havingBuilder.length());
        }
        if (orderByBuilder.length() > 0) {
            orderByBuilder.deleteCharAt(orderByBuilder.length() - 1);
            orderByBuilder.insert(0, "order by ");
        }
        //处理所有子查询的where语句
        for (AbstractSubCondition abstractSubCondition : subConditionList) {
            if (abstractSubCondition.whereBuilder.length() > 0) {
                abstractSubCondition.whereBuilder.delete(abstractSubCondition.whereBuilder.length() - 5, abstractSubCondition.whereBuilder.length());
            }
        }

        hasDone = true;
        return this;
    }

    @Override
    public long count() {
        assureDone();
        sqlBuilder.setLength(0);
        sqlBuilder.append("select count(1) from " + tableName + " as t ");
        addJoinTableStatement();
        addWhereStatement();
        sql = sqlBuilder.toString().replaceAll("\\s+", " ");
        long count = -1;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            addMainTableParameters(ps);
            addJoinTableParameters(ps);
            logger.debug("[Count]执行SQL:{}", sql);
            ResultSet resultSet = ps.executeQuery();
            if (resultSet.next()) {
                count = resultSet.getLong(1);
            }
            resultSet.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return count;
    }

    @Override
    public long update() {
        assureDone();
        if (setBuilder.length() == 0) {
            throw new IllegalArgumentException("请先调用addUpdate()函数!");
        }
        if (updateParameterList == null || updateParameterList.size() == 0) {
            throw new IllegalArgumentException("请先调用addUpdate()函数!");
        }
        sqlBuilder.setLength(0);
        sqlBuilder.append("update " + tableName + " as t ");
        addJoinTableStatement();
        sqlBuilder.append(setBuilder.toString());
        addWhereStatement();
        sql = sqlBuilder.toString().replaceAll("\\s+", " ");

        long effect = -1;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);) {
            for (Object parameter : updateParameterList) {
                ps.setObject(parameterIndex++, parameter);
                replaceParameter(parameter);
            }
            addMainTableParameters(ps);
            addJoinTableParameters(ps);
            logger.debug("[Update]执行SQL:{}", sql);
            effect = ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return effect;
    }

    @Override
    public long delete() {
        assureDone();
        sqlBuilder.setLength(0);
        sqlBuilder.append("delete t from " + tableName + " as t ");
        addJoinTableStatement();
        addWhereStatement();
        sql = sqlBuilder.toString().replaceAll("\\s+", " ");

        long effect = -1;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);) {
            addMainTableParameters(ps);
            addJoinTableParameters(ps);
            logger.debug("[Delete]执行SQL:{}", sql);
            effect = ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return effect;
    }

    @Override
    public T getOne() {
        List<T> list = getList();
        if(list==null||list.size()==0){
            return null;
        }else{
            return list.get(0);
        }
    }

    @Override
    public List<T> getList() {
        return getArray().toJavaList(_class);
    }

    @Override
    public JSONArray getArray() {
        assureDone();
        sqlBuilder.setLength(0);
        sqlBuilder.append("select " + distinct + " " + SQLUtil.columns(_class, "t") + " from " + tableName + " as t ");
        addJoinTableStatement();
        addWhereStatement();
        sqlBuilder.append(" " + orderByBuilder.toString() + " " + limit);
        sql = sqlBuilder.toString().replaceAll("\\s+", " ");

        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);) {
            addMainTableParameters(ps);
            addJoinTableParameters(ps);
            logger.debug("[getList]执行SQL:{}", sql);
            ResultSet resultSet = ps.executeQuery();
            JSONArray array = ReflectionUtil.mappingResultSetToJSONArray(resultSet, "t", (int) count());
            ps.close();
            return array;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public PageVo<T> getPagingList() {
        getPageVo();
        pageVo.setList(getList());
        return pageVo;
    }

    @Override
    public PageVo<T> getPagingCompositList() {
        getPageVo();
        pageVo.setList(getCompositList());
        return pageVo;
    }

    @Override
    public List<T> getCompositList() {
        JSONArray array = getCompositArray();
        return array.toJavaList(_class);
    }

    @Override
    public JSONArray getCompositArray() {
        assureDone();
        sqlBuilder.setLength(0);
        sqlBuilder.append("select " + distinct + " " + SQLUtil.columns(_class, "t"));
        for (AbstractSubCondition subCondition : subConditionList) {
            sqlBuilder.append("," + SQLUtil.columns(subCondition._class, subCondition.tableAliasName));
        }
        sqlBuilder.append(" from " + tableName + " as t ");
        addJoinTableStatement();
        addWhereStatement();
        sqlBuilder.append(" " + orderByBuilder.toString() + " " + limit);
        sql = sqlBuilder.toString().replaceAll("\\s+", " ");

        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);) {
            addMainTableParameters(ps);
            addJoinTableParameters(ps);
            logger.debug("[getList]执行SQL:{}", sql);
            ResultSet resultSet = ps.executeQuery();
            JSONArray array = new JSONArray((int) count());

            Field[] fields = ReflectionUtil.getFields(_class);
            while (resultSet.next()) {
                JSONObject o = new JSONObject();
                for (Field field : fields) {
                    if (field.getAnnotation(Ignore.class) != null) {
                        continue;
                    }
                    String columnName = "t_" + StringUtil.Camel2Underline(field.getName());
                    String type = field.getType().getSimpleName().toLowerCase();
                    //根据类型进行映射
                    switch (type) {
                        case "int": {
                            o.put(field.getName(), resultSet.getInt(columnName));
                        }
                        break;
                        case "integer": {
                            o.put(field.getName(), resultSet.getInt(columnName));
                        }
                        break;
                        case "long": {
                            o.put(field.getName(), resultSet.getLong(columnName));
                        }
                        ;
                        break;
                        case "boolean": {
                            o.put(field.getName(), resultSet.getBoolean(columnName));
                        }
                        ;
                        break;
                        case "date": {
                            o.put(field.getName(), resultSet.getDate(columnName));
                        }
                        ;
                        break;
                        default: {
                            o.put(field.getName(), resultSet.getObject(columnName));
                        }
                    }
                }

                for (AbstractCondition.AbstractSubCondition subCondition : subConditionList) {
                    if (ValidateUtil.isEmpty(subCondition.compositField)) {
                        continue;
                    }
                    JSONObject subObject = new JSONObject();
                    Field[] subFields = ReflectionUtil.getFields(subCondition._class);
                    for (Field field : subFields) {
                        if (field.getAnnotation(Ignore.class) != null) {
                            continue;
                        }
                        String columnName = subCondition.tableAliasName + "_" + StringUtil.Camel2Underline(field.getName());
                        String type = field.getType().getSimpleName().toLowerCase();
                        //根据类型进行映射
                        switch (type) {
                            case "int":
                            case "integer": {
                                subObject.put(field.getName(), resultSet.getInt(columnName));
                            }
                            break;
                            case "long": {
                                subObject.put(field.getName(), resultSet.getLong(columnName));
                            }
                            ;
                            break;
                            case "boolean": {
                                subObject.put(field.getName(), resultSet.getBoolean(columnName));
                            }
                            ;
                            break;
//                            case "date":{
//                                subObject.put(plainColumnName,resultSet.getDate(columnName));
//                            };break;
                            default: {
                                subObject.put(field.getName(), resultSet.getObject(columnName));
                            }
                        }
                    }
                    o.put(ReflectionUtil.getCompositField(_class, subCondition._class, subCondition.compositField).getName(), subObject);
                }
                array.add(o);
            }
            resultSet.close();
            return array;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public List<T> getPartList() {
        if (columnBuilder.length() == 0) {
            throw new IllegalArgumentException("请先调用addColumn()函数!");
        }
        assureDone();
        sqlBuilder.setLength(0);
        sqlBuilder.append("select " + distinct + " " + columnBuilder.toString() + " from " + tableName + " as t ");
        addJoinTableStatement();
        addWhereStatement();
        sqlBuilder.append(" " + orderByBuilder.toString() + " " + limit);
        sql = sqlBuilder.toString().replaceAll("\\s+", " ");

        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);) {
            addMainTableParameters(ps);
            addJoinTableParameters(ps);
            logger.debug("[getValueList]执行SQL:{}", sql);
            ResultSet resultSet = ps.executeQuery();
            List<T> instanceList = getArray().toJavaList(_class);
            ps.close();
            return instanceList;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public <E> List<E> getValueList(Class<E> _class, String column) {
        assureDone();
        sqlBuilder.setLength(0);
        sqlBuilder.append("select " + distinct + " " + "t.`" + StringUtil.Camel2Underline(column) + "` from " + tableName + " as t ");
        addJoinTableStatement();
        addWhereStatement();
        sqlBuilder.append(" " + orderByBuilder.toString() + " " + limit);
        sql = sqlBuilder.toString().replaceAll("\\s+", " ");

        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);) {
            addMainTableParameters(ps);
            addJoinTableParameters(ps);
            logger.debug("[getValueList]执行SQL:{}", sql);
            ResultSet resultSet = ps.executeQuery();
            List<E> instanceList = ReflectionUtil.mappingSingleResultToList(resultSet, (int) count(), _class);
            ps.close();
            return instanceList;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public JSONArray getAggerateList() {
        assureDone();
        sqlBuilder.setLength(0);
        sqlBuilder.append("select " + distinct + " ");
        if (columnBuilder.toString().length() > 0) {
            sqlBuilder.append(columnBuilder.toString() + ",");
        }
        sqlBuilder.append(aggerateColumnBuilder.toString() + " from " + tableName + " as t ");
        addJoinTableStatement();
        addWhereStatement();
        sqlBuilder.append(" "+groupByBuilder.toString() + " " + havingBuilder.toString() + " " + orderByBuilder.toString() + " " + limit);
        sql = sqlBuilder.toString().replaceAll("\\s+", " ");

        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);) {
            addMainTableParameters(ps);
            addJoinTableParameters(ps);
            logger.debug("[getList]执行SQL:{}", sql);
            JSONArray array = new JSONArray();
            ResultSet resultSet = ps.executeQuery();
            ResultSetMetaData metaData = resultSet.getMetaData();
            int columnCount = metaData.getColumnCount();
            while (resultSet.next()) {
                JSONObject o = new JSONObject();
                for (int i = 1; i <= columnCount; i++) {
                    o.put(metaData.getColumnName(i).toLowerCase(), resultSet.getString(i));
                }
                array.add(o);
            }
            resultSet.close();
            ps.close();
            connection.close();
            return array;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public AbstractCondition clone() throws CloneNotSupportedException {
        try {
            /* 写入当前对象的二进制流 */
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(this);
            /* 读出二进制流产生的新对象 */
            ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
            ObjectInputStream ois = new ObjectInputStream(bis);
            AbstractCondition abstractCondition = (AbstractCondition) ois.readObject();
            abstractCondition.dataSource = this.dataSource;
            return abstractCondition;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public String toString() {
        return "[Condition]类[" + _class.getName() + "],where子句:[" + whereBuilder.toString() + "],参数列表:[" + parameterList + "],排序:[" + orderByBuilder.toString() + "],分页:[" + limit + "]";
    }

    private PageVo<T> getPageVo() {
        if (pageVo == null) {
            throw new IllegalArgumentException("请先调用page()函数!");
        }
        pageVo.setTotalSize(count());
        pageVo.setTotalPage((int) (pageVo.getTotalSize() / pageVo.getPageSize()) + 1);
        pageVo.setHasMore(pageVo.getCurrentPage() < pageVo.getTotalPage());
        return pageVo;
    }

    protected void addMainTableParameters(PreparedStatement ps) throws SQLException {
        for (Object parameter : parameterList) {
            ps.setObject(parameterIndex, parameter);
            replaceParameter(parameter);
            parameterIndex++;
        }
    }

    /**
     * 添加外键关联查询条件
     */
    protected void addJoinTableStatement() {
        for (AbstractSubCondition abstractSubCondition : subConditionList) {
            sqlBuilder.append("join `" + SQLUtil.classTableMap.get(abstractSubCondition._class.getName()) + "` as " + abstractSubCondition.tableAliasName + " on t." + StringUtil.Camel2Underline(abstractSubCondition.primaryField) + " = " + StringUtil.Camel2Underline(abstractSubCondition.tableAliasName) + "." + StringUtil.Camel2Underline(abstractSubCondition.joinTableField) + " ");
        }
    }

    protected void addWhereStatement() {
        //添加查询条件
        sqlBuilder.append(whereBuilder.toString());
        for (AbstractSubCondition abstractSubCondition : subConditionList) {
            if (abstractSubCondition.whereBuilder.length() > 0) {
                sqlBuilder.append(" and " + abstractSubCondition.whereBuilder.toString() + " ");
            }
        }
    }

    /**
     * 添加外键查询参数
     */
    protected void addJoinTableParameters(PreparedStatement ps) throws SQLException {
        for (AbstractSubCondition abstractSubCondition : subConditionList) {
            for (Object parameter : abstractSubCondition.parameterList) {
                ps.setObject(parameterIndex, parameter);
                replaceParameter(parameter);
                parameterIndex++;
            }
        }
    }

    /**
     * 替换查询参数
     */
    protected void replaceParameter(Object parameter) {
        String type = parameter.getClass().getSimpleName().toLowerCase();
        switch (type) {
            case "int": {
            }
            ;
            case "integer": {
            }
            ;
            case "long": {
            }
            ;
            case "boolean": {
                sql = sql.replaceFirst("\\?", parameter.toString());
            }
            break;
            case "string": {
                sql = sql.replaceFirst("\\?", "'" + parameter.toString() + "'");
            }
            break;
            default: {
                sql = sql.replaceFirst("\\?", parameter.toString());
            }
        }
    }

    /**
     * 确保执行了done方法
     */
    protected void assureDone() {
        if (!hasDone) {
            done();
        }
        parameterIndex = 1;
    }

    private void addINQuery(String tableAliasName, String field, Object[] values, String in) {
        if (values[0] instanceof String) {
            for (int i = 0; i < values.length; i++) {
                //不能加百分号
                values[i] = values[i].toString();
            }
        }
        parameterList.addAll(Arrays.asList(values));
        whereBuilder.append("(" + tableAliasName + "." + StringUtil.Camel2Underline(field) + " " + in + " (");
        for (int i = 0; i < values.length; i++) {
            whereBuilder.append("?,");
        }
        whereBuilder.deleteCharAt(whereBuilder.length() - 1);
        whereBuilder.append(") ) and ");
    }

    class AbstractSubCondition<T> implements SubCondition<T> {
        private Class<T> _class;
        private String tableAliasName;
        private String primaryField;
        private String joinTableField;
        private String compositField;
        private StringBuilder whereBuilder = new StringBuilder();
        private List parameterList = new ArrayList();
        private Condition condition;

        public AbstractSubCondition(Class<T> _class, String tableAliasName, String primaryField, String joinTableField, String compositField, Condition condition) {
            this._class = _class;
            this.tableAliasName = tableAliasName;
            this.primaryField = primaryField;
            this.joinTableField = joinTableField;
            this.compositField = compositField;
            this.condition = condition;
        }

        @Override
        public SubCondition addNullQuery(String field) {
            whereBuilder.append("(" + tableAliasName + ".`" + StringUtil.Camel2Underline(field) + "` is null) and ");
            return this;
        }

        @Override
        public SubCondition addNotNullQuery(String field) {
            whereBuilder.append("(" + tableAliasName + ".`" + StringUtil.Camel2Underline(field) + "` is not null) and ");
            return this;
        }

        @Override
        public SubCondition addNotEmptyQuery(String field) {
            whereBuilder.append("(" + tableAliasName + ".`" + StringUtil.Camel2Underline(field) + "` is not null and " + tableAliasName + ".`" + StringUtil.Camel2Underline(field) + "` != '') and ");
            return this;
        }

        @Override
        public SubCondition addInQuery(String field, Object[] values) {
            if (values == null || values.length == 0) {
                return this;
            }
            addINQuery(tableAliasName, field, values, "in");
            return this;
        }

        @Override
        public SubCondition addInQuery(String field, List values) {
            return addInQuery(field, values.toArray(new Object[values.size()]));
        }

        @Override
        public SubCondition addNotInQuery(String field, Object[] values) {
            if (values == null || values.length == 0) {
                return this;
            }
            addINQuery(tableAliasName, field, values, "not in");
            return this;
        }

        @Override
        public SubCondition addNotInQuery(String field, List values) {
            return addNotInQuery(field, values.toArray(new Object[values.size()]));
        }

        @Override
        public SubCondition addQuery(String query) {
            whereBuilder.append("(" + query + ") and ");
            return this;
        }

        @Override
        public SubCondition addQuery(String property, Object value) {
            if (value == null || value.toString().equals("")) {
                return this;
            }
            if (value instanceof String) {
                addQuery(property, "like", value);
            } else {
                addQuery(property, "=", value);
            }
            return this;
        }

        @Override
        public SubCondition addQuery(String property, String operator, Object value) {
            if (value instanceof String) {
                whereBuilder.append("(" + tableAliasName + ".`" + StringUtil.Camel2Underline(property) + "` " + operator + " ?) and ");
                boolean hasContains = false;
                for (String pattern : patterns) {
                    if (((String) value).contains(pattern)) {
                        parameterList.add(value);
                        hasContains = true;
                        break;
                    }
                }
                if (!hasContains) {
                    parameterList.add("%" + value + "%");

                }
            } else {
                whereBuilder.append("(" + tableAliasName + ".`" + StringUtil.Camel2Underline(property) + "` " + operator + " ?) and ");
                parameterList.add(value);
            }
            return this;
        }

        @Override
        public SubCondition orderBy(String field) {
            orderByBuilder.append(tableAliasName + ".`" + StringUtil.Camel2Underline(field) + "` asc,");
            return this;
        }

        @Override
        public SubCondition orderByDesc(String field) {
            orderByBuilder.append(tableAliasName + ".`" + StringUtil.Camel2Underline(field) + "` desc,");
            return this;
        }

        @Override
        public Condition done() {
            return this.condition;
        }
    }
}
