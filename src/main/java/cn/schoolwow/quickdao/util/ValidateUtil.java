package cn.schoolwow.quickdao.util;

import java.util.List;

/**参数校验工具类*/
public class ValidateUtil {
    public static boolean isNull(Object object){
        return object==null;
    }
    public static boolean isNotNull(Object object){
        return object!=null;
    }

    public static boolean isEmpty(String s){
        return s==null||s.length()==0;
    }
    public static boolean isNotEmpty(String s){
        return s!=null&&s.length()>0;
    }

    public static boolean isEmpty(Object[] array){
        return array==null||array.length==0;
    }
    public static boolean isNotEmpty(Object[] array){
        return array!=null&&array.length>0;
    }

    public static boolean isEmpty(List list){
        return list==null||list.size()==0;
    }
    public static boolean isNotEmpty(List list){
        return list!=null&&list.size()>0;
    }
}
