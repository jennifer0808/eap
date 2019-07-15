package cn.tzauto.octopus.isecsLayer.resolver.cohu;

/**
 * Created by wj_co
 */
public class NY20POJO {

    private  String name;
    private  String nominalValue;
    private  String LowLimitFail;
    private  String HighLimitFail;
    private  String BasicItem;
    private  String  unit;

    public NY20POJO(String name, String nominalValue, String lowLimitFail, String highLimitFail, String basicItem, String unit) {
        this.name = name;
        this.nominalValue = nominalValue;
        LowLimitFail = lowLimitFail;
        HighLimitFail = highLimitFail;
        BasicItem = basicItem;
        this.unit = unit;
    }

    public NY20POJO() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNominalValue() {
        return nominalValue;
    }

    public void setNominalValue(String nominalValue) {
        this.nominalValue = nominalValue;
    }

    public String getLowLimitFail() {
        return LowLimitFail;
    }

    public void setLowLimitFail(String lowLimitFail) {
        LowLimitFail = lowLimitFail;
    }

    public String getHighLimitFail() {
        return HighLimitFail;
    }

    public void setHighLimitFail(String highLimitFail) {
        HighLimitFail = highLimitFail;
    }

    public String getBasicItem() {
        return BasicItem;
    }

    public void setBasicItem(String basicItem) {
        BasicItem = basicItem;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    @Override
    public String toString() {
        return "NY20POJO{" +
                "name='" + name + '\'' +
                ", nominalValue='" + nominalValue + '\'' +
                ", LowLimitFail='" + LowLimitFail + '\'' +
                ", HighLimitFail='" + HighLimitFail + '\'' +
                ", BasicItem='" + BasicItem + '\'' +
                ", unit='" + unit + '\'' +
                '}';
    }
}
