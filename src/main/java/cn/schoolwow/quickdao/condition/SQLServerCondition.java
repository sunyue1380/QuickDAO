package cn.schoolwow.quickdao.condition;

import cn.schoolwow.quickdao.dao.AbstractDAO;
import cn.schoolwow.quickdao.domain.PageVo;
import cn.schoolwow.quickdao.helper.SQLHelper;
import cn.schoolwow.quickdao.syntax.Syntax;
import cn.schoolwow.quickdao.syntax.SyntaxHandler;
import cn.schoolwow.quickdao.util.StringUtil;

import javax.sql.DataSource;

public class SQLServerCondition extends SQLiteCondition{
    public SQLServerCondition(Class _class, DataSource dataSource, AbstractDAO abstractDAO, SyntaxHandler syntaxHandler, SQLHelper sqlHelper) {
        super(_class, dataSource, abstractDAO, syntaxHandler, sqlHelper);
    }

    @Override
    public Condition addLikeQuery(String field, Object value) {
        if (value == null || value.toString().equals("")) {
            return this;
        }
        query.whereBuilder.append("charindex(?,t."+query.syntaxHandler.getSyntax(Syntax.Escape, StringUtil.Camel2Underline(field))+" ) >0 and ");
        query.parameterList.add(value.toString());
        return this;
    }

    @Override
    public Condition limit(long offset, long limit) {
        if(query.orderByBuilder.length()==0){
            throw new IllegalArgumentException("SQL Server的分页操作必须包含order子句!");
        }
        query.limit = "offset "+offset+" rows " + " fetch next "+limit+" rows only";
        return this;
    }

    @Override
    public Condition page(int pageNum, int pageSize) {
        if(query.orderByBuilder.length()==0){
            throw new IllegalArgumentException("SQL Server的分页操作必须包含order子句!");
        }
        query.limit = "offset "+(pageNum - 1) * pageSize+" rows " + " fetch next "+pageSize+" rows only";
        pageVo = new PageVo<>();
        pageVo.setPageSize(pageSize);
        pageVo.setCurrentPage(pageNum);
        return this;
    }
}
