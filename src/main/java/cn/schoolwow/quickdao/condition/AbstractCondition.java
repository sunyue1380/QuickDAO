package cn.schoolwow.quickdao.condition;

import cn.schoolwow.quickdao.annotation.Ignore;
import cn.schoolwow.quickdao.domain.PageVo;
import cn.schoolwow.quickdao.util.ReflectionUtil;
import cn.schoolwow.quickdao.util.SQLUtil;
import cn.schoolwow.quickdao.util.StringUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class AbstractCondition<T> implements Condition<T>{
    Logger logger = LoggerFactory.getLogger(AbstractCondition.class);
    protected StringBuilder columnBuilder = new StringBuilder();
    protected StringBuilder setBuilder = new StringBuilder();
    /**查询条件构建*/
    protected StringBuilder whereBuilder = new StringBuilder();
    /**分组*/
    protected StringBuilder groupByBuilder = new StringBuilder("group by ");
    /**分组过滤*/
    protected StringBuilder havingBuilder = new StringBuilder("having ");
    /**排序*/
    protected StringBuilder orderByBuilder = new StringBuilder();
    /**分页*/
    protected String limit = "";
    /**存放查询参数*/
    protected List parameterList = new ArrayList();
    /**存放更新参数*/
    protected List updateParameterList;
    /**类名*/
    protected Class<T> _class;
    /**数据源*/
    protected DataSource dataSource;

    /**构建sql语句*/
    protected StringBuilder sqlBuilder = new StringBuilder();
    /**用于记录sql日志*/
    protected String sql;

    /**关联表计数*/
    private int joinTableIndex = 1;
    /**关联查询条件*/
    private List<AbstractSubCondition> subConditionList = new ArrayList<>();
    /**表名*/
    protected String tableName = null;
    /**是否已经完成条件构建*/
    protected boolean hasDone = false;

    protected PageVo<T> pageVo = null;

    private static String[] patterns = new String[]{"%","_","[","[^","[!","]"};

    public AbstractCondition(Class<T> _class, DataSource dataSource) {
        this._class = _class;
        this.tableName = "`"+SQLUtil.classTableMap.get(_class)+"`";
        this.dataSource = dataSource;
    }

    @Override
    public Condition addNullQuery(String field) {
        whereBuilder.append("(t.`"+StringUtil.Camel2Underline(field)+"` is null or t.`"+StringUtil.Camel2Underline(field)+"` = '') and ");
        return this;
    }

    @Override
    public Condition addNotNullQuery(String field) {
        //判断字段是否是String类型
        whereBuilder.append("(t.`"+StringUtil.Camel2Underline(field)+"` is not null) and ");
        return this;
    }

    @Override
    public Condition addNotEmptyQuery(String field) {
        //判断字段是否是String类型
        whereBuilder.append("(t.`"+StringUtil.Camel2Underline(field)+"` is not null and t.`"+StringUtil.Camel2Underline(field)+"` != '') and ");
        return this;
    }

    @Override
    public Condition addInQuery(String field, Object[] values) {
        if(values==null||values.length==0){
            return this;
        }
        if(values[0] instanceof String){
            for(int i=0;i<values.length;i++){
                //不能加百分号
                values[i] = values[i].toString();
            }
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
            whereBuilder.append("(t.`"+StringUtil.Camel2Underline(property)+"` "+operator+" ?) and ");
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
            whereBuilder.append("(t.`"+StringUtil.Camel2Underline(property)+"` "+operator+" ?) and ");
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
        String tableNameAlias = "t"+(joinTableIndex++);
        SubCondition<T> subCondition = new AbstractSubCondition<T>(_class,tableNameAlias,primaryField,joinTableField,this);
        subConditionList.add((AbstractSubCondition) subCondition);
        return subCondition;
    }

    @Override
    public Condition orderBy(String field) {
        orderByBuilder.append("t.`"+StringUtil.Camel2Underline(field)+"` asc,");
        return this;
    }

    @Override
    public Condition orderByDesc(String field) {
        orderByBuilder.append("t.`"+StringUtil.Camel2Underline(field)+"` desc,");
        return this;
    }

    @Override
    public Condition limit(long offset,long limit) {
        this.limit = "limit "+offset+","+limit;
        return this;
    }

    @Override
    public Condition page(int pageNum, int pageSize) {
        this.limit = "limit "+(pageNum-1)*pageSize+","+pageSize;
        pageVo = new PageVo<>();
        pageVo.setPageSize(pageSize);
        pageVo.setCurrentPage(pageNum);
        return this;
    }

    @Override
    public Condition addColumn(String field) {
        columnBuilder.append("t."+StringUtil.Camel2Underline(field)+",");
        return this;
    }

    protected Condition done() {
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
        assureDone();
        sqlBuilder.setLength(0);
        sqlBuilder.append("select count(1) from "+tableName+" as t ");
        addJoinTableStatement();
        sql = sqlBuilder.toString().replaceAll("\\s+"," ");
        //设置参数
        long count = -1;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)){
            //设置主表查询参数
            for(int i=0;i<parameterList.size();i++){
                ps.setObject((i+1),parameterList.get(i));
                replaceParameter(parameterList.get(i));
            }
            //设置外键查询参数
            addJoinTableParamters(ps);
            logger.debug("[Count]执行SQL:{}",sql);
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
        assureDone();
        if(setBuilder.length()==0){
            logger.warn("请先调用addUpdate方法!");
            return 0;
        }
        sqlBuilder.setLength(0);
        sqlBuilder.append("update "+tableName+" as t "+setBuilder.toString()+" ");
        addJoinTableStatement();
        sql = sqlBuilder.toString().replaceAll("\\s+"," ");

        long effect = -1;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);){
            int parameterIndex = 1;
            if(updateParameterList!=null&&updateParameterList.size()>0){
                for(Object parameter:updateParameterList){
                    ps.setObject(parameterIndex++,parameter);
                    replaceParameter(parameter);
                }
            }
            for(Object parameter:parameterList){
                ps.setObject(parameterIndex++,parameter);
                replaceParameter(parameter);
            }
            addJoinTableParamters(ps);
            logger.debug("[Update]执行SQL:{}",sql);
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
        sqlBuilder.append("delete t from "+tableName+" as t ");
        addJoinTableStatement();
        sql = sqlBuilder.toString().replaceAll("\\s+"," ");

        long effect = -1;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);){
            for(int i=0;i<parameterList.size();i++){
                ps.setObject((i+1),parameterList.get(i));
                replaceParameter(parameterList.get(i));
            }
            addJoinTableParamters(ps);
            logger.debug("[Delete]执行SQL:{}",sql);
            effect = ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return effect;
    }

    @Override
    public List<T> getList() {
        assureDone();
        sqlBuilder.setLength(0);
        sqlBuilder.append("select "+SQLUtil.columns(_class,"t")+" from "+tableName+" as t ");
        addJoinTableStatement();
        sqlBuilder.append(groupByBuilder.toString()+" "+havingBuilder.toString()+" "+orderByBuilder.toString()+" "+limit);
        sql = sqlBuilder.toString().replaceAll("\\s+"," ");

        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);){
            for(int i=0;i<parameterList.size();i++){
                ps.setObject((i+1),parameterList.get(i));
                replaceParameter(parameterList.get(i));
            }
            addJoinTableParamters(ps);
            logger.debug("[getList]执行SQL:{}",sql);
            ResultSet resultSet = ps.executeQuery();
            List<T> instanceList = new ArrayList((int) count());
            ReflectionUtil.mappingResultToList(resultSet,instanceList,_class);
            ps.close();
            return instanceList;
        } catch (SQLException | InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public PageVo<T> getPagingList() {
        if(pageVo==null){
            throw new IllegalArgumentException("请先调用page()函数!");
        }
        List<T> list = getList();
        pageVo.setList(list);
        pageVo.setTotalSize(count());
        pageVo.setTotalPage((int)(pageVo.getTotalSize()/pageVo.getPageSize())+1);
        pageVo.setHasMore(pageVo.getCurrentPage()<pageVo.getTotalPage());
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
        sqlBuilder.append("select "+SQLUtil.columns(_class,"t"));
        for(AbstractSubCondition subCondition:subConditionList){
            sqlBuilder.append(","+SQLUtil.columns(subCondition._class,subCondition.tableAliasName));
        }
        sqlBuilder.append(" from "+tableName+" as t ");
        addJoinTableStatement();
        sqlBuilder.append(groupByBuilder.toString()+" "+havingBuilder.toString()+" "+orderByBuilder.toString()+" "+limit);
        sql = sqlBuilder.toString().replaceAll("\\s+"," ");

        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);){
            for(int i=0;i<parameterList.size();i++){
                ps.setObject((i+1),parameterList.get(i));
                replaceParameter(parameterList.get(i));
            }
            addJoinTableParamters(ps);
            logger.debug("[getList]执行SQL:{}",sql);
            ResultSet resultSet = ps.executeQuery();
            JSONArray array = new JSONArray((int)count());
            while(resultSet.next()){
                JSONObject o = new JSONObject();
                Field[] fields = ReflectionUtil.getFields(_class);
                for(Field field:fields){
                    if(field.getAnnotation(Ignore.class)!=null){
                        continue;
                    }
                    String columnName = "t_"+StringUtil.Camel2Underline(field.getName());
                    String type = field.getType().getSimpleName().toLowerCase();
                    //根据类型进行映射
                    switch(type){
                        case "int":{o.put(field.getName(),resultSet.getInt(columnName));}break;
                        case "integer":{o.put(field.getName(),resultSet.getInt(columnName));}break;
                        case "long":{o.put(field.getName(),resultSet.getLong(columnName));};break;
                        case "boolean":{
                            o.put(field.getName(),resultSet.getBoolean(columnName));
                        };break;
                        case "date":{
                            o.put(field.getName(),resultSet.getDate(columnName));
                        };break;
                        default:{
                            o.put(field.getName(),resultSet.getObject(columnName));
                        }
                    }
                }

                for(AbstractCondition.AbstractSubCondition subCondition:subConditionList){
                    JSONObject subObject = new JSONObject();
                    fields = ReflectionUtil.getFields(subCondition._class);
                    for(Field field:fields){
                        if(field.getAnnotation(Ignore.class)!=null){
                            continue;
                        }
                        String plainColumnName = StringUtil.Camel2Underline(field.getName());
                        String columnName = subCondition.tableAliasName+"_"+plainColumnName;
                        String type = field.getType().getSimpleName().toLowerCase();
                        //根据类型进行映射
                        switch(type){
                            case "int":{subObject.put(plainColumnName,resultSet.getInt(columnName));}break;
                            case "integer":{subObject.put(plainColumnName,resultSet.getInt(columnName));}break;
                            case "long":{subObject.put(plainColumnName,resultSet.getLong(columnName));};break;
                            case "boolean":{
                                subObject.put(plainColumnName,resultSet.getBoolean(columnName));
                            };break;
//                            case "date":{
//                                subObject.put(plainColumnName,resultSet.getDate(columnName));
//                            };break;
                            default:{
                                subObject.put(plainColumnName,resultSet.getObject(columnName));
                            }
                        }
                    }
                    o.put(StringUtil.Camel2Underline(subCondition._class.getSimpleName()),subObject);
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
    public List<T> getValueList(Class<T> _class, String column) {
        assureDone();
        sqlBuilder.setLength(0);
        sqlBuilder.append("select "+(columnBuilder.length()>0?columnBuilder.toString():"t.`"+column+"`")+" from "+tableName+" as t ");
        addJoinTableStatement();
        sqlBuilder.append(groupByBuilder.toString()+" "+havingBuilder.toString()+" "+orderByBuilder.toString()+" "+limit);
        sql = sqlBuilder.toString().replaceAll("\\s+"," ");

        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);){
            for(int i=0;i<parameterList.size();i++){
                ps.setObject((i+1),parameterList.get(i));
                replaceParameter(parameterList.get(i));
            }
            addJoinTableParamters(ps);
            logger.debug("[getValueList]执行SQL:{}",sql);
            ResultSet resultSet = ps.executeQuery();
            List<T> instanceList = new ArrayList((int) count());
            ReflectionUtil.mappingResultToList(resultSet,instanceList,_class,column);
            ps.close();
            return instanceList;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**添加外键关联查询条件*/
    private void addJoinTableStatement() {
        for (AbstractSubCondition abstractSubCondition : subConditionList) {
            sqlBuilder.append("join `" + SQLUtil.classTableMap.get(abstractSubCondition._class) + "` as " + abstractSubCondition.tableAliasName + " on t." + StringUtil.Camel2Underline(abstractSubCondition.primaryField) + " = " + StringUtil.Camel2Underline(abstractSubCondition.tableAliasName) + "." + abstractSubCondition.joinTableField + " ");
        }
        //添加查询条件
        sqlBuilder.append(whereBuilder.toString());
        for (AbstractSubCondition abstractSubCondition : subConditionList) {
            if (abstractSubCondition.whereBuilder.length() > 0) {
                sqlBuilder.append(" and " + abstractSubCondition.whereBuilder.toString() + " ");
            }
        }
    }

    /**添加外键查询参数*/
    private void addJoinTableParamters(PreparedStatement ps) throws SQLException {
        int parameterIndex = parameterList.size()+1;
        for (AbstractSubCondition abstractSubCondition : subConditionList) {
            for(Object parameter:abstractSubCondition.parameterList){
                ps.setObject(parameterIndex++,parameter);
                replaceParameter(parameter);
            }
        }
    }

    /**替换查询参数*/
    protected void replaceParameter(Object parameter){
        String type = parameter.getClass().getSimpleName().toLowerCase();
        switch(type){
            case "int":{};
            case "integer":{};
            case "long":{} ;
            case "boolean": {
                sql = sql.replaceFirst("\\?",parameter.toString());
            }break;
            case "string": {
                sql = sql.replaceFirst("\\?","'"+parameter.toString()+"'");
            }break;
            default: {
                sql = sql.replaceFirst("\\?",parameter.toString());
            }
        }
    }

    /**确保执行了done方法*/
    private void assureDone(){
        if(!hasDone){
            done();
        }
    }

    class AbstractSubCondition<T> implements SubCondition<T> {
        private Class<T> _class;
        private String tableAliasName;
        private String primaryField;
        private String joinTableField;
        private StringBuilder whereBuilder = new StringBuilder();
        private List parameterList = new ArrayList();
        private Condition condition;

        public AbstractSubCondition(Class<T> _class, String tableAliasName, String primaryField, String joinTableField, Condition condition) {
            this._class = _class;
            this.tableAliasName = tableAliasName;
            this.primaryField = primaryField;
            this.joinTableField = joinTableField;
            this.condition = condition;
        }

        @Override
        public SubCondition addNullQuery(String field) {
            whereBuilder.append("("+tableAliasName+".`"+ StringUtil.Camel2Underline(field)+"` is null or "+tableAliasName+".`"+StringUtil.Camel2Underline(field)+"` = '') and ");
            return this;
        }

        @Override
        public SubCondition addNotNullQuery(String field) {
            whereBuilder.append("("+tableAliasName+".`"+StringUtil.Camel2Underline(field)+"` is not null and "+tableAliasName+".`"+StringUtil.Camel2Underline(field)+"` != '') and ");
            return this;
        }

        @Override
        public SubCondition addInQuery(String field, Object[] values) {
            if(values==null||values.length==0){
                return this;
            }
            parameterList.addAll(Arrays.asList(values));
            whereBuilder.append("("+tableAliasName+"."+StringUtil.Camel2Underline(field)+" in (");
            for(int i=0;i<values.length;i++){
                whereBuilder.append("?,");
            }
            whereBuilder.deleteCharAt(whereBuilder.length()-1);
            whereBuilder.append(") ) and ");
            return this;
        }

        @Override
        public SubCondition addInQuery(String field, List values) {
            return addInQuery(field,values.toArray(new Object[values.size()]));
        }

        @Override
        public SubCondition addQuery(String query) {
            whereBuilder.append("("+query+") and ");
            return this;
        }

        @Override
        public SubCondition addQuery(String property, Object value) {
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
        public SubCondition addQuery(String property, String operator, Object value) {
            if(value instanceof String){
                whereBuilder.append("("+tableAliasName+".`"+StringUtil.Camel2Underline(property)+"` "+operator+" ?) and ");
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
                whereBuilder.append("("+tableAliasName+".`"+property+"` "+operator+" ?) and ");
                parameterList.add(value);
            }
            return this;
        }

        @Override
        public Condition done() {
            return this.condition;
        }
    }
}