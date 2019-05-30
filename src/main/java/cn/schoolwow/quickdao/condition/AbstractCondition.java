package cn.schoolwow.quickdao.condition;

import cn.schoolwow.quickdao.annotation.Ignore;
import cn.schoolwow.quickdao.dao.AbstractDAO;
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
     * 参数索引
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
    /**存放关联的DAO对象*/
    protected transient AbstractDAO abstractDAO;

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

    public AbstractCondition(Class<T> _class, DataSource dataSource, AbstractDAO abstractDAO) {
        this._class = _class;
        this.tableName = "`" + SQLUtil.classTableMap.get(_class.getName()) + "`";
        this.dataSource = dataSource;
        this.abstractDAO = abstractDAO;
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
    public Condition addBetweenQuery(String field, Object start, Object end) {
        whereBuilder.append("(t.`"+StringUtil.Camel2Underline(field)+"` between ? and ? ) and ");
        parameterList.add(start);
        parameterList.add(end);
        return this;
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
    public Condition addJSONObjectQuery(JSONObject queryCondition) {
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
        String fieldName = getFirstClassFieldInMainClass(this._class,_class);
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
        field = StringUtil.Camel2Underline(field);
        columnBuilder.append("t.`" + field + "` as `t_" + field + "`,");
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
        String sqlBack = sql;
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
        //因其他方法会调用count方法,若sql已经有值,需保存之前的值
        if(ValidateUtil.isNotEmpty(sqlBack)){
            sql = sqlBack;
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
        try {
            Connection connection = abstractDAO.getConnection();
            PreparedStatement ps = connection.prepareStatement(sql);
            for (Object parameter : updateParameterList) {
                ps.setObject(parameterIndex++, parameter);
                replaceParameter(parameter);
            }
            addMainTableParameters(ps);
            addJoinTableParameters(ps);
            logger.debug("[Update]执行SQL:{}", sql);
            effect = ps.executeUpdate();
            ps.close();
            if(!abstractDAO.startTranscation){
                connection.close();
            }
        }catch (SQLException e){
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
        try {
            Connection connection = abstractDAO.getConnection();
            PreparedStatement ps = connection.prepareStatement(sql);
            addMainTableParameters(ps);
            addJoinTableParameters(ps);
            logger.debug("[Delete]执行SQL:{}", sql);
            effect = ps.executeUpdate();
            ps.close();
            if(!abstractDAO.startTranscation){
                connection.close();
            }
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
            int count = (int) count();
            logger.debug("[getArray]执行SQL:{}", sql);
            ResultSet resultSet = ps.executeQuery();
            JSONArray array = ReflectionUtil.mappingResultSetToJSONArray(resultSet, "t", count);
            ps.close();
            return array;
        } catch (SQLException e) {
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
            int count = (int) count();
            logger.debug("[getValueList]执行SQL:{}", sql);
            ResultSet resultSet = ps.executeQuery();
            List<E> instanceList = ReflectionUtil.mappingSingleResultToList(resultSet, count, _class);
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
            logger.debug("[getAggerateList]执行SQL:{}", sql);
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
            int count = (int) count();
            logger.debug("[getPartList]执行SQL:{}", sql);
            ResultSet resultSet = ps.executeQuery();
            JSONArray array = ReflectionUtil.mappingResultSetToJSONArray(resultSet, "t", count);
            List<T> instanceList = array.toJavaList(_class);
            ps.close();
            return instanceList;
        } catch (Exception e) {
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
    public PageVo<T> getPartPagingList(){
        getPageVo();
        pageVo.setList(getPartList());
        return pageVo;
    }

    @Override
    public PageVo<T> getCompositPagingList() {
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
            int count = (int) count();
            logger.debug("[getCompositArray]执行SQL:{}", sql);
            ResultSet resultSet = ps.executeQuery();
            JSONArray array = new JSONArray(count);

            while (resultSet.next()) {
                JSONObject o = getSubObject(_class,"t",resultSet);
                for (AbstractCondition.AbstractSubCondition subCondition : subConditionList) {
                    if (ValidateUtil.isEmpty(subCondition.compositField)) {
                        continue;
                    }
                    JSONObject subObject = getSubObject(subCondition._class,subCondition.tableAliasName,resultSet);
                    AbstractSubCondition parentSubCondition = (AbstractSubCondition) subCondition.parentSubCondition;
                    if(parentSubCondition==null){
                        o.put(subCondition.compositField, subObject);
                    }else{
                        List<String> fieldNames = new ArrayList<>();
                        while(parentSubCondition!=null){
                            fieldNames.add(parentSubCondition.compositField);
                            parentSubCondition = (AbstractSubCondition)parentSubCondition.parentSubCondition;
                        }
                        JSONObject oo = o;
                        for(int i=fieldNames.size()-1;i>=0;i--){
                            oo = oo.getJSONObject(fieldNames.get(i));
                        }
                        oo.put(subCondition.compositField,subObject);
                    }
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

    public AbstractCondition clone() {
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
            abstractCondition.abstractDAO = this.abstractDAO;
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
            AbstractSubCondition parentSubCondition = (AbstractSubCondition) abstractSubCondition.parentSubCondition;
            if(parentSubCondition==null){
                //如果parentSubCondition为空,则为主表关联子表
                sqlBuilder.append(abstractSubCondition.join+" `" + SQLUtil.classTableMap.get(abstractSubCondition._class.getName()) + "` as " + abstractSubCondition.tableAliasName + " on t." + StringUtil.Camel2Underline(abstractSubCondition.primaryField) + " = " + StringUtil.Camel2Underline(abstractSubCondition.tableAliasName) + "." + StringUtil.Camel2Underline(abstractSubCondition.joinTableField) + " ");
            }else{
                //如果parentSubCondition不为空,则为子表关联子表
                sqlBuilder.append(abstractSubCondition.join+" `" + SQLUtil.classTableMap.get(abstractSubCondition._class.getName()) + "` as " + abstractSubCondition.tableAliasName + " on "+abstractSubCondition.tableAliasName+"." + StringUtil.Camel2Underline(abstractSubCondition.joinTableField) + " = " + StringUtil.Camel2Underline(parentSubCondition.tableAliasName) + "." + StringUtil.Camel2Underline(abstractSubCondition.primaryField) + " ");
            }
        }
    }

    /**
     * 添加where的SQL语句
     */
    protected void addWhereStatement() {
        //添加查询条件
        sqlBuilder.append(" "+whereBuilder.toString());
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
            case "integer": {
            }
            case "long": {
            }
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

    /**找到mainClass中第一个类型为_class的字段名称*/
    private String getFirstClassFieldInMainClass(Class mainClass,Class _class){
        //获取第一个类型为_class的字段名
        Field[] fields = ReflectionUtil.getCompositField(mainClass, _class);
        if (fields == null || fields.length == 0) {
            return null;
        }
        if (fields.length == 1) {
            return fields[0].getName();
        } else {
            throw new IllegalArgumentException("类[" + mainClass.getName() + "]存在[" + fields.length + "]个类型为[" + _class.getName() + "]的成员变量!");
        }
    }

    /**获取子对象属性值*/
    private JSONObject getSubObject(Class _class,String tableAliasName,ResultSet resultSet) throws SQLException {
        JSONObject subObject = new JSONObject();
        Field[] subFields = ReflectionUtil.getFields(_class);
        for (Field field : subFields) {
            if (field.getAnnotation(Ignore.class) != null) {
                continue;
            }
            setValue(tableAliasName,field,subObject,resultSet);
        }
        return subObject;
    }

    /**设置属性值*/
    private void setValue(String tableAlias,Field field,JSONObject o,ResultSet resultSet) throws SQLException {
        String columnName = tableAlias+"_" + StringUtil.Camel2Underline(field.getName());
        String type = field.getType().getSimpleName().toLowerCase();
        switch (type) {
            case "int":
            case "integer": {
                o.put(field.getName(), resultSet.getInt(columnName));
            }
            break;
            case "long": {
                o.put(field.getName(), resultSet.getLong(columnName));
            }
            break;
            case "boolean": {
                o.put(field.getName(), resultSet.getBoolean(columnName));
            }
            break;
            default: {
                o.put(field.getName(), resultSet.getObject(columnName));
            }
        }
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

    class AbstractSubCondition<T> implements SubCondition<T>,Serializable {
        protected Class<T> _class;
        protected String tableAliasName;
        protected String primaryField;
        protected String joinTableField;
        protected String compositField;
        protected StringBuilder whereBuilder = new StringBuilder();
        protected List parameterList = new ArrayList();
        protected transient Condition condition;
        protected String join = "join";
        protected SubCondition parentSubCondition;

        public AbstractSubCondition(Class<T> _class, String tableAliasName, String primaryField, String joinTableField, String compositField, Condition condition) {
            this._class = _class;
            this.tableAliasName = tableAliasName;
            this.primaryField = primaryField;
            this.joinTableField = joinTableField;
            this.compositField = compositField;
            this.condition = condition;
        }

        @Override
        public SubCondition leftJoin() {
            join = "left outer join";
            return this;
        }

        @Override
        public SubCondition rightJoin() {
            join = "right outer join";
            return this;
        }

        @Override
        public <T> SubCondition<T> joinTable(Class<T> _class, String primaryField, String joinTableField) {
            String fieldName = getFirstClassFieldInMainClass(this._class,_class);
            AbstractSubCondition abstractSubCondition = (AbstractSubCondition) condition.joinTable(_class,primaryField,joinTableField,fieldName);
            abstractSubCondition.parentSubCondition = this;
            return abstractSubCondition;
        }

        @Override
        public <T> SubCondition<T> joinTable(Class<T> _class, String primaryField, String joinTableField, String compositField) {
            AbstractSubCondition abstractSubCondition = (AbstractSubCondition) condition.joinTable(_class,primaryField,joinTableField,compositField);
            abstractSubCondition.parentSubCondition = this;
            return abstractSubCondition;
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
        public SubCondition doneSubCondition() {
            return this;
        }

        @Override
        public Condition done() {
            return this.condition;
        }

        public SubCondition clone() {
            try {
                /* 写入当前对象的二进制流 */
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(bos);
                oos.writeObject(this);
                /* 读出二进制流产生的新对象 */
                ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
                ObjectInputStream ois = new ObjectInputStream(bis);
                AbstractSubCondition subCondition = (AbstractSubCondition) ois.readObject();
                subCondition.condition = this.condition;
                return subCondition;
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    class SqliteSubCondition<T> extends AbstractSubCondition<T> {
        public SqliteSubCondition(Class<T> _class, String tableAliasName, String primaryField, String joinTableField, String compositField, Condition condition) {
            super(_class, tableAliasName, primaryField, joinTableField, compositField, condition);
        }

        @Override
        public SubCondition rightJoin() {
            throw new UnsupportedOperationException("RIGHT and FULL OUTER JOINs are not currently supported");
        }
    }
}
