package cn.schoolwow.quickdao.condition;

import cn.schoolwow.quickdao.dao.AbstractDAO;
import cn.schoolwow.quickdao.domain.PageVo;
import cn.schoolwow.quickdao.helper.SQLHelper;
import cn.schoolwow.quickdao.syntax.SyntaxHandler;

import javax.sql.DataSource;

public class PostgreCondition extends SQLiteCondition {
    public PostgreCondition(Class _class, DataSource dataSource, AbstractDAO abstractDAO, SyntaxHandler syntaxHandler, SQLHelper sqlHelper) {
        super(_class, dataSource, abstractDAO, syntaxHandler, sqlHelper);
    }

    @Override
    public Condition limit(long offset, long limit) {
        query.limit = "limit " + limit + " offset " + offset;
        return this;
    }

    @Override
    public Condition page(int pageNum, int pageSize) {
        query.limit = "limit " + pageSize + " offset " + (pageNum - 1) * pageSize;
        pageVo = new PageVo<>();
        pageVo.setPageSize(pageSize);
        pageVo.setCurrentPage(pageNum);
        return this;
    }
}
