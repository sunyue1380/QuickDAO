package com.schoolwow.quickdao.dao;

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
    protected StringBuilder whereBuilder = new StringBuilder();
    protected StringBuilder groupByBuilder = new StringBuilder("group by ");
    protected StringBuilder havingBuilder = new StringBuilder("having ");
    protected StringBuilder orderByBuilder = new StringBuilder();
    protected String limit = "";
    protected List parameterList = new ArrayList();
    protected Class<T> _class;
    protected DataSource dataSource;
    protected ResultSet resultSet;

    private static String[] patterns = new String[]{"%","_","[","[^","[!","]"};

    public AbstractCondition(Class<T> _class, DataSource dataSource) {
        this._class = _class;
        this.dataSource = dataSource;
    }

    @Override
    public Condition addNullQuery(String field) {
        whereBuilder.append("(`"+StringUtil.Camel2Underline(field)+"` is null) and ");
        return this;
    }

    @Override
    public Condition addNotNullQuery(String field) {
        whereBuilder.append("(`"+StringUtil.Camel2Underline(field)+"` is not null) and ");
        return this;
    }

    @Override
    public Condition addInQuery(String field, Object[] values) {
        parameterList.addAll(Arrays.asList(values));
        whereBuilder.append("("+StringUtil.Camel2Underline(field)+" in (");
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
            whereBuilder.append("(`"+property+"` "+operator+" ?) and ");
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
            whereBuilder.append("(`"+property+"` "+operator+" ?) and ");
            parameterList.add(value);
        }
        return this;
    }

    @Override
    public Condition groupBy(String field) {
        groupByBuilder.append("`"+StringUtil.Camel2Underline(field)+"`,");
        return this;
    }

    @Override
    public Condition having(String query) {
        havingBuilder.append("("+query+") and ");
        return this;
    }

    @Override
    public Condition orderBy(String field) {
        orderByBuilder.append("`"+field+"` asc,");
        return this;
    }

    @Override
    public Condition orderByDesc(String field) {
        orderByBuilder.append("`"+field+"` desc,");
        return this;
    }

    @Override
    public Condition limit(long offset,long limit) {
        this.limit = "limit "+offset+","+limit;
        return this;
    }

    @Override
    public Condition addColumn(String field) {
        columnBuilder.append(""+StringUtil.Camel2Underline(field)+",");
        return this;
    }

    @Override
    public Condition done() {
        if (columnBuilder.length() > 0) {
            columnBuilder.deleteCharAt(columnBuilder.length() - 1);
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
        return this;
    }

    @Override
    public long count() {
        String countSQL = "select count(1) from "+StringUtil.Camel2Underline(_class.getSimpleName())+" "+whereBuilder.toString();
        long count = -1;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(countSQL);){
            if(parameterList!=null&&parameterList.size()>0){
                for(int i=0;i<parameterList.size();i++){
                    ps.setObject((i+1),parameterList.get(i));
                }
            }
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
    public long delete() {
        String deleteSQL = "delete from "+StringUtil.Camel2Underline(_class.getSimpleName())+" "+whereBuilder.toString();
        long effect = 0;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(deleteSQL);){
            if(parameterList!=null&&parameterList.size()>0){
                for(int i=0;i<parameterList.size();i++){
                    ps.setObject((i+1),parameterList.get(i));
                }
            }
            logger.debug("delete sql:"+ps.toString());
            effect = ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return effect;
    }

    @Override
    public List<T> getList() {
        StringBuilder sqlBuilder = new StringBuilder("select "+SQLUtil.columns(_class)+" from "+StringUtil.Camel2Underline(_class.getSimpleName())+" ");
        sqlBuilder.append(whereBuilder.toString()+" "+groupByBuilder.toString()+" "+havingBuilder.toString()+" "+orderByBuilder.toString()+" "+limit);

        int size = (int) count();
        List<T> instanceList = new ArrayList<>(size);
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sqlBuilder.toString());){
            if(parameterList!=null&&parameterList.size()>0){
                for(int i=0;i<parameterList.size();i++){
                    ps.setObject((i+1),parameterList.get(i));
                }
            }
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
                    StatementUtil.getSingleField(resultSet, instance, field,StringUtil.Camel2Underline(field.getName()));
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
        StringBuilder sqlBuilder = new StringBuilder("select " + (columnBuilder.length()>0?columnBuilder.toString():"`"+column+"`") + " from " + StringUtil.Camel2Underline(this._class.getSimpleName()) + " ");
        sqlBuilder.append(whereBuilder.toString() + " " + groupByBuilder.toString() +" "+ havingBuilder.toString() + " " + orderByBuilder.toString() + " " + limit);

        int size = (int) count();
        List<T> instanceList = new ArrayList<>(size);
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sqlBuilder.toString());) {
            if (parameterList != null && parameterList.size() > 0) {
                for (int i = 0; i < parameterList.size(); i++) {
                    ps.setObject((i + 1), parameterList.get(i));
                }
            }
            logger.debug("get value:"+ps.toString());
            ResultSet resultSet = ps.executeQuery();
            while (resultSet.next()) {
                T instance = _class.getConstructor(String.class).newInstance(resultSet.getString(column));
                instanceList.add(instance);
            }
        } catch (SQLException | InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            e.printStackTrace();
            return null;
        }
        return instanceList;
    }
}
