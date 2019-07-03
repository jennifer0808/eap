package cn.tzauto.octopus.biz.monitor.domain;

import java.util.Date;

public class DeviceRealtimePara {
    private String id;

    private String deviceId;

    private String deviceCode;

    private String deviceName;

    private String recipeRowId;

    private String paraCode;

    private String paraName;

    private String paraShotName;

    private String paraMeasure;

    private String valueType;

    private String realtimeValue;

    private String setValue;

    private String minValue;

    private String maxValue;

    private String paraDesc;

    private String createBy;

    private Date createDate;

    private String updateBy;

    private Date updateDate;
    
    private long updateCnt;
    
    private String remarks;

    private String delFlag;

    public long getUpdateCnt() {
        return updateCnt;
    }

    public void setUpdateCnt(long updateCnt) {
        this.updateCnt = updateCnt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id == null ? null : id.trim();
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId == null ? null : deviceId.trim();
    }

    public String getDeviceCode() {
        return deviceCode;
    }

    public void setDeviceCode(String deviceCode) {
        this.deviceCode = deviceCode == null ? null : deviceCode.trim();
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName == null ? null : deviceName.trim();
    }

    public String getRecipeRowId() {
        return recipeRowId;
    }

    public void setRecipeRowId(String recipeRowId) {
        this.recipeRowId = recipeRowId == null ? null : recipeRowId.trim();
    }

    public String getParaCode() {
        return paraCode;
    }

    public void setParaCode(String paraCode) {
        this.paraCode = paraCode == null ? null : paraCode.trim();
    }

    public String getParaName() {
        return paraName;
    }

    public void setParaName(String paraName) {
        this.paraName = paraName == null ? null : paraName.trim();
    }

    public String getParaShotName() {
        return paraShotName;
    }

    public void setParaShotName(String paraShotName) {
        this.paraShotName = paraShotName == null ? null : paraShotName.trim();
    }

    public String getParaMeasure() {
        return paraMeasure;
    }

    public void setParaMeasure(String paraMeasure) {
        this.paraMeasure = paraMeasure == null ? null : paraMeasure.trim();
    }

    public String getValueType() {
        return valueType;
    }

    public void setValueType(String valueType) {
        this.valueType = valueType == null ? null : valueType.trim();
    }

    public String getRealtimeValue() {
        return realtimeValue;
    }

    public void setRealtimeValue(String realtimeValue) {
        this.realtimeValue = realtimeValue == null ? null : realtimeValue.trim();
    }

    public String getSetValue() {
        return setValue;
    }

    public void setSetValue(String setValue) {
        this.setValue = setValue == null ? null : setValue.trim();
    }

    public String getMinValue() {
        return minValue;
    }

    public void setMinValue(String minValue) {
        this.minValue = minValue;
    }

    public String getMaxValue() {
        return maxValue;
    }

    public void setMaxValue(String maxValue) {
        this.maxValue = maxValue;
    }

    public String getParaDesc() {
        return paraDesc;
    }

    public void setParaDesc(String paraDesc) {
        this.paraDesc = paraDesc == null ? null : paraDesc.trim();
    }

    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(String createBy) {
        this.createBy = createBy == null ? null : createBy.trim();
    }

    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    public String getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(String updateBy) {
        this.updateBy = updateBy == null ? null : updateBy.trim();
    }

    public Date getUpdateDate() {
        return updateDate;
    }

    public void setUpdateDate(Date updateDate) {
        this.updateDate = updateDate;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks == null ? null : remarks.trim();
    }

    public String getDelFlag() {
        return delFlag;
    }

    public void setDelFlag(String delFlag) {
        this.delFlag = delFlag == null ? null : delFlag.trim();
    }
}