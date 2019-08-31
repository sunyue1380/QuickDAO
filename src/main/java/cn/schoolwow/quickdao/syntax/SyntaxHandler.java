package cn.schoolwow.quickdao.syntax;

/**
 * 数据库差异语法
 */
public interface SyntaxHandler {
    /**
     * 获取对应语法
     */
    String getSyntax(Syntax syntax, Object... values);
}
