package cn.tzauto.octopus.biz.device.domain;

import java.util.Date;

public class DeviceOplog {
    private String id;

    private String deviceRowid;

    private String deviceCode;

    private Date opTime;

    private String opType;

    private String deviceCeid;

    private String opDesc;

    private String formerRecipeId;

    private String formerRecipeName;

    private String currRecipeId;

    private String currRecipeName;

    private String formerLotId;

    private String currLotId;

    private String formerDeviceStatus;

    private String currDeviceStatus;

    private String syncFlag;

    private String remarks;

    private String createBy;

    private Date createDate;

    private String updateBy;

    private Date updateDate;

    private String delFlag;

    private Integer verNo;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id == null ? null : id.trim();
    }

    public String getDeviceRowid() {
        return deviceRowid;
    }

    public void setDeviceRowid(String deviceRowid) {
        this.deviceRowid = deviceRowid == null ? null : deviceRowid.trim();
    }

    public String getDeviceCode() {
        return deviceCode;
    }

    public void setDeviceCode(String deviceCode) {
        this.deviceCode = deviceCode == null ? null : deviceCode.trim();
    }

    public Date getOpTime() {
        return opTime;
    }

    public void setOpTime(Date opTime) {
        this.opTime = opTime;
    }

    public String getOpType() {
        return opType;
    }

    public void setOpType(String opType) {
        this.opType = opType == null ? null : opType.trim();
    }

    public String getDeviceCeid() {
        return deviceCeid;
    }

    public void setDeviceCeid(String deviceCeid) {
        this.deviceCeid = deviceCeid == null ? null : deviceCeid.trim();
    }

    public String getOpDesc() {
        return opDesc;
    }

    public void setOpDesc(String opDesc) {
        this.opDesc = opDesc == null ? null : opDesc.trim();
    }

    public String getFormerRecipeId() {
        return formerRecipeId;
    }

    public void setFormerRecipeId(String formerRecipeId) {
        this.formerRecipeId = formerRecipeId == null ? null : formerRecipeId.trim();
    }

    public String getFormerRecipeName() {
        return formerRecipeName;
    }

    public void setFormerRecipeName(String formerRecipeName) {
        this.formerRecipeName = formerRecipeName == null ? null : formerRecipeName.trim();
    }

    public String getCurrRecipeId() {
        return currRecipeId;
    }

    public void setCurrRecipeId(String currRecipeId) {
        this.currRecipeId = currRecipeId == null ? null : currRecipeId.trim();
    }

    public String getCurrRecipeName() {
        return currRecipeName;
    }

    public void setCurrRecipeName(String currRecipeName) {
        this.currRecipeName = currRecipeName == null ? null : currRecipeName.trim();
    }

    public String getFormerLotId() {
        return formerLotId;
    }

    public void setFormerLotId(String formerLotId) {
        this.formerLotId = formerLotId == null ? null : formerLotId.trim();
    }

    public String getCurrLotId() {
        return currLotId;
    }

    public void setCurrLotId(String currLotId) {
        this.currLotId = currLotId == null ? null : currLotId.trim();
    }

    public String getFormerDeviceStatus() {
        return formerDeviceStatus;
    }

    public void setFormerDeviceStatus(String formerDeviceStatus) {
        this.formerDeviceStatus = formerDeviceStatus == null ? null : formerDeviceStatus.trim();
    }

    public String getCurrDeviceStatus() {
        return currDeviceStatus;
    }

    public void setCurrDeviceStatus(String currDeviceStatus) {
        this.currDeviceStatus = currDeviceStatus == null ? null : currDeviceStatus.trim();
    }

    public String getSyncFlag() {
        return syncFlag;
    }

    public void setSyncFlag(String syncFlag) {
        this.syncFlag = syncFlag == null ? null : syncFlag.trim();
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks == null ? null : remarks.trim();
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

    public String getDelFlag() {
        return delFlag;
    }

    public void setDelFlag(String delFlag) {
        this.delFlag = delFlag == null ? null : delFlag.trim();
    }

    public Integer getVerNo() {
        return verNo;
    }

    public void setVerNo(Integer verNo) {
        this.verNo = verNo;
    }
}