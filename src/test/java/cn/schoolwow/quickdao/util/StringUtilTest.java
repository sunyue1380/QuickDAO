package cn.schoolwow.quickdao.util;

import org.junit.Test;

public class StringUtilTest {

    @Test
    public void camel2Underline() {
    }

    @Test
    public void underline2Camel() {
        System.out.println(StringUtil.Underline2Camel("t_username"));
    }
}