package cn.schoolwow.quickdao.condition;

import cn.schoolwow.quickdao.condition.subCondition.AbstractSubCondition;
import cn.schoolwow.quickdao.condition.subCondition.SubCondition;
import cn.schoolwow.quickdao.dao.AbstractDAO;
import cn.schoolwow.quickdao.domain.*;
import cn.schoolwow.quickdao.helper.SQLHelper;
import cn.schoolwow.quickdao.syntax.Syntax;
import cn.schoolwow.quickdao.syntax.SyntaxHandler;
import cn.schoolwow.quickdao.util.ReflectionUtil;
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
import java.util.Stack;

public class AbstractCondition<T> implements Condition<T>, Serializable {
    Logger logger = LoggerFactory.getLogger(AbstractCondition.class);
    public static String[] patterns = new String[]{"%", "_", "[", "[^", "[!", "]"};
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
    protected PageVo<T> pageVo = null;

    protected Query query;

    public AbstractCondition(Class<T> _class, DataSource dataSource, AbstractDAO abstractDAO, SyntaxHandler syntaxHandler, SQLHelper sqlHelper) {
        query = new Query();
        query.dataSource = dataSource;
        query.abstractDAO = abstractDAO;
        query.syntaxHandler = syntaxHandler;
        query.sqlHelper = sqlHelper;
        query.entity = ReflectionUtil.entityMap.get(_class.getName());
        query._class = _class;
        query.className = _class.getName();
        query.tableName = syntaxHandler.getSyntax(Syntax.Escape, query.entity.tableName);
    }

    @Override
    public Condition distinct() {
        query.distinct = "distinct";
        return this;
    }

    @Override
    public Condition addNullQuery(String field) {
        query.whereBuilder.append("(t." + query.syntaxHandler.getSyntax(Syntax.Escape, StringUtil.Camel2Underline(field)) + " is null) and ");
        return this;
    }

    @Override
    public Condition addNotNullQuery(String field) {
        //判断字段是否是String类型
        query.whereBuilder.append("(t." + query.syntaxHandler.getSyntax(Syntax.Escape, StringUtil.Camel2Underline(field)) + " is not null) and ");
        return this;
    }

    @Override
    public Condition addNotEmptyQuery(String field) {
        //判断字段是否是String类型
        query.whereBuilder.append("(t." + query.syntaxHandler.getSyntax(Syntax.Escape, StringUtil.Camel2Underline(field)) + " is not null and t." + query.syntaxHandler.getSyntax(Syntax.Escape, StringUtil.Camel2Underline(field)) + " != '') and ");
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

    public void addINQuery(String tableAliasName, String field, Object[] values, String in) {
        if (values[0] instanceof String) {
            for (int i = 0; i < values.length; i++) {
                //不能加百分号
                values[i] = values[i].toString();
            }
        }
        query.parameterList.addAll(Arrays.asList(values));
        query.whereBuilder.append("(" + tableAliasName + "." + StringUtil.Camel2Underline(field) + " " + in + " (");
        for (int i = 0; i < values.length; i++) {
            query.whereBuilder.append("?,");
        }
        query.whereBuilder.deleteCharAt(query.whereBuilder.length() - 1);
        query.whereBuilder.append(") ) and ");
    }

    @Override
    public Condition addBetweenQuery(String field, Object start, Object end) {
        query.whereBuilder.append("(t." + query.syntaxHandler.getSyntax(Syntax.Escape, StringUtil.Camel2Underline(field)) + " between ? and ? ) and ");
        query.parameterList.add(start);
        query.parameterList.add(end);
        return this;
    }

    @Override
    public Condition addLikeQuery(String field, Object value) {
        if (value == null || value.toString().equals("")) {
            return this;
        }
        query.whereBuilder.append("(t." + query.syntaxHandler.getSyntax(Syntax.Escape, StringUtil.Camel2Underline(field)) + " like ?) and ");
        boolean hasContains = false;
        for (String pattern : patterns) {
            if (((String) value).contains(pattern)) {
                query.parameterList.add(value);
                hasContains = true;
                break;
            }
        }
        if (!hasContains) {
            query.parameterList.add("%" + value + "%");
        }
        return this;
    }

    @Override
    public Condition addQuery(String query) {
        this.query.whereBuilder.append("(" + query + ") and ");
        return this;
    }

    @Override
    public Condition addQuery(String field, Object value) {
        if (value == null || value.toString().equals("")) {
            return this;
        }
        if (value instanceof String) {
            addLikeQuery(field,value);
        } else {
            addQuery(field, "=", value);
        }
        return this;
    }

    @Override
    public Condition addQuery(String field, String operator, Object value) {
        query.whereBuilder.append("(t." + query.syntaxHandler.getSyntax(Syntax.Escape, StringUtil.Camel2Underline(field)) + " " + operator + " ?) and ");
        query.parameterList.add(value);
        return this;
    }

    @Override
    public Condition addJSONObjectQuery(JSONObject queryCondition) {
        //主键查询
        {
            Property[] properties = query.entity.properties;
            for (Property property : properties) {
                if (queryCondition.containsKey(property.name)) {
                    addQuery(property.name, queryCondition.get(property.name));
                }
                if (queryCondition.containsKey(property.name + "Start")) {
                    addQuery(property.name, ">=", queryCondition.get(property.name + "Start"));
                }
                if (queryCondition.containsKey(property.name + "End")) {
                    addQuery(property.name, "<=", queryCondition.get(property.name + "End"));
                }
                if (queryCondition.containsKey(property.name + "IN")) {
                    addInQuery(property.name, queryCondition.getJSONArray(property.name + "IN"));
                }
                if (queryCondition.containsKey(property.name + "NOTNULL")) {
                    addNotNullQuery(property.name);
                }
                if (queryCondition.containsKey(property.name + "NULL")) {
                    addNullQuery(property.name);
                }
                if (queryCondition.containsKey(property.name + "NOTEMPTY")) {
                    addNotEmptyQuery(property.name);
                }
            }
            if (queryCondition.containsKey("_orderBy")) {
                if (queryCondition.get("_orderBy") instanceof String) {
                    orderBy(queryCondition.getString("_orderBy"));
                } else if (queryCondition.get("_orderBy") instanceof JSONArray) {
                    JSONArray array = queryCondition.getJSONArray("_orderBy");
                    for (int i = 0; i < array.size(); i++) {
                        orderBy(array.getString(i));
                    }
                }
            }
            if (queryCondition.containsKey("_orderByDesc")) {
                if (queryCondition.get("_orderByDesc") instanceof String) {
                    orderByDesc(queryCondition.getString("_orderByDesc"));
                } else if (queryCondition.get("_orderByDesc") instanceof JSONArray) {
                    JSONArray array = queryCondition.getJSONArray("_orderByDesc");
                    for (int i = 0; i < array.size(); i++) {
                        orderByDesc(array.getString(i));
                    }
                }
            }
            if (queryCondition.containsKey("_pageNumber") && queryCondition.containsKey("_pageSize")) {
                page(queryCondition.getInteger("_pageNumber"), queryCondition.getInteger("_pageSize"));
            }
        }
        //外键关联查询
        {
            JSONArray _joinTables = queryCondition.getJSONArray("_joinTables");
            if (_joinTables == null || _joinTables.size() == 0) {
                return this;
            }
            try {
                Stack<SubCondition> subConditionStack = new Stack<>();
                Stack<JSONArray> joinTablesStack = new Stack<>();
                for (int i = 0; i < _joinTables.size(); i++) {
                    JSONObject _joinTable = _joinTables.getJSONObject(i);
                    String primaryField = _joinTable.getString("_primaryField");
                    String joinTableField = _joinTable.getString("_joinTableField");
                    SubCondition subCondition = joinTable(Class.forName(_joinTable.getString("_class")), primaryField, joinTableField);
                    addSubConditionQuery(subCondition, _joinTable);

                    if (_joinTable.containsKey("_joinTables")) {
                        subConditionStack.push(subCondition);
                        joinTablesStack.push(_joinTable.getJSONArray("_joinTables"));
                        while (!joinTablesStack.isEmpty()) {
                            _joinTables = joinTablesStack.pop();
                            subCondition = subConditionStack.pop();
                            addSubConditionQuery(subCondition, _joinTable);
                            for (int j = 0; j < _joinTables.size(); j++) {
                                _joinTable = _joinTables.getJSONObject(j);
                                primaryField = _joinTable.getString("_primaryField");
                                joinTableField = _joinTable.getString("_joinTableField");
                                SubCondition _subCondition = subCondition.joinTable(Class.forName(_joinTable.getString("_class")), primaryField, joinTableField);
                                if (_joinTable.containsKey("_joinTables")) {
                                    subConditionStack.push(_subCondition);
                                    joinTablesStack.push(_joinTable.getJSONArray("_joinTables"));
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return this;
    }

    /**
     * 添加子查询条件
     */
    private void addSubConditionQuery(SubCondition subCondition, JSONObject _joinTable) {
        Property[] properties = ReflectionUtil.entityMap.get(_joinTable.getString("_class")).properties;
        for (Property property : properties) {
            if (_joinTable.containsKey(property.name)) {
                subCondition.addQuery(property.name, _joinTable.get(property.name));
            }
            if (_joinTable.containsKey(property.name + "Start")) {
                subCondition.addQuery(property.name, ">=", _joinTable.get(property.name + "Start"));
            }
            if (_joinTable.containsKey(property.name + "End")) {
                subCondition.addQuery(property.name, "<=", _joinTable.get(property.name + "End"));
            }
            if (_joinTable.containsKey(property.name + "IN")) {
                subCondition.addInQuery(property.name, _joinTable.getJSONArray(property.name + "IN"));
            }
            if (_joinTable.containsKey(property.name + "NOTNULL")) {
                subCondition.addNotNullQuery(property.name);
            }
            if (_joinTable.containsKey(property.name + "NULL")) {
                subCondition.addNullQuery(property.name);
            }
            if (_joinTable.containsKey(property.name + "NOTEMPTY")) {
                subCondition.addNotEmptyQuery(property.name);
            }
        }
        if (_joinTable.containsKey("_orderBy")) {
            if (_joinTable.get("_orderBy") instanceof String) {
                subCondition.orderBy(_joinTable.getString("_orderBy"));
            } else if (_joinTable.get("_orderBy") instanceof JSONArray) {
                JSONArray array = _joinTable.getJSONArray("_orderBy");
                for (int j = 0; j < array.size(); j++) {
                    subCondition.orderBy(array.getString(j));
                }
            }
        }
        if (_joinTable.containsKey("_orderByDesc")) {
            if (_joinTable.get("_orderByDesc") instanceof String) {
                subCondition.orderByDesc(_joinTable.getString("_orderByDesc"));
            } else if (_joinTable.get("_orderByDesc") instanceof JSONArray) {
                JSONArray array = _joinTable.getJSONArray("_orderByDesc");
                for (int j = 0; j < array.size(); j++) {
                    subCondition.orderByDesc(array.getString(j));
                }
            }
        }
        subCondition.done();
    }

    @Override
    public Condition addUpdate(String field, Object value) {
        if (query.updateParameterList == null) {
            query.updateParameterList = new ArrayList();
        }
        query.setBuilder.append("t." + query.syntaxHandler.getSyntax(Syntax.Escape, StringUtil.Camel2Underline(field)) + "=?,");
        query.updateParameterList.add(value);
        return this;
    }

    @Override
    public Condition addAggerate(String aggerate, String field) {
        field = StringUtil.Camel2Underline(field);
        query.aggregateColumnBuilder.append(aggerate + "(t." + query.syntaxHandler.getSyntax(Syntax.Escape, field) + ") as " + query.syntaxHandler.getSyntax(Syntax.Escape, aggerate + "(" + field + ")") + ",");
        return this;
    }

    @Override
    public Condition addAggerate(String aggerate, String field, String alias) {
        field = StringUtil.Camel2Underline(field);
        query.aggregateColumnBuilder.append(aggerate + "(t." + query.syntaxHandler.getSyntax(Syntax.Escape, field) + ") as " + query.syntaxHandler.getSyntax(Syntax.Escape, alias) + ",");
        return this;
    }

    @Override
    public Condition groupBy(String field) {
        query.groupByBuilder.append("t." + query.syntaxHandler.getSyntax(Syntax.Escape, StringUtil.Camel2Underline(field)) + ",");
        return this;
    }

    @Override
    public <T> SubCondition<T> joinTable(Class<T> _class, String primaryField, String joinTableField) {
        String tableNameAlias = "t" + (joinTableIndex++);
        String fieldName = ReflectionUtil.getFirstClassFieldInMainClass(query.className, _class.getName());
        AbstractSubCondition<T> subCondition = new AbstractSubCondition<T>(_class, tableNameAlias, primaryField, joinTableField, fieldName, this, query);
        query.subQueryList.add(subCondition.subQuery);
        return subCondition;
    }

    @Override
    public <T> SubCondition<T> joinTable(Class<T> _class, String primaryField, String joinTableField, String compositField) {
        String tableNameAlias = "t" + (joinTableIndex++);
        AbstractSubCondition<T> subCondition = new AbstractSubCondition<T>(_class, tableNameAlias, primaryField, joinTableField, compositField, this, query);
        query.subQueryList.add(subCondition.subQuery);
        return subCondition;
    }

    @Override
    public Condition orderBy(String field) {
        query.orderByBuilder.append("t." + query.syntaxHandler.getSyntax(Syntax.Escape, StringUtil.Camel2Underline(field)) + " asc,");
        return this;
    }

    @Override
    public Condition orderByDesc(String field) {
        query.orderByBuilder.append("t." + query.syntaxHandler.getSyntax(Syntax.Escape, StringUtil.Camel2Underline(field)) + " desc,");
        return this;
    }

    @Override
    public Condition limit(long offset, long limit) {
        query.limit = "limit " + offset + "," + limit;
        return this;
    }

    @Override
    public Condition page(int pageNum, int pageSize) {
        query.limit = "limit " + (pageNum - 1) * pageSize + "," + pageSize;
        pageVo = new PageVo<>();
        pageVo.setPageSize(pageSize);
        pageVo.setCurrentPage(pageNum);
        return this;
    }

    @Override
    public Condition addColumn(String field) {
        field = StringUtil.Camel2Underline(field);
        query.columnBuilder.append("t." + query.syntaxHandler.getSyntax(Syntax.Escape, field) + " as " + query.syntaxHandler.getSyntax(Syntax.Escape, "t_" + field) + ",");
        return this;
    }

    protected Condition done() {
        if (query.columnBuilder.length() > 0) {
            query.columnBuilder.deleteCharAt(query.columnBuilder.length() - 1);
        }
        if (query.aggregateColumnBuilder.length() > 0) {
            query.aggregateColumnBuilder.deleteCharAt(query.aggregateColumnBuilder.length() - 1);
        }
        if (query.setBuilder.length() > 0) {
            query.setBuilder.deleteCharAt(query.setBuilder.length() - 1);
            query.setBuilder.insert(0, "set ");
        }
        if (query.whereBuilder.length() > 0) {
            query.whereBuilder.delete(query.whereBuilder.length() - 5, query.whereBuilder.length());
            query.whereBuilder.insert(0, "where ");
        }
        if ("group by ".equals(query.groupByBuilder.toString())) {
            query.groupByBuilder.setLength(0);
        } else {
            query.groupByBuilder.deleteCharAt(query.groupByBuilder.length() - 1);
        }
        if (query.orderByBuilder.length() > 0) {
            query.orderByBuilder.deleteCharAt(query.orderByBuilder.length() - 1);
            query.orderByBuilder.insert(0, "order by ");
        }
        //处理所有子查询的where语句
        for (SubQuery subQuery : query.subQueryList) {
            if (subQuery.whereBuilder.length() > 0) {
                subQuery.whereBuilder.delete(subQuery.whereBuilder.length() - 5, subQuery.whereBuilder.length());
            }
        }
        query.hasDone = true;
        return this;
    }

    @Override
    public long count() {
        assureDone();
        sqlBuilder.setLength(0);
        sqlBuilder.append("select count(1) from " + query.tableName + " as t ");
        addJoinTableStatement();
        addWhereStatement();
        String sqlBack = sql;
        sql = sqlBuilder.toString().replaceAll("\\s+", " ");
        long count = -1;
        try (Connection connection = query.dataSource.getConnection();
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
        if (ValidateUtil.isNotEmpty(sqlBack)) {
            sql = sqlBack;
        }
        return count;
    }

    @Override
    public long update() {
        assureDone();
        assureUpdate();
        sqlBuilder.setLength(0);
        sqlBuilder.append("update " + query.tableName + " as t ");
        addJoinTableStatement();
        sqlBuilder.append(query.setBuilder.toString());
        addWhereStatement();
        sql = sqlBuilder.toString().replaceAll("\\s+", " ");

        long effect = -1;
        try {
            Connection connection = query.abstractDAO.getConnection();
            PreparedStatement ps = connection.prepareStatement(sql);
            for (Object parameter : query.updateParameterList) {
                ps.setObject(query.parameterIndex++, parameter);
                replaceParameter(parameter);
            }
            addMainTableParameters(ps);
            addJoinTableParameters(ps);
            logger.debug("[Update]执行SQL:{}", sql);
            effect = ps.executeUpdate();
            ps.close();
            if (!query.abstractDAO.startTranscation) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return effect;
    }

    @Override
    public long delete() {
        assureDone();
        sqlBuilder.setLength(0);
        sqlBuilder.append("delete t from " + query.tableName + " as t ");
        addJoinTableStatement();
        addWhereStatement();
        sql = sqlBuilder.toString().replaceAll("\\s+", " ");

        long effect = -1;
        try {
            Connection connection = query.abstractDAO.getConnection();
            PreparedStatement ps = connection.prepareStatement(sql);
            addMainTableParameters(ps);
            addJoinTableParameters(ps);
            logger.debug("[Delete]执行SQL:{}", sql);
            effect = ps.executeUpdate();
            ps.close();
            if (!query.abstractDAO.startTranscation) {
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
        if (list == null || list.size() == 0) {
            return null;
        } else {
            return list.get(0);
        }
    }

    @Override
    public List<T> getList() {
        return getArray().toJavaList(query._class);
    }

    @Override
    public JSONArray getArray() {
        assureDone();
        sqlBuilder.setLength(0);
        sqlBuilder.append("select " + query.distinct + " " + query.sqlHelper.columns(query.className, "t") + " from " + query.tableName + " as t ");
        addJoinTableStatement();
        addWhereStatement();
        sqlBuilder.append(" " + query.orderByBuilder.toString() + " " + query.limit);
        sql = sqlBuilder.toString().replaceAll("\\s+", " ");

        try (Connection connection = query.dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);) {
            addMainTableParameters(ps);
            addJoinTableParameters(ps);
            int count = (int) count();
            logger.debug("[getArray]执行SQL:{}", sql);
            ResultSet resultSet = ps.executeQuery();
            JSONArray array = ReflectionUtil.mappingResultSetToJSONArray(resultSet, count);
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
        sqlBuilder.append("select " + query.distinct + " " + "t." + query.syntaxHandler.getSyntax(Syntax.Escape, StringUtil.Camel2Underline(column)) + " from " + query.tableName + " as t ");
        addJoinTableStatement();
        addWhereStatement();
        sqlBuilder.append(" " + query.orderByBuilder.toString() + " " + query.limit);
        sql = sqlBuilder.toString().replaceAll("\\s+", " ");

        try (Connection connection = query.dataSource.getConnection();
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
        sqlBuilder.append("select " + query.distinct + " ");
        if (query.columnBuilder.toString().length() > 0) {
            sqlBuilder.append(query.columnBuilder.toString() + ",");
        }
        sqlBuilder.append(query.aggregateColumnBuilder.toString() + " from " + query.tableName + " as t ");
        addJoinTableStatement();
        addWhereStatement();
        sqlBuilder.append(" " + query.groupByBuilder.toString() + " " + query.orderByBuilder.toString() + " " + query.limit);
        sql = sqlBuilder.toString().replaceAll("\\s+", " ");

        try (Connection connection = query.dataSource.getConnection();
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
        if (query.columnBuilder.length() == 0) {
            throw new IllegalArgumentException("请先调用addColumn()函数!");
        }
        assureDone();
        sqlBuilder.setLength(0);
        sqlBuilder.append("select " + query.distinct + " " + query.columnBuilder.toString() + " from " + query.tableName + " as t ");
        addJoinTableStatement();
        addWhereStatement();
        sqlBuilder.append(" " + query.orderByBuilder.toString() + " " + query.limit);
        sql = sqlBuilder.toString().replaceAll("\\s+", " ");

        try (Connection connection = query.dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);) {
            addMainTableParameters(ps);
            addJoinTableParameters(ps);
            int count = (int) count();
            logger.debug("[getPartList]执行SQL:{}", sql);
            ResultSet resultSet = ps.executeQuery();
            JSONArray array = ReflectionUtil.mappingResultSetToJSONArray(resultSet, count);
            List<T> instanceList = array.toJavaList(query._class);
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
    public PageVo<T> getPartPagingList() {
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
        return array.toJavaList(query._class);
    }

    @Override
    public JSONArray getCompositArray() {
        assureDone();
        sqlBuilder.setLength(0);
        sqlBuilder.append("select " + query.distinct + " " + query.sqlHelper.columns(query.className, "t"));
        for (SubQuery subQuery : query.subQueryList) {
            sqlBuilder.append("," + query.sqlHelper.columns(subQuery.className, subQuery.tableAliasName));
        }
        sqlBuilder.append(" from " + query.tableName + " as t ");
        addJoinTableStatement();
        addWhereStatement();
        sqlBuilder.append(" " + query.orderByBuilder.toString() + " " + query.limit);
        sql = sqlBuilder.toString().replaceAll("\\s+", " ");

        try (Connection connection = query.dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);) {
            addMainTableParameters(ps);
            addJoinTableParameters(ps);
            int count = (int) count();
            logger.debug("[getCompositArray]执行SQL:{}", sql);
            ResultSet resultSet = ps.executeQuery();
            JSONArray array = new JSONArray(count);

            while (resultSet.next()) {
                JSONObject o = getSubObject(query.className, "t", resultSet);
                for (SubQuery subQuery : query.subQueryList) {
                    if (ValidateUtil.isEmpty(subQuery.compositField)) {
                        continue;
                    }
                    JSONObject subObject = getSubObject(subQuery.className, subQuery.tableAliasName, resultSet);
                    SubQuery parentSubQuery = subQuery.parentSubQuery;
                    if (parentSubQuery == null) {
                        o.put(subQuery.compositField, subObject);
                    } else {
                        List<String> fieldNames = new ArrayList<>();
                        while (parentSubQuery != null) {
                            fieldNames.add(parentSubQuery.compositField);
                            parentSubQuery = parentSubQuery.parentSubQuery;
                        }
                        JSONObject oo = o;
                        for (int i = fieldNames.size() - 1; i >= 0; i--) {
                            oo = oo.getJSONObject(fieldNames.get(i));
                        }
                        oo.put(subQuery.compositField, subObject);
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
            abstractCondition.query.dataSource = query.dataSource;
            abstractCondition.query.abstractDAO = query.abstractDAO;
            abstractCondition.query.syntaxHandler = query.syntaxHandler;
            abstractCondition.query.sqlHelper = query.sqlHelper;
            abstractCondition.query.entity = query.entity;
            return abstractCondition;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public String toString() {
        return "[Condition]类[" + query.className + "],where子句:[" + query.whereBuilder.toString() + "],参数列表:[" + query.parameterList + "],排序:[" + query.orderByBuilder.toString() + "],分页:[" + query.limit + "]";
    }

    private PageVo<T> getPageVo() {
        if (pageVo == null) {
            throw new IllegalArgumentException("请先调用page()函数!");
        }
        pageVo.setTotalSize(count());
        pageVo.setTotalPage((pageVo.getTotalSize() / pageVo.getPageSize() + pageVo.getTotalSize() % pageVo.getPageSize() > 0 ? 1 : 0));
        pageVo.setHasMore(pageVo.getCurrentPage() < pageVo.getTotalPage());
        return pageVo;
    }

    protected void addMainTableParameters(PreparedStatement ps) throws SQLException {
        for (Object parameter : query.parameterList) {
            ps.setObject(query.parameterIndex, parameter);
            replaceParameter(parameter);
            query.parameterIndex++;
        }
    }

    /**
     * 添加外键关联查询条件
     */
    protected void addJoinTableStatement() {
        for (SubQuery subQuery : query.subQueryList) {
            Entity entity = ReflectionUtil.entityMap.get(subQuery.className);
            if (subQuery.parentSubQuery == null) {
                //如果parentSubCondition为空,则为主表关联子表
                sqlBuilder.append(subQuery.join + " " + query.syntaxHandler.getSyntax(Syntax.Escape, entity.tableName) + " as " + subQuery.tableAliasName + " on t." + query.syntaxHandler.getSyntax(Syntax.Escape,subQuery.primaryField) + " = " + subQuery.tableAliasName + "." + query.syntaxHandler.getSyntax(Syntax.Escape,subQuery.joinTableField) + " ");
            } else {
                //如果parentSubCondition不为空,则为子表关联子表
                sqlBuilder.append(subQuery.join + " " + query.syntaxHandler.getSyntax(Syntax.Escape, entity.tableName) + " as " + subQuery.tableAliasName + " on " + subQuery.tableAliasName + "." + query.syntaxHandler.getSyntax(Syntax.Escape,subQuery.joinTableField) + " = " + subQuery.parentSubQuery.tableAliasName + "." + query.syntaxHandler.getSyntax(Syntax.Escape,subQuery.primaryField) + " ");
            }
        }
    }

    /**
     * 添加where的SQL语句
     */
    protected void addWhereStatement() {
        //添加查询条件
        sqlBuilder.append(" " + query.whereBuilder.toString());
        for (SubQuery subQuery : query.subQueryList) {
            if (subQuery.whereBuilder.length() > 0) {
                sqlBuilder.append(" and " + subQuery.whereBuilder.toString() + " ");
            }
        }
    }

    /**
     * 添加外键查询参数
     */
    protected void addJoinTableParameters(PreparedStatement ps) throws SQLException {
        for (SubQuery subQuery : query.subQueryList) {
            for (Object parameter : subQuery.parameterList) {
                ps.setObject(query.parameterIndex, parameter);
                replaceParameter(parameter);
                query.parameterIndex++;
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
        if (!query.hasDone) {
            done();
        }
        query.parameterIndex = 1;
    }

    /**
     * 确保执行addUpdate方法
     */
    protected void assureUpdate() {
        if (query.setBuilder.length() == 0) {
            throw new IllegalArgumentException("请先调用addUpdate()函数!");
        }
        if (query.updateParameterList == null || query.updateParameterList.size() == 0) {
            throw new IllegalArgumentException("请先调用addUpdate()函数!");
        }
    }

    /**
     * 获取子对象属性值
     */
    private JSONObject getSubObject(String clssName, String tableAliasName, ResultSet resultSet) throws SQLException {
        JSONObject subObject = new JSONObject();
        Property[] properties = ReflectionUtil.entityMap.get(clssName).properties;
        for (Property property : properties) {
            setValue(tableAliasName, property.field, subObject, resultSet);
        }
        return subObject;
    }

    /**
     * 设置属性值
     */
    private void setValue(String tableAlias, Field field, JSONObject o, ResultSet resultSet) throws SQLException {
        String columnName = tableAlias + "_" + StringUtil.Camel2Underline(field.getName());
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
}
