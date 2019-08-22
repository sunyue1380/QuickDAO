package cn.schoolwow.quickdao.condition;

import cn.schoolwow.quickdao.dao.AbstractDAO;
import cn.schoolwow.quickdao.util.StringUtil;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;

public class SqliteCondition extends AbstractCondition{
    public SqliteCondition(Class _class, DataSource dataSource, AbstractDAO abstractDAO) {
        super(_class, dataSource,abstractDAO);
    }

    @Override
    public Condition addUpdate(String field, Object value) {
        if(query.updateParameterList==null){
            query.updateParameterList = new ArrayList();
        }
        query.setBuilder.append("`"+ StringUtil.Camel2Underline(field)+"`=?,");
        query.updateParameterList.add(value);
        return this;
    }

    @Override
    public long update() {
        assureDone();
        assureUpdate();
        sqlBuilder.setLength(0);
        sqlBuilder.append("update "+query.tableName+" ");
        sqlBuilder.append(query.setBuilder.toString()+" ");
        sqlBuilder.append(query.whereBuilder.toString());
        sql = sqlBuilder.toString().replace("t."," ").replaceAll("\\s+"," ");
        logger.debug("[批量更新]执行SQL语句:{}",sql);

        long effect = -1;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);){
            for(Object parameter:query.updateParameterList){
                ps.setObject(query.parameterIndex++,parameter);
                replaceParameter(parameter);
            }
            addMainTableParameters(ps);
            logger.debug("[Update]执行SQL:{}",sql);
            effect = ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return effect;
    }

    @Override
    public long delete() {
        if(!query.hasDone){
            done();
        }
        sqlBuilder.setLength(0);
        sqlBuilder.append("delete from "+query.tableName+" ");
        sqlBuilder.append(query.whereBuilder.toString().replaceAll("t\\.",""));
        sql = sqlBuilder.toString().replaceAll("\\s+"," ");

        long effect = -1;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);){
            for(int i=0;i<query.parameterList.size();i++){
                ps.setObject((i+1),query.parameterList.get(i));
                replaceParameter(query.parameterList.get(i));
            }
            logger.debug("[Delete]执行SQL:{}",sql);
            effect = ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return effect;
    }
}
