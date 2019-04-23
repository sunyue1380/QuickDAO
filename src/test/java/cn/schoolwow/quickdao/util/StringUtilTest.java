package cn.schoolwow.quickdao.util;

import org.junit.Test;

import static org.junit.Assert.*;

public class StringUtilTest {

    @Test
    public void camel2Underline() {
    }

    @Test
    public void underline2Camel() {
        String s = "last_login";
        System.out.println(StringUtil.Underline2Camel(s));
    }
}