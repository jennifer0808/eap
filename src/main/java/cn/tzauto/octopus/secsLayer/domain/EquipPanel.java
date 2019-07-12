/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.secsLayer.domain;

import cn.tzauto.octopus.secsLayer.util.NormalConstant;

/**
 *
 * @author rain
 */
public class EquipPanel {

    private String runState;
    private String runningRcp;
    private String workLot;
    private int alarmState;
    private String controlState; //LOCAL_ONLINE = 0, REMOTE_ONLINE =1, OFFLINE = -1
    private int netState; //断网=0，正常=1；

    public EquipPanel() {
        this.runState = "No Event Report";
        this.runningRcp = "--";
        this.workLot = "--";
        this.alarmState = 0;
        this.controlState = NormalConstant.CONTROL_OFFLINE;
        this.netState = 1;
    }

    public String getRunState() {
        return runState;
    }

    public void setRunState(String runState) {
        this.runState = runState;
    }

    public String getRunningRcp() {
        return runningRcp;
    }

    public void setRunningRcp(String runningRcp) {
        this.runningRcp = runningRcp;
    }

    public String getWorkLot() {
        return workLot;
    }

    public void setWorkLot(String workLot) {
        this.workLot = workLot;
    }

    public int getAlarmState() {
        return alarmState;
    }

    public void setAlarmState(int alarmState) {
        this.alarmState = alarmState;
    }

    public String getControlState() {
        return controlState;
    }

    public void setControlState(String controlState) {
        this.controlState = controlState;
    }

    public int getNetState() {
        return netState;
    }

    public void setNetState(int netState) {
        this.netState = netState;
    }

    @Override
    public Object clone() {
        EquipPanel equipPanel = new EquipPanel();
        equipPanel.setRunState(runState);
        equipPanel.setRunningRcp(runningRcp);
        equipPanel.setWorkLot(workLot);
        equipPanel.setAlarmState(alarmState);
        equipPanel.setControlState(controlState);
        equipPanel.setNetState(netState);
        return equipPanel;
    }
}
