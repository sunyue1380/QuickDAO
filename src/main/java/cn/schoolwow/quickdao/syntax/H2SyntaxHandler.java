package cn.schoolwow.quickdao.syntax;

public class H2SyntaxHandler extends MySQLSyntaxHandler {
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
                return "";
            }
            case Escape: {
                return "`" + values[0] + "`";
            }
            default:
                return "";
        }
    }
}
