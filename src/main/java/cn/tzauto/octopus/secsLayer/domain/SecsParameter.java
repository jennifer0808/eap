package cn.tzauto.octopus.secsLayer.domain;

/**
 * Created by luosy
 */
public class SecsParameter {

    private String type;
    private String name;
    private String code;
    private String id;
    private String value;
    private short format;
    private Judge judge;

    public SecsParameter(String type, String name, String code, String id, String value, short format, Judge judge) {
        this.type = type;
        this.name = name;
        this.code = code;
        this.id = id;
        this.value = value;
        this.judge = judge;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public short getFormat() {
        return format;
    }

    public void setFormat(short format) {
        this.format = format;
    }

    public Judge getJudge() {
        return judge;
    }

    public void setJudge(Judge judge) {
        this.judge = judge;
    }
}
