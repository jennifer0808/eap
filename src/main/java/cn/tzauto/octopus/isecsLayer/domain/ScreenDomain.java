/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.isecsLayer.domain;

import java.util.List;

/**
 *
 * @author gavin
 */
public class ScreenDomain {
    private String screenName;
    private int commandRepeatNum;//为了确保能跳转到指定屏幕，多执行几次action
    private int beforeReadDelay;//命令执行后的延迟时间
    private List<ISecsPara> paraList;
    private String readMode;

    public String getReadMode() {
        return readMode;
    }

    public void setReadMode(String readMode) {
        this.readMode = readMode;
    }

    
    
    public String getScreenName() {
        return screenName;
    }

    public void setScreenName(String screenName) {
        this.screenName = screenName;
    }

    public int getCommandRepeatNum() {
        return commandRepeatNum;
    }

    public void setCommandRepeatNum(int commandRepeatNum) {
        this.commandRepeatNum = commandRepeatNum;
    }

    public int getBeforeReadDelay() {
        return beforeReadDelay;
    }

    public void setBeforeReadDelay(int beforeReadDelay) {
        this.beforeReadDelay = beforeReadDelay;
    }

    public List<ISecsPara> getParaList() {
        return paraList;
    }

    public void setParaList(List<ISecsPara> paraList) {
        this.paraList = paraList;
    }
    
    
}
