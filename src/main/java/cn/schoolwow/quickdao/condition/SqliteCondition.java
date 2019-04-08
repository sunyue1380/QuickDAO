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
    public Condition addUpdate(String property, Object value) {
        if(updateParameterList==null){
            updateParameterList = new ArrayList();
        }
        setBuilder.append("`"+ StringUtil.Camel2Underline(property)+"`=?,");
        updateParameterList.add(value);
        return this;
    }

    @Override
    public long update() {
        if(!hasDone){
            done();
        }
        if(setBuilder.length()==0){
            logger.warn("请先调用addUpdate方法!");
            return 0;
        }
        sqlBuilder.setLength(0);
        sqlBuilder.append("update "+tableName+" "+setBuilder.toString()+" ");
        //添加查询条件
        sqlBuilder.append(whereBuilder.toString().replaceAll("t\\.",""));
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
