package cn.schoolwow.quickdao.syntax;

public class MySQLSyntaxHandler extends AbstractSyntaxHandler {
    @Override
    public String getSyntax(Syntax syntax, Object... values) {
        switch (syntax) {
            case AutoIncrement: {
                return "auto_increment";
            }
            case InsertIgnore: {
                return "insert ignore into ";
            }
            case Comment: {
                return "comment \"" + values[0] + "\"";
            }
            case Escape: {
                return "`" + values[0] + "`";
            }
            default:
                return "";
        }
    }
}
