package com.schoolwow.quickdao.dao;

import java.util.List;

public interface Response<T> {
    long count();
    long delete();
    List<T> getList();
    List<T> getValueList(Class<T> _class, String column);

    boolean hasNext();
    void next();
    T getValue(Class<T> _class, String column);
}
