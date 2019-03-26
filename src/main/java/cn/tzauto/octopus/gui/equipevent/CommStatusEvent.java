
package cn.tzauto.octopus.gui.equipevent;

public class CommStatusEvent extends ControlEvent
{
    private boolean comm = false;

    public CommStatusEvent(boolean isComm, int deviceId)
    {
        super(deviceId);
        comm = isComm;
    }

    public boolean isComm() {
        return comm;
    }
    
}
