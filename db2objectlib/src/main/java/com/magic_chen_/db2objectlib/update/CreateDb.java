package com.magic_chen_.db2objectlib.update;

import org.w3c.dom.Element;

import java.util.List;

public class CreateDb {
    /**
     * 数据库表名
     */
    private String name;

    private List<String> sqlCreates;

    public String getName() {
        return name;
    }

    public List<String> getSqlCreates() {
        return sqlCreates;
    }

    public CreateDb(Element element) {
        name = element.getAttribute("name");
    }
}
