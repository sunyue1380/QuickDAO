package cn.schoolwow.quickdao.syntax;

public class PostgreSyntaxHandler extends AbstractSyntaxHandler {
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
                return "comment \"" + values[0] + "\"";
            }
            case Escape: {
                return "\"" + values[0] + "\"";
            }
            default:
                return "";
        }
    }
}
