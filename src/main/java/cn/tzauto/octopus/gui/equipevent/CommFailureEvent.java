
package cn.tzauto.octopus.gui.equipevent;


public class CommFailureEvent extends ControlEvent
{
    private Exception anyExeption; 

    public CommFailureEvent(Exception e, int deviceId)
    {
        super(deviceId);
        anyExeption = e;
    }

    public CommFailureEvent(int deviceId)
    {
        super(deviceId);
    }

    /**
     * @return the anyExeption
     */
    public Exception getAnyExeption() {
        return anyExeption;
    }

}
