package cn.schoolwow.quickdao.util;

import cn.schoolwow.quickdao.entity.user.User;
import com.alibaba.fastjson.JSON;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class StringUtilTest {

    @Test
    public void camel2Underline() {
    }

    @Test
    public void underline2Camel() {
        System.out.println(StringUtil.Underline2Camel("t_username"));
    }
}