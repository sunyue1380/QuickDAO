package cn.schoolwow.quickdao.condition;

import cn.schoolwow.quickdao.dao.AbstractDAO;
import cn.schoolwow.quickdao.helper.SQLHelper;
import cn.schoolwow.quickdao.syntax.SyntaxHandler;

import javax.sql.DataSource;

public class H2Condition extends MySQLCondition {
    public H2Condition(Class _class, DataSource dataSource, AbstractDAO abstractDAO, SyntaxHandler syntaxHandler, SQLHelper sqlHelper) {
        super(_class, dataSource, abstractDAO, syntaxHandler, sqlHelper);
    }
}
