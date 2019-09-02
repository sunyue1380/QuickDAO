package cn.schoolwow.quickdao.entity.logic;

import cn.schoolwow.quickdao.annotation.ColumnName;
import cn.schoolwow.quickdao.annotation.TableName;

@TableName("consumer")
public class Customer {
    private long id;
    @ColumnName("quality")
    private String order;
    @ColumnName("park")
    private String garden;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getOrder() {
        return order;
    }

    public void setOrder(String order) {
        this.order = order;
    }

    public String getGarden() {
        return garden;
    }

    public void setGarden(String garden) {
        this.garden = garden;
    }
}
