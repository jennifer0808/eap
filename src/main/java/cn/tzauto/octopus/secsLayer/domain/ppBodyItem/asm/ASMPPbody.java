/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.secsLayer.domain.ppBodyItem.asm;

/**
 *
 * @author wjy
 */
public class  ASMPPbody{

    private String paraType;
    private String paraName;
    private String paraValue;
    private String paraUnit;

    public String getParaName() {
        return paraName;
    }

    public void setParaName(String paraName) {
        this.paraName = paraName;
    }

    public String getParaType() {
        return paraType;
    }

    public void setParaType(String paraType) {
        this.paraType = paraType;
    }

    public String getParaUnit() {
        return paraUnit;
    }

    public void setParaUnit(String paraUnit) {
        this.paraUnit = paraUnit;
    }

    public String getParaValue() {
        return paraValue;
    }

    public void setParaValue(String paraValue) {
        this.paraValue = paraValue;
    }

    public String toString() {
        return "ASMPPbody{" + "paraType=" + paraType + ", paraName=" + paraName + ", paraValue=" + paraValue + ", paraUnit=" + paraUnit + '}';
    }
    
}
