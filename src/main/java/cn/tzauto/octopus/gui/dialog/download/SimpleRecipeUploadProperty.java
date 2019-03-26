package cn.tzauto.octopus.gui.dialog.download;

import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.CheckBox;

/**
 * Created by wj_co on 2019/2/15.
 */
public class SimpleRecipeUploadProperty {

    private  SimpleStringProperty deviceCode;
    private  SimpleStringProperty deviceName;

    private CheckBox checkBox = new CheckBox();


    public SimpleRecipeUploadProperty(){}

    public SimpleRecipeUploadProperty(String deviceCode, String deviceName) {
        this.deviceCode = new SimpleStringProperty(deviceCode);
        this.deviceName = new SimpleStringProperty(deviceName);

    }

    public SimpleStringProperty getDeviceCode() {
        return deviceCode;
    }
    public void setDeviceCode(String deviceCode) {
        this.deviceCode = new SimpleStringProperty(deviceCode);
    }

    public SimpleStringProperty getDeviceName() {
        return deviceName;
    }
    public void setDeviceName(String deviceName) {
        this.deviceName = new SimpleStringProperty(deviceName);
    }

    public CheckBox getCheckBox() {
//        checkBox.setDisable(true);
        return checkBox;
    }

    public void setCheckBox(CheckBox checkBox) {
        this.checkBox = checkBox;
    }

}
