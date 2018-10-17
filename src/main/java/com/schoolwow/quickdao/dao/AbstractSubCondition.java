package com.schoolwow.quickdao.dao;

import com.schoolwow.quickdao.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class AbstractSubCondition<T> implements SubCondition<T> {
    public Class<T> _class;
    public String tableAliasName;
    public String primaryField;
    public String joinTableField;
    public StringBuilder whereBuilder = new StringBuilder();
    public List parameterList = new ArrayList();
    private boolean hasDone = false;
    private static String[] patterns = new String[]{"%","_","[","[^","[!","]"};
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
