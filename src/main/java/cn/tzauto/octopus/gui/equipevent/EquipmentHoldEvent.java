/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.gui.equipevent;

/**
 *
 * @author luosy
 */
public class EquipmentHoldEvent extends ControlEvent{
     private boolean hold = false;

    public EquipmentHoldEvent(boolean isHold, int deviceId)
    {
        super(deviceId);
        hold = isHold;
    }

    public boolean isHold() {
        return hold;
    }
}
