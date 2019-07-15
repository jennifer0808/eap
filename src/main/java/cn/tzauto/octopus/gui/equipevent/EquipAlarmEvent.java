/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.gui.equipevent;


public class EquipAlarmEvent extends ControlEvent {

    private int alarmState = 0;

    public EquipAlarmEvent(int alarmState, int deviceId) {
        super(deviceId);
        this.alarmState = alarmState;
    }

    public int getAlarmState() {
        return alarmState;
    }
}
