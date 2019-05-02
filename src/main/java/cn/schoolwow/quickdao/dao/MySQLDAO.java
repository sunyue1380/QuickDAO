package cn.schoolwow.quickdao.dao;

import javax.sql.DataSource;

public class MySQLDAO extends AbstractDAO{
    public MySQLDAO(DataSource dataSource) {
        super(dataSource);
        fieldMapping.put("long","INTEGER");
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
            case Comment:{
                return "comment \""+values[0]+"\"";
            }
            default:return "";
        }
    }
}
