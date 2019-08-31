package cn.schoolwow.quickdao.condition.subCondition;

import cn.schoolwow.quickdao.condition.AbstractCondition;
import cn.schoolwow.quickdao.condition.Condition;
import cn.schoolwow.quickdao.domain.Query;
import cn.schoolwow.quickdao.domain.SubQuery;
import cn.schoolwow.quickdao.syntax.Syntax;
import cn.schoolwow.quickdao.util.ReflectionUtil;
import cn.schoolwow.quickdao.util.StringUtil;

import java.io.*;
import java.util.List;

public class AbstractSubCondition<T> implements SubCondition<T>, Serializable {
    public SubQuery subQuery;

    public AbstractSubCondition(Class<T> _class, String tableAliasName, String primaryField, String joinTableField, String compositField, AbstractCondition condition, Query query) {
        subQuery = new SubQuery();
        subQuery._class = _class;
        subQuery.className = _class.getName();
        subQuery.tableAliasName = tableAliasName;
        subQuery.primaryField = StringUtil.Camel2Underline(primaryField);
        subQuery.joinTableField = StringUtil.Camel2Underline(joinTableField);
        subQuery.compositField = StringUtil.Camel2Underline(compositField);
        subQuery.condition = condition;
        subQuery.query = query;
    }

    @Override
    public SubCondition leftJoin() {
        subQuery.join = "left outer join";
        return this;
    }

    @Override
    public SubCondition rightJoin() {
        subQuery.join = "right outer join";
        return this;
    }

    @Override
    public <T> SubCondition<T> joinTable(Class<T> _class, String primaryField, String joinTableField) {
        String fieldName = ReflectionUtil.getFirstClassFieldInMainClass(subQuery.className, _class.getName());
        AbstractSubCondition abstractSubCondition = (AbstractSubCondition) subQuery.condition.joinTable(_class, primaryField, joinTableField, fieldName);
        abstractSubCondition.subQuery.parentSubQuery = this.subQuery;
        abstractSubCondition.subQuery.parentSubCondition = this;
        return abstractSubCondition;
    }

    @Override
    public <T> SubCondition<T> joinTable(Class<T> _class, String primaryField, String joinTableField, String compositField) {
        AbstractSubCondition abstractSubCondition = (AbstractSubCondition) subQuery.condition.joinTable(_class, primaryField, joinTableField, compositField);
        abstractSubCondition.subQuery.parentSubQuery = this.subQuery;
        abstractSubCondition.subQuery.parentSubCondition = this;
        return abstractSubCondition;
    }

    @Override
    public SubCondition addNullQuery(String field) {
        subQuery.whereBuilder.append("(" + subQuery.tableAliasName + "." + subQuery.query.syntaxHandler.getSyntax(Syntax.Escape, StringUtil.Camel2Underline(field)) + " is null) and ");
        return this;
    }

    @Override
    public SubCondition addNotNullQuery(String field) {
        subQuery.whereBuilder.append("(" + subQuery.tableAliasName + "." + subQuery.query.syntaxHandler.getSyntax(Syntax.Escape, StringUtil.Camel2Underline(field)) + " is not null) and ");
        return this;
    }

    @Override
    public SubCondition addNotEmptyQuery(String field) {
        subQuery.whereBuilder.append("(" + subQuery.tableAliasName + "." + subQuery.query.syntaxHandler.getSyntax(Syntax.Escape, StringUtil.Camel2Underline(field)) + " is not null and " + subQuery.tableAliasName + "." + subQuery.query.syntaxHandler.getSyntax(Syntax.Escape, StringUtil.Camel2Underline(field)) + " != '') and ");
        return this;
    }

    @Override
    public SubCondition addInQuery(String field, Object[] values) {
        if (values == null || values.length == 0) {
            return this;
        }
        subQuery.condition.addINQuery(subQuery.tableAliasName, field, values, "in");
        return this;
    }

    @Override
    public SubCondition addInQuery(String field, List values) {
        return addInQuery(field, values.toArray(new Object[0]));
    }

    @Override
    public SubCondition addNotInQuery(String field, Object[] values) {
        if (values == null || values.length == 0) {
            return this;
        }
        subQuery.condition.addINQuery(subQuery.tableAliasName, field, values, "not in");
        return this;
    }

    @Override
    public SubCondition addNotInQuery(String field, List values) {
        return addNotInQuery(field, values.toArray(new Object[0]));
    }

    @Override
    public SubCondition addQuery(String query) {
        subQuery.whereBuilder.append("(" + query + ") and ");
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
            subQuery.whereBuilder.append("(" + subQuery.tableAliasName + "." + subQuery.query.syntaxHandler.getSyntax(Syntax.Escape, StringUtil.Camel2Underline(property)) + " " + operator + " ?) and ");
            boolean hasContains = false;
            for (String pattern : AbstractCondition.patterns) {
                if (((String) value).contains(pattern)) {
                    subQuery.parameterList.add(value);
                    hasContains = true;
                    break;
                }
            }
            if (!hasContains) {
                subQuery.parameterList.add("%" + value + "%");

            }
        } else {
            subQuery.whereBuilder.append("(" + subQuery.tableAliasName + "." + subQuery.query.syntaxHandler.getSyntax(Syntax.Escape, StringUtil.Camel2Underline(property)) + " " + operator + " ?) and ");
            subQuery.parameterList.add(value);
        }
        return this;
    }

    @Override
    public SubCondition orderBy(String field) {
        subQuery.query.orderByBuilder.append(subQuery.tableAliasName + "." + subQuery.query.syntaxHandler.getSyntax(Syntax.Escape, StringUtil.Camel2Underline(field)) + " asc,");
        return this;
    }

    @Override
    public SubCondition orderByDesc(String field) {
        subQuery.query.orderByBuilder.append(subQuery.tableAliasName + "." + subQuery.query.syntaxHandler.getSyntax(Syntax.Escape, StringUtil.Camel2Underline(field)) + " desc,");
        return this;
    }

    @Override
    public SubCondition doneSubCondition() {
        if (subQuery.parentSubCondition == null) {
            return this;
        } else {
            return subQuery.parentSubCondition;
        }
    }

    @Override
    public Condition done() {
        return subQuery.condition;
    }

    public SubCondition clone() {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(this);
            ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
            ObjectInputStream ois = new ObjectInputStream(bis);
            AbstractSubCondition subCondition = (AbstractSubCondition) ois.readObject();
            subCondition.subQuery.condition = this.subQuery.condition;
            return subCondition;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }
}
