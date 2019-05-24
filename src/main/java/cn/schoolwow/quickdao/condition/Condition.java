package cn.schoolwow.quickdao.condition;

import cn.schoolwow.quickdao.domain.PageVo;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.util.List;

/**
 * 查询条件接口
 * */
public interface Condition<T> {
    /**添加distinct语句*/
    Condition distinct();
    /**
     * 添加空查询
     * @param field 指明哪个字段为Null
     * */
    Condition addNullQuery(String field);
    /**
     * 添加非空查询
     * @param field 指明哪个字段不为Null
     * */
    Condition addNotNullQuery(String field);
    /**
     * 添加非空查询
     * @param field 指明哪个字段不为空字符串
     * */
    Condition addNotEmptyQuery(String field);
    /**
     * 添加范围查询语句
     * @param field 字段
     * @param values 指明在该范围内的值
     * */
    Condition addInQuery(String field, Object[] values);
    /**
     * 添加范围查询语句
     * @param field 字段
     * @param values 指明在该范围内的值
     * */
    Condition addInQuery(String field, List values);
    /**
     * 添加范围查询语句
     * @param field 字段
     * @param values 指明在不该范围内的值
     * */
    Condition addNotInQuery(String field, Object[] values);
    /**
     * 添加范围查询语句
     * @param field 字段
     * @param values 指明在不该范围内的值
     * */
    Condition addNotInQuery(String field, List values);
    /**
     * 添加between语句
     * @param field 字段
     * @param start 范围开始值
     * @param end 范围结束值
     * */
    Condition addBetweenQuery(String field, Object start,Object end);
    /**
     * 添加自定义查询条件
     * @param query 子查询条件(<b>主表</b>统一别名为t)
     * */
    Condition addQuery(String query);
    /**
     * 添加字段查询
     * @param field 指明字段
     * @param value 字段值
     * */
    Condition addQuery(String field, Object value);
    /**
     * 添加字段查询
     * @param field 指明字段
     * @param operator 操作符,可为<b>></b>,<b>>=</b>,<b>=</b>,<b><</b><b><=</b>
     * @param value 字段值
     * */
    Condition addQuery(String field, String operator, Object value);
    /**
     * 添加自定义查询条件<br/>
     * <code>
     *     {<br/>
     *         {field}:{value},字段查询<br/>
     *         {field}Start:{value},添加大于等于查询<br/>
     *         {field}End:{value},添加小于等于查询<br/>
     *         {field}IN:[array],添加IN查询<br/>
     *         {field}NOTNULL:{value},添加not null查询<br/>
     *         {field}NULL:{value},添加null查询<br/>
     *         _orderBy:{value},升序排列<br/>
     *         _orderByDesc:{value},降序排列<br/>
     *         _pageNumber:{value},页码<br/>
     *         _pageSize:{value},每页个数<br/>
     *     }<br/>
     * </code>
     * */
    Condition addJSONObjectQuery(JSONObject queryCondition);
    /**
     * 添加更新字段,用于<b>{@link Condition#update()}</b>方法
     * @param field 待更新的字段
     * @param value 待更新字段的值
     * */
    Condition addUpdate(String field, Object value);
    /**
     * <p>添加聚合字段,默认别名为<b>{@link Condition#getAggerateList()}</b></p>
     * <p>用于<b>getAggerateList()</b>方法</p>
     * @param aggerate COUNT,SUM,MAX,MIN,AVG
     * @param field 字段名
     * */
    Condition addAggerate(String aggerate,String field);
    /**
     * 添加聚合字段
     * @param aggerate COUNT,SUM,MAX,MIN,AVG
     * @param field 字段名
     * @param alias 聚合字段别名
     * */
    Condition addAggerate(String aggerate,String field,String alias);
    /**
     * 添加分组查询
     * @param field 分组字段
     * */
    Condition groupBy(String field);
//    /**分组过滤*/
//    Condition having(String query);
    /**
     * 关联表
     * <p><b>主表,默认别名为t</b>(即query方法传递的类对象)</p>
     * <p><b>子表,第一次调用JoinTable方法关联的表别名为t1，第二个为t2,以此类推</b></p>
     * @param _class 要关联的表
     * @param primaryField <b>主表</b>关联字段
     * @param joinTableField <b>子表</b>关联字段
     * */
    <T> SubCondition<T> joinTable(Class<T> _class, String primaryField, String joinTableField);
    /**
     * 关联表
     * <p><b>主表,默认别名为t</b>(即query方法传递的类对象)</p>
     * <p><b>子表,第一次调用JoinTable方法关联的表别名为t1，第二个为t2,以此类推</b></p>
     * @param _class 要关联的表
     * @param primaryField <b>主表</b>关联字段
     * @param joinTableField <b>子表</b>关联字段
     * @param compositField <b>主表</b>关联字段对应复杂字段的字段名
     * */
    <T> SubCondition<T> joinTable(Class<T> _class, String primaryField, String joinTableField,String compositField);

    /**
     * 根据指定字段升序排列
     * @param field 升序排列字段名
     * */
    Condition orderBy(String field);
    /**
     * 根据指定字段降序排列
     * @param field 降序排列字段名
     * */
    Condition orderByDesc(String field);
    /**
     * 分页操作
     * @param offset 偏移量
     * @param limit 返回个数
     * */
    Condition limit(long offset, long limit);
    /**
     * 分页操作
     * @param pageNum 第几页
     * @param pageSize 每页个数
     * */
    Condition page(int pageNum,int pageSize);
    /**
     * 添加待查询字段,用于<b>{@link Condition#getPartList()}</b>
     * @param field 要返回的字段
     * */
    Condition addColumn(String field);

    /**
     * 获取符合条件的总数目
     * */
    long count();
    /**
     * <p>更新符合条件的记录</p>
     * <p><b>前置条件</b>:请先调用<b>{@link Condition#addUpdate(String, Object)}</b>方法</p>
     * */
    long update();
    /**
     * 删除符合条件的数据库记录
     * */
    long delete();
    /**
     * <p>获取符合条件的数据库记录的第一条</p>
     * <p>若无符合条件的数据库记录,返回Null</p>
     * */
    T getOne();
    /**
     * 返回符合条件的数据库记录
     * */
    List<T> getList();
    /**
     * 返回符合条件的数据库记录
     * */
    JSONArray getArray();
    /**
     * <p>返回指定单个字段的集合</p>
     * @param _class 返回字段类型
     * @param column 待返回的字段
     * */
    <E> List<E> getValueList(Class<E> _class, String column);
    /**
     * <p>返回聚合字段的数据库记录</p>
     * <p><b>前置条件</b>:请先调用{@link Condition#addAggerate(String, String)}</p>
     * <p>若调用了{@link Condition#addColumn(String)} 则会返回addColumn所指定的字段</p>
     * */
    JSONArray getAggerateList();
    /**
     * <p>返回指定的部分字段的数据库记录</p>
     * <p><b>前置条件</b>:请先调用<b>{@link Condition#addColumn(String)} </b></p>
     * */
    List<T> getPartList();
    /**
     * <p>返回符合条件的数据库分页记录</p>
     * <p><b>前置条件</b>:请先调用<b>{@link Condition#page(int, int)} </b>方法</p>
     * */
    PageVo<T> getPagingList();
    /**
     * <p>返回指定的部分字段的数据库记录</p>
     * <p><b>前置条件</b>:请先调用<b>{@link Condition#addColumn(String)} </b></p>
     * */
    PageVo<T> getPartPagingList();
    /**
     * <p>返回符合条件的数据库分页记录,同时返回关联查询方法({@link Condition#joinTable(Class, String, String)})所关联的字段信息</p>
     * <p><b>前置条件</b>:请先调用<b>{@link Condition#page(int, int)} </b>方法</p>
     * */
    PageVo<T> getCompositPagingList();
    /**
     * <p>返回符合条件的数据库记录,同时返回关联查询方法({@link Condition#joinTable(Class, String, String)})所关联的字段信息</p>
     * <p><b>注意</b>:若未调用过joinTable方法,则该方法不会返回复杂对象字段信息</p>
     * */
    List<T> getCompositList();
    /**
     * <p>返回符合条件的数据库记录,同时返回关联查询方法({@link Condition#joinTable(Class, String, String)})所关联的字段信息</p>
     * <p><b>注意</b>:若未调用过joinTable方法,则该方法不会返回复杂对象字段信息</p>
     * */
    JSONArray getCompositArray();
    /**
     * <p>克隆该Condition对象</p>
     * */
    Condition clone();
}
