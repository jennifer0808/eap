/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.isecsLayer.domain;

/**
 *
 * @author gavin
 */
public class ISecsPara extends EquipPara{
    private String isecsParaName;
    private String screenName;

    public String getScreenName() {
        return screenName;
    }

    public void setScreenName(String screenName) {
        this.screenName = screenName;
    }

    public String getIsecsParaName() {
        return isecsParaName;
    }

    public void setIsecsParaName(String isecsParaName) {
        this.isecsParaName = isecsParaName;
    }
    
}
