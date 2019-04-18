package cn.schoolwow.quickdao.dao;

import javax.sql.DataSource;
import java.util.List;

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

    @Override
    protected String getUniqueStatement(String tableName, List<String> columns) {
        StringBuilder uniqueSQLBuilder = new StringBuilder("alter table `"+tableName+"` add unique index `"+tableName+"_");
        columns.stream().forEach((column)->{
            uniqueSQLBuilder.append(column+"_");
        });
        uniqueSQLBuilder.append("unique_index` (");
        columns.stream().forEach((column)->{
            uniqueSQLBuilder.append("`"+column+"`,");
        });
        uniqueSQLBuilder.deleteCharAt(uniqueSQLBuilder.length()-1);
        uniqueSQLBuilder.append(");");
        return uniqueSQLBuilder.toString();
    }
}
