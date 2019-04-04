package cn.tzauto.octopus.secsLayer.secsbody;

/**
 * Created by luosy on 2019/4/3.
 */
public class EventReport {
    private long ceidId;
    private Report report;

    public EventReport(long ceidId, Report report) {
        this.ceidId = ceidId;
        this.report = report;
    }
}
