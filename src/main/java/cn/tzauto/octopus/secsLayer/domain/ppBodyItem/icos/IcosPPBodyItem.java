/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.secsLayer.domain.ppBodyItem.icos;

/**
 *
 * @author Administrator
 */
public class IcosPPBodyItem {

    private String paraName;
    private String paraValue;

    public String getParaName() {
        return paraName;
    }

    public void setParaName(String paraName) {
        this.paraName = paraName;
    }

    public String getParaValue() {
        return paraValue;
    }

    public void setParaValue(String paraValue) {
        this.paraValue = paraValue;
    }

    public String toString() {
        return "IcosPPBodyItem{" + "paraName=" + paraName + ", paraValue=" + paraValue  + '}';
    }
    
}
