package cn.tzauto.octopus.secsLayer.secsbody;


/**
 * Created by luosy on 2019/4/3.
 */
public class Report {
    private long reportId;
    private ReportData reportData;

    public Report(long reportId, ReportData reportData) {
        this.reportId = reportId;
        this.reportData = reportData;
    }
}
