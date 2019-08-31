package cn.schoolwow.quickdao.syntax;

public class SQLiteSyntaxHandler extends AbstractSyntaxHandler {
    @Override
    public String getSyntax(Syntax syntax, Object... values) {
        switch (syntax) {
            case AutoIncrement: {
                return "autoincrement";
            }
            case InsertIgnore: {
                return "insert or ignore into ";
            }
            case Comment: {
                return "/*" + values[0] + "*/";
            }
            case Escape: {
                return "`" + values[0] + "`";
            }
            default:
                return "";
        }
    }
}
