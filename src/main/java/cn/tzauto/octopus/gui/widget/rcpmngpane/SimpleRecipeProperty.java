/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.gui.widget.rcpmngpane;

import cn.tzauto.octopus.biz.recipe.domain.Recipe;
import cn.tzauto.octopus.common.globalConfig.GlobalConstants;

import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.CheckBox;

/**
 * @author luosy
 */
public class SimpleRecipeProperty {

    private final SimpleStringProperty number;
    private final SimpleStringProperty recipeName;
    private final SimpleStringProperty deviceCode;
    private final SimpleStringProperty versionType;
    private final SimpleStringProperty versionNo;
    private final SimpleStringProperty uploadBy;
    private final SimpleStringProperty uploadDate;
    private final Recipe recipe;
    private CheckBox checkBox = new CheckBox();

    private CheckBox delCheckBox = new CheckBox();


    public SimpleRecipeProperty() {
        number = new SimpleStringProperty();
        recipeName = new SimpleStringProperty();
        deviceCode = new SimpleStringProperty();
        versionType = new SimpleStringProperty();
        versionNo = new SimpleStringProperty();
        uploadBy = new SimpleStringProperty();
        uploadDate = new SimpleStringProperty();
        recipe = new Recipe();
//        checkBox = new CheckBox();


    }

    public SimpleRecipeProperty(Recipe recipe, int num, String flag) {
            number = new SimpleStringProperty(String.valueOf(num));
            recipeName = new SimpleStringProperty(recipe.getRecipeName());
            deviceCode = new SimpleStringProperty(recipe.getDeviceCode());
            versionType = new SimpleStringProperty(recipe.getVersionType());
            versionNo = new SimpleStringProperty(String.valueOf(recipe.getVersionNo()));
            uploadBy = new SimpleStringProperty(recipe.getCreateBy());
            uploadDate = new SimpleStringProperty(GlobalConstants.dateFormat.format(recipe.getCreateDate()));
            this.recipe = recipe;
            if ("N".equals(flag)) {
                checkBox.setSelected(false);
            } else if ("Y".equals(flag)) {
                checkBox.setSelected(true);
            }
    }


    public SimpleStringProperty getNumber() {
        return number;
    }

    public SimpleStringProperty getRecipeName() {
        return recipeName;
    }

    public SimpleStringProperty getDeviceCode() {
        return deviceCode;
    }

    public SimpleStringProperty getVersionType() {
        return versionType;
    }

    public SimpleStringProperty getVersionNo() {
        return versionNo;
    }

    public SimpleStringProperty getUploadBy() {
        return uploadBy;
    }

    public SimpleStringProperty getUploadDate() {
        return uploadDate;
    }

    public Recipe getRecipe() {
        return recipe;
    }

    public CheckBox getCheckBox() {
        checkBox.setDisable(true);
        return checkBox;
    }

    public void setCheckBox(CheckBox checkBox) {
        this.checkBox = checkBox;
    }

    public CheckBox getDelCheckBox() {
        return delCheckBox;
    }

    public void setDelCheckBox(CheckBox delCheckBox) {
        this.delCheckBox = delCheckBox;
    }
}
