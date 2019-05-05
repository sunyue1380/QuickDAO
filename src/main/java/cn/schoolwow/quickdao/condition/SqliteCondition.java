package cn.schoolwow.quickdao.condition;

import cn.schoolwow.quickdao.util.StringUtil;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;

public class SqliteCondition extends AbstractCondition{
    public SqliteCondition(Class _class, DataSource dataSource) {
        super(_class, dataSource);
    }

    @Override
    public Condition addUpdate(String field, Object value) {
        if(updateParameterList==null){
            updateParameterList = new ArrayList();
        }
        setBuilder.append("`"+ StringUtil.Camel2Underline(field)+"`=?,");
        updateParameterList.add(value);
        return this;
    }

    @Override
    public long update() {
        assureDone();
        if(setBuilder.length()==0){
            throw new IllegalArgumentException("请先调用addUpdate()函数!");
        }
        if(updateParameterList==null||updateParameterList.size()==0){
            throw new IllegalArgumentException("请先调用addUpdate()函数!");
        }
        sqlBuilder.setLength(0);
        sqlBuilder.append("update "+tableName+" ");
        sqlBuilder.append(setBuilder.toString()+" ");
        sqlBuilder.append(whereBuilder.toString());
        sql = sqlBuilder.toString().replace("t."," ").replaceAll("\\s+"," ");
        logger.info("[批量更新]执行SQL语句:{}",sql);

        long effect = -1;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);){
            for(Object parameter:updateParameterList){
                ps.setObject(parameterIndex++,parameter);
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
        if(!hasDone){
            done();
        }
        sqlBuilder.setLength(0);
        sqlBuilder.append("delete from "+tableName+" ");
        sqlBuilder.append(whereBuilder.toString().replaceAll("t\\.",""));
        sql = sqlBuilder.toString().replaceAll("\\s+"," ");

        long effect = -1;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);){
            for(int i=0;i<parameterList.size();i++){
                ps.setObject((i+1),parameterList.get(i));
                replaceParameter(parameterList.get(i));
            }
            logger.debug("[Delete]执行SQL:{}",sql);
            effect = ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return effect;
    }
}
