
package cn.tzauto.octopus.gui.equipevent;

public class ServiceStatusEvent extends ControlEvent
{
    private boolean service = false;

    public ServiceStatusEvent(boolean isService, int deviceId)
    {
        super(deviceId);
        service = isService;
    }

    public boolean isService() {
        return service;
    }
    
}
