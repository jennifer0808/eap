
package cn.tzauto.octopus.gui.equipevent;


public class BehaviorStatusEvent extends ControlEvent
{
    private int status;

    public BehaviorStatusEvent(int bStatus, int deviceId)
    {
        super(deviceId);
        status = bStatus;
    }
    public int getStatus() {
        return status;
    }
    
}
