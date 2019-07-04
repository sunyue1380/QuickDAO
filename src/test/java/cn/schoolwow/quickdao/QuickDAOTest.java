package cn.schoolwow.quickdao;

import cn.schoolwow.quickdao.dao.DAO;
import cn.schoolwow.quickdao.domain.Entity;
import cn.schoolwow.quickdao.util.ReflectionUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.dbcp.BasicDataSource;
import org.junit.runners.Parameterized;

import javax.sql.DataSource;
import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Scanner;

public class QuickDAOTest {
    protected DAO dao;

    @Parameterized.Parameters
    public static Collection prepareData(){
        BasicDataSource mysqlDataSource = new BasicDataSource();
        mysqlDataSource.setDriverClassName("com.mysql.jdbc.Driver");
        mysqlDataSource.setUrl("jdbc:mysql://127.0.0.1:3306/quickdao");
        mysqlDataSource.setUsername("root");
        mysqlDataSource.setPassword("123456");

        BasicDataSource sqliteDataSource = new BasicDataSource();
        sqliteDataSource.setDriverClassName("org.sqlite.JDBC");
        sqliteDataSource.setUrl("jdbc:sqlite:d:/db/quickdao_sqlite.db");

        BasicDataSource h2DataSource = new BasicDataSource();
        h2DataSource.setDriverClassName("org.h2.Driver");
        h2DataSource.setUrl("jdbc:h2:d:/db/quickdao_h2.db;mode=MYSQL");

        //各种数据库产品
        DataSource[] dataSources = {mysqlDataSource,sqliteDataSource,h2DataSource};
        Object[][] data = new Object[dataSources.length][1];
        for(int i=0;i<dataSources.length;i++){
            DAO dao = QuickDAO.newInstance().dataSource(dataSources[i])
                    .packageName("cn.schoolwow.quickdao.entity")
                    .packageName("cn.schoolwow.quickdao.domain","d")
                    .autoCreateTable(true)
                    .build();
            try {
                initialDatabase(dao);
            } catch (Exception e) {
                e.printStackTrace();
            }
            data[i][0] = dao;
        }
        return Arrays.asList(data);
    }

    protected static void initialDatabase(DAO dao) throws Exception{
        File file = new File("data.json");
        Scanner scanner = new Scanner(file);
        StringBuilder sb = new StringBuilder();
        while(scanner.hasNext()){
            sb.append(scanner.nextLine());
        }
        scanner.close();
        JSONArray array = JSON.parseArray(sb.toString());
        Collection<Entity> entityList = ReflectionUtil.entityMap.values();
        for(int i=0;i<array.size();i++){
            JSONObject o = array.getJSONObject(i);
            String tableName = o.getString("table");
            for(Entity entity:entityList){
                if(entity.tableName.equals(tableName)){
                    dao.drop(entity._class);
                    dao.create(entity._class);
                    List list = o.getJSONArray("rows").toJavaList(entity._class);
                    dao.save(list);
                    break;
                }
            }
        }
    }

    public QuickDAOTest(DAO dao){
        this.dao = dao;
    }
}
