package cn.tzauto.octopus.gui.equipevent;

public class ControlEvent {

    private int deviceId;

    public ControlEvent(int devId) {
        this.deviceId = devId;
    }

    /**
     * @return the deviceId
     */
    public int getDeviceId() {
        return deviceId;
    }

    /**
     * @param deviceId the deviceId to set
     */
    public void setDeviceId(int deviceId) {
        this.deviceId = deviceId;
    }

}
