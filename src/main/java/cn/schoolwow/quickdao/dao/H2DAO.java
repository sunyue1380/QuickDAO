package cn.schoolwow.quickdao.dao;

import javax.sql.DataSource;

public class H2DAO extends MySQLDAO{
    public H2DAO(DataSource dataSource) {
        super(dataSource);
        fieldMapping.put("long","INTEGER");
        fieldMapping.put("float","REAL");
        fieldMapping.put("double", "DOUBLE");
    }

    @Override
    protected String getSyntax(Syntax syntax,Object... values) {
        switch(syntax){
            case AutoIncrement:{
                return "auto_increment";
            }
            case InsertIgnore:{
                return "insert ignore into ";
            }
            default:return "";
        }
    }
}
