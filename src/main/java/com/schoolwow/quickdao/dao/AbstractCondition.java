package com.schoolwow.quickdao.dao;

import com.alibaba.fastjson.JSON;
import com.schoolwow.quickdao.annotation.Ignore;
import com.schoolwow.quickdao.util.SQLUtil;
import com.schoolwow.quickdao.util.StatementUtil;
import com.schoolwow.quickdao.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AbstractCondition<T> implements Condition<T>{
    Logger logger = LoggerFactory.getLogger(AbstractCondition.class);
    protected StringBuilder columnBuilder = new StringBuilder();
    protected StringBuilder setBuilder = new StringBuilder();
    protected StringBuilder whereBuilder = new StringBuilder();
    protected StringBuilder groupByBuilder = new StringBuilder("group by ");
    protected StringBuilder havingBuilder = new StringBuilder("having ");
    protected StringBuilder orderByBuilder = new StringBuilder();
    protected String limit = "";
    protected List parameterList = new ArrayList();
    protected List updateParameterList;
    protected Class<T> _class;
    protected DataSource dataSource;

    private int joinTableIndex = 1;
    private List<AbstractSubCondition> subConditionList = new ArrayList<>();

    private String tableName = null;
    private boolean hasDone = false;

    private static String[] patterns = new String[]{"%","_","[","[^","[!","]"};

    public AbstractCondition(Class<T> _class, DataSource dataSource) {
        this._class = _class;
        this.tableName = StringUtil.Camel2Underline(_class.getSimpleName());
        this.dataSource = dataSource;
    }

    @Override
    public Condition addNullQuery(String field) {
        whereBuilder.append("(t.`"+StringUtil.Camel2Underline(field)+"` is null or t.`"+StringUtil.Camel2Underline(field)+"` = '') and ");
        return this;
    }

    @Override
    public Condition addNotNullQuery(String field) {
        whereBuilder.append("(t.`"+StringUtil.Camel2Underline(field)+"` is not null and t.`"+StringUtil.Camel2Underline(field)+"` != '') and ");
        return this;
    }

    @Override
    public Condition addInQuery(String field, Object[] values) {
        if(values==null||values.length==0){
            return this;
        }
        parameterList.addAll(Arrays.asList(values));
        whereBuilder.append("(t."+StringUtil.Camel2Underline(field)+" in (");
        for(int i=0;i<values.length;i++){
            whereBuilder.append("?,");
        }
        whereBuilder.deleteCharAt(whereBuilder.length()-1);
        whereBuilder.append(") ) and ");
        return this;
    }

    @Override
    public Condition addInQuery(String field, List values) {
        return addInQuery(field,values.toArray(new Object[values.size()]));
    }

    @Override
    public Condition addQuery(String query) {
        whereBuilder.append("("+query+") and ");
        return this;
    }

    @Override
    public Condition addQuery(String property, Object value) {
        if(value==null||value.toString().equals("")){
            return this;
        }
        if(value instanceof String){
            addQuery(property,"like",value);
        }else{
            addQuery(property,"=",value);
        }
        return this;
    }

    @Override
    public Condition addQuery(String property, String operator, Object value) {
        if(value instanceof String){
            whereBuilder.append("(t.`"+property+"` "+operator+" ?) and ");
            boolean hasContains = false;
            for(String pattern:patterns){
                if(((String) value).contains(pattern)){
                    parameterList.add(value);
                    hasContains = true;
                    break;
                }
            }
            if(!hasContains){
                parameterList.add("%"+value+"%");

            }
        }else{
            whereBuilder.append("(t.`"+property+"` "+operator+" ?) and ");
            parameterList.add(value);
        }
        return this;
    }

    @Override
    public Condition addUpdate(String property, Object value) {
        if(updateParameterList==null){
            updateParameterList = new ArrayList();
        }
        setBuilder.append("t.`"+StringUtil.Camel2Underline(property)+"`=?,");
        updateParameterList.add(value);
        return this;
    }

//    @Override
//    public Condition groupBy(String field) {
//        groupByBuilder.append("t.`"+StringUtil.Camel2Underline(field)+"`,");
//        return this;
//    }
//
//    @Override
//    public Condition having(String query) {
//        havingBuilder.append("("+query+") and ");
//        return this;
//    }

    @Override
    public <T> SubCondition<T> joinTable(Class<T> _class, String primaryField, String joinTableField) {
        String tableNameAlias = "t"+joinTableIndex;
        joinTableIndex++;
        SubCondition<T> subCondition = new AbstractSubCondition<T>(_class,tableNameAlias,primaryField,joinTableField,this);
        subConditionList.add((AbstractSubCondition) subCondition);
        return subCondition;
    }

    @Override
    public Condition orderBy(String field) {
        orderByBuilder.append("t.`"+field+"` asc,");
        return this;
    }

    @Override
    public Condition orderByDesc(String field) {
        orderByBuilder.append("t.`"+field+"` desc,");
        return this;
    }

    @Override
    public Condition limit(long offset,long limit) {
        this.limit = "limit "+offset+","+limit;
        return this;
    }

    @Override
    public Condition addColumn(String field) {
        columnBuilder.append("t."+StringUtil.Camel2Underline(field)+",");
        return this;
    }

    private Condition done() {
        if (columnBuilder.length() > 0) {
            columnBuilder.deleteCharAt(columnBuilder.length() - 1);
        }
        if(setBuilder.length()>0){
            setBuilder.deleteCharAt(setBuilder.length() - 1);
            setBuilder.insert(0,"set ");
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
        for(AbstractSubCondition abstractSubCondition:subConditionList){
            if (abstractSubCondition.whereBuilder.length() > 0) {
                abstractSubCondition.whereBuilder.delete(abstractSubCondition.whereBuilder.length() - 5,abstractSubCondition.whereBuilder.length());
            }
        }

        hasDone = true;
        return this;
    }

    @Override
    public long count() {
        if(!hasDone){
            done();
            hasDone = true;
        }
        StringBuilder countSQLBuilder = new StringBuilder("select count(1) from `"+tableName+"` as t ");
        addJoinTableStatement(countSQLBuilder);
        String countSQL = countSQLBuilder.toString().replaceAll("\\s+"," ");

        long count = -1;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(countSQL);){
            if(parameterList!=null&&parameterList.size()>0){
                for(int i=0;i<parameterList.size();i++){
                    ps.setObject((i+1),parameterList.get(i));
                }
            }
            addJoinTableParamters(ps);
            logger.debug("count sql:"+ps.toString());
            ResultSet resultSet = ps.executeQuery();
            if(resultSet.next()){
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
        if(!hasDone){
            done();
            hasDone = true;
        }
        if(setBuilder.length()==0){
            logger.warn("请先调用addUpdate方法!");
            return 0;
        }

        StringBuilder updateSQLBuilder = new StringBuilder("update "+tableName+" as t "+setBuilder.toString()+" ");
        addJoinTableStatement(updateSQLBuilder);
        String updateSQL = updateSQLBuilder.toString().replaceAll("\\s+"," ");

        long effect = 0;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(updateSQL);){
            int index = 1;
            if(updateParameterList!=null&&updateParameterList.size()>0){
                for(Object parameter:updateParameterList){
                    ps.setObject(index++,parameter);
                }
                for(Object parameter:parameterList){
                    ps.setObject(index++,parameter);
                }
            }
            addJoinTableParamters(ps);
            logger.debug("update sql:"+ps.toString());
            effect = ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return effect;
    }

    @Override
    public long delete() {
        if(!hasDone){
            done();
            hasDone = true;
        }
        StringBuilder deleteSQLBuilder = new StringBuilder("delete t from "+tableName+" as t ");
        addJoinTableStatement(deleteSQLBuilder);
        String deleteSQL = deleteSQLBuilder.toString().replaceAll("\\s+"," ");

        long effect = 0;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(deleteSQL);){
            if(parameterList!=null&&parameterList.size()>0){
                for(int i=0;i<parameterList.size();i++){
                    ps.setObject((i+1),parameterList.get(i));
                }
            }
            addJoinTableParamters(ps);
            logger.debug("delete sql:"+ps.toString());
            effect = ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return effect;
    }

    @Override
    public List<T> getList() {
        if(!hasDone){
            done();
            hasDone = true;
        }
        StringBuilder getListSQLBuilder = new StringBuilder("select "+SQLUtil.columns(_class,"t")+" from `"+tableName+"` as t ");
        addJoinTableStatement(getListSQLBuilder);
        getListSQLBuilder.append(groupByBuilder.toString()+" "+havingBuilder.toString()+" "+orderByBuilder.toString()+" "+limit);
        String getListSQL = getListSQLBuilder.toString().replaceAll("\\s+"," ");

        int size = (int) count();
        List<T> instanceList = new ArrayList<>(size);
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(getListSQL);){
            if(parameterList!=null&&parameterList.size()>0){
                for(int i=0;i<parameterList.size();i++){
                    ps.setObject((i+1),parameterList.get(i));
                }
            }
            addJoinTableParamters(ps);
            logger.debug("get list:"+ps.toString());
            ResultSet resultSet = ps.executeQuery();
            while(resultSet.next()){
                T instance = _class.newInstance();
                Field[] fields = _class.getDeclaredFields();
                Field.setAccessible(fields,true);
                for(Field field:fields){
                    if(field.getAnnotation(Ignore.class)!=null){
                        continue;
                    }
                    StatementUtil.getSingleField(resultSet, instance, field,"t."+StringUtil.Camel2Underline(field.getName()));
                }
                instanceList.add(instance);
            }
            resultSet.close();
        } catch (SQLException | InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
            return null;
        }
        return instanceList;
    }

    @Override
    public List<T> getValueList(Class<T> _class, String column) {
        if(!hasDone){
            done();
            hasDone = true;
        }
        StringBuilder getValueListSQLBuilder = new StringBuilder("select " + (columnBuilder.length()>0?columnBuilder.toString():"t.`"+column+"` as "+column+" ") + " from " + tableName + " as t ");
        addJoinTableStatement(getValueListSQLBuilder);
        getValueListSQLBuilder.append(groupByBuilder.toString() +" "+ havingBuilder.toString() + " " + orderByBuilder.toString() + " " + limit);
        String getValueListSQL = getValueListSQLBuilder.toString().replaceAll("\\s+"," ");

        int size = (int) count();
        List<T> instanceList = new ArrayList<>(size);
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(getValueListSQL);) {
            if (parameterList != null && parameterList.size() > 0) {
                for (int i = 0; i < parameterList.size(); i++) {
                    ps.setObject((i + 1), parameterList.get(i));
                }
            }
            addJoinTableParamters(ps);
            logger.debug("get value:"+ps.toString());
            ResultSet resultSet = ps.executeQuery();
            while (resultSet.next()) {
                T instance = _class.getConstructor(String.class).newInstance(resultSet.getString("t."+column));
                instanceList.add(instance);
            }
        } catch (SQLException | InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            e.printStackTrace();
            return null;
        }
        return instanceList;
    }

    private void addJoinTableStatement(StringBuilder SQLBuilder) {
        for (AbstractSubCondition abstractSubCondition : subConditionList) {
            String subTableName = StringUtil.Camel2Underline(abstractSubCondition._class.getSimpleName());
            SQLBuilder.append("join " + subTableName + " as " + abstractSubCondition.tableAliasName + " on t." + abstractSubCondition.primaryField + " = " + abstractSubCondition.tableAliasName + "." + abstractSubCondition.joinTableField + " ");
        }
        SQLBuilder.append(whereBuilder.toString());
        for (AbstractSubCondition abstractSubCondition : subConditionList) {
            if (abstractSubCondition.whereBuilder.length() > 0) {
                SQLBuilder.append(" and " + abstractSubCondition.whereBuilder.toString() + " ");
            }
        }
    }

    private void addJoinTableParamters(PreparedStatement ps) throws SQLException {
        int parameterIndex = parameterList.size()+1;
        for (AbstractSubCondition abstractSubCondition : subConditionList) {
            if(abstractSubCondition.parameterList!=null&&abstractSubCondition.parameterList.size()>0){
                for(Object parameter:abstractSubCondition.parameterList){
                    ps.setObject(parameterIndex++,parameter);
                }
            }
        }
    }
}
