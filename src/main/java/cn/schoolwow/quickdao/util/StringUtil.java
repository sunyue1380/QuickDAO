package cn.schoolwow.quickdao.util;

public class StringUtil {
    /**驼峰命名转下划线命名*/
    public static String Camel2Underline(String s){
        StringBuilder sb = new StringBuilder();
        for(int i=0;i<s.length();i++){
            if(i==0&&s.charAt(i)>=65&&s.charAt(i)<=90){
                sb.append((char)(s.charAt(i)+32));
                continue;
            }
            if(s.charAt(i)>=65&&s.charAt(i)<=90){
                //如果它前面是小写字母
                if(s.charAt(i-1)>=97&&s.charAt(i-1)<=122){
                    sb.append("_");
                }
                sb.append((char)(s.charAt(i)+32));
            }else{
                sb.append(s.charAt(i));
            }
        }
        return sb.toString();
    }
}
