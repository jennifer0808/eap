
package cn.tzauto.octopus.gui.equipevent;

/**
 * 行为状态事件
 * @author root
 */
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
