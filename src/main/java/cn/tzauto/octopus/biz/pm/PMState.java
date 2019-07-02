package cn.tzauto.octopus.biz.pm;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by luosy on 2019/6/29.
 */
public class PMState {
    private String startTime;
    private String endTime;
    private boolean isPM;
    private String operater;
    private static DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public PMState(String operater) {
        startTime = dateFormat.format(new Date());
        endTime = "";
        isPM = true;
        this.operater = operater;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public boolean isPM() {
        return isPM;
    }

    public void setPM(boolean PM) {
        isPM = PM;
    }

    public String getOperater() {
        return operater;
    }

    public void setOperater(String operater) {
        this.operater = operater;
    }
}
