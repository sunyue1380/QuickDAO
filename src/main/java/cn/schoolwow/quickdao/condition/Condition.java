package cn.schoolwow.quickdao.condition;

import cn.schoolwow.quickdao.domain.PageVo;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.util.List;

public interface Condition<T> {
    Condition distinct();
    /**添加空查询*/
    Condition addNullQuery(String field);
    /**添加非空查询*/
    Condition addNotNullQuery(String field);
    /**添加非空查询*/
    Condition addNotEmptyQuery(String field);
    /**添加范围语句*/
    Condition addInQuery(String field, Object[] values);
    /**添加范围语句*/
    Condition addInQuery(String field, List values);
    /**添加范围语句*/
    Condition addNotInQuery(String field, Object[] values);
    /**添加范围语句*/
    Condition addNotInQuery(String field, List values);
    /**添加自定义查询条件*/
    Condition addQuery(String query);
    /**添加属性查询*/
    Condition addQuery(String property, Object value);
    /**添加属性查询*/
    Condition addQuery(String property, String operator, Object value);
    /**添加实体属性查询*/
    Condition addInstanceQuery(Object instance);
    /**添加实体属性
     * @param userBasicDataType 是否使用基本属性类型进行查询*/
    Condition addInstanceQuery(Object instance,boolean userBasicDataType);
    /**添加自定义查询条件*/
    Condition addQuery(JSONObject queryCondition);
    /**添加更新字段*/
    Condition addUpdate(String property, Object value);
    /**添加聚合字段
     * @param aggerate COUNT,SUM,MAX,MIN,AVG
     * @param field 字段名
     * */
    Condition addAggerate(String aggerate,String field);
    /**添加聚合字段*/
    Condition addAggerate(String aggerate,String field,String alias);
    /**分组*/
    Condition groupBy(String field);
//    /**分组过滤*/
//    Condition having(String query);
    /**关联表*/
    <T> SubCondition<T> joinTable(Class<T> _class, String primaryField, String joinTableField);

    /**排序字段(升序)*/
    Condition orderBy(String field);
    /**排序字段(降序)*/
    Condition orderByDesc(String field);
    /**分页操作*/
    Condition limit(long offset, long limit);
    /**分页操作*/
    Condition page(int pageNum,int pageSize);
    Condition addColumn(String field);

    long count();
    long update();
    long delete();
    List<T> getList();
    PageVo<T> getPagingList();
    /**获取分页列表
     * @param composit 是否返回复杂对象信息*/
    PageVo<T> getPagingList(boolean composit);
    /**获取复合列表(即返回关联表字段)*/
    List<T> getCompositList();
    JSONArray getCompositArray();
    /**获取部分列*/
    List<T> getPartList();
    List<T> getValueList(Class<T> _class, String column);

    JSONArray getAggerateList();
}
