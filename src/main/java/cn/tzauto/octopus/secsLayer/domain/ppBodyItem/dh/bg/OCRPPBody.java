/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.secsLayer.domain.ppBodyItem.dh.bg;

/**
 *
 * @author Administrator
 */
public class OCRPPBody {

    private String type;
    private String key;
    private String value;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "OcrBody [type=" + type + ", key=" + key + ", value=" + value + "]";
    }
}
