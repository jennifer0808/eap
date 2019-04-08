/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package cn.tzauto.octopus.secsLayer.resolver.ismeca;

/**
 *
 * @author Wang Danfeng
 * @date 2018-8-6 17:48:14
 * @version V1.0
 * @desc 
 */
public class Ny20RecipeItem {
    
    private String name;
    private String nominalValue;
    private String highLimitFail;
    private String lowLimitFail;
    private String basicItem;

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

    public String getHighLimitFail() {
        return highLimitFail;
    }

    public void setHighLimitFail(String highLimitFail) {
        this.highLimitFail = highLimitFail;
    }

    public String getLowLimitFail() {
        return lowLimitFail;
    }

    public void setLowLimitFail(String lowLimitFail) {
        this.lowLimitFail = lowLimitFail;
    }

    public String getBasicItem() {
        return basicItem;
    }

    public void setBasicItem(String basicItem) {
        this.basicItem = basicItem;
    }

    @Override
    public String toString() {
        return "Ny20RecipeItem{" + "name=" + name + ", nominalValue=" + nominalValue + ", highLimitFail=" + highLimitFail + ", lowLimitFail=" + lowLimitFail + ", basicItem=" + basicItem + '}';
    }


    
    

}
