package cn.tzauto.octopus.gui.widget.paraviewpane;

import javafx.beans.property.SimpleStringProperty;


public class SimpleRecipeParaProperty {

    private SimpleStringProperty paraCode;
    private SimpleStringProperty paraName;
    private SimpleStringProperty paraShotName;
    private SimpleStringProperty setValue;
    private SimpleStringProperty minValue;
    private SimpleStringProperty maxValue;
    private SimpleStringProperty paraMeasure;

    public SimpleRecipeParaProperty(){}

    public SimpleRecipeParaProperty(String paraCode,String paraName,String paraShotName,String setValue,String minValue,String maxValue,String paraMeasure) {
        this.paraCode = new SimpleStringProperty(paraCode);
        this.paraName = new SimpleStringProperty(paraShotName);
        this.paraShotName = new SimpleStringProperty(paraShotName);
        this.setValue = new SimpleStringProperty(setValue);
        this.minValue = new SimpleStringProperty(minValue);
        this.maxValue = new SimpleStringProperty(maxValue);
        this.paraMeasure = new SimpleStringProperty(paraMeasure);
    }

    public SimpleStringProperty getParaCode() {
        return paraCode;
    }
    public void setParaCode(String paraCode) {
        this.paraCode = new SimpleStringProperty(paraCode);
    }


    public SimpleStringProperty getParaName() {
        return paraName;
    }

    public void setParaName(String paraName) {
        this.paraName = new SimpleStringProperty(paraName);
    }


    public SimpleStringProperty getParaShotName() {
        return paraShotName;
    }

    public void setParaShotName(String paraShotName) {
        this.paraShotName = new SimpleStringProperty(paraShotName);
    }


    public SimpleStringProperty getSetValue() {
        return setValue;
    }

    public void setSetValue(String setValue) {
        this.setValue = new SimpleStringProperty(setValue);
    }


    public SimpleStringProperty getMinValue() {
        return minValue;
    }

    public void setMinValue(String minValue) {
        this.minValue = new SimpleStringProperty(minValue);
    }


    public SimpleStringProperty getMaxValue() {
        return maxValue;
    }

    public void setMaxValue(String maxValue) {
        this.maxValue = new SimpleStringProperty(maxValue);
    }


    public SimpleStringProperty getParaMeasure() {
        return paraMeasure;
    }

    public void setParaMeasure(String paraMeasure) {
        this.paraMeasure = new SimpleStringProperty(paraMeasure);
    }

}
