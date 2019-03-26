/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.secsLayer.domain.ppBodyItem.asm;

/**
 *
 * @author Administrator
 */
public class ASM3GPPbody {

    private String station;
    private String type;
    private String parameter;
    private String fileType;
    private String value;
    private String unit;

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public String getParameter() {
        return parameter;
    }

    public void setParameter(String parameter) {
        this.parameter = parameter;
    }

    public String getStation() {
        return station;
    }

    public void setStation(String station) {
        this.station = station;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String toString() {
        return "ASMPPbody{" + "station=" + station + ", type=" + type + ", parameter=" + parameter + ", fileType=" + fileType + ", value=" + value + ", unit=" + unit + '}';
    }
}
