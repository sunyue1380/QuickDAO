package cn.schoolwow.quickdao.domain;

/**外键级联策略*/
public enum ForeignKeyOption {
    RESTRICT("RESTRICT"),
    NOACTION("NO ACTION"),
    SETNULL("SET NULL"),
    CASCADE("CASCADE");

    private String operation;

    ForeignKeyOption(String operation){
        this.operation = operation;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }
}