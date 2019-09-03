package cn.schoolwow.quickdao.syntax;

public class SQLServerSyntaxHandler extends PostgreSyntaxHandler{
    @Override
    public String getSyntax(Syntax syntax, Object... values) {
        switch (syntax) {
            case AutoIncrement: {
                return "";
            }
            case InsertIgnore: {
                return "insert into ";
            }
            case Comment: {
                return "";
            }
            case Escape: {
                return "\"" + values[0] + "\"";
            }
            default:
                return "";
        }
    }
}
