package cn.tzauto.octopus.biz.device.domain;

import java.util.Date;

public class EquipSecsConstants {
    private String id;

    private String deviceTypeId;

    private String deviceTypeName;

    private String constansType;

    private String constansGroup;

    private String constansCode;

    private String constansValue;

    private String constansDesc;

    private String activeFlag;

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

    public String getDeviceTypeId() {
        return deviceTypeId;
    }

    public void setDeviceTypeId(String deviceTypeId) {
        this.deviceTypeId = deviceTypeId == null ? null : deviceTypeId.trim();
    }

    public String getDeviceTypeName() {
        return deviceTypeName;
    }

    public void setDeviceTypeName(String deviceTypeName) {
        this.deviceTypeName = deviceTypeName == null ? null : deviceTypeName.trim();
    }

    public String getConstansType() {
        return constansType;
    }

    public void setConstansType(String constansType) {
        this.constansType = constansType == null ? null : constansType.trim();
    }

    public String getConstansGroup() {
        return constansGroup;
    }

    public void setConstansGroup(String constansGroup) {
        this.constansGroup = constansGroup == null ? null : constansGroup.trim();
    }

    public String getConstansCode() {
        return constansCode;
    }

    public void setConstansCode(String constansCode) {
        this.constansCode = constansCode == null ? null : constansCode.trim();
    }

    public String getConstansValue() {
        return constansValue;
    }

    public void setConstansValue(String constansValue) {
        this.constansValue = constansValue == null ? null : constansValue.trim();
    }

    public String getConstansDesc() {
        return constansDesc;
    }

    public void setConstansDesc(String constansDesc) {
        this.constansDesc = constansDesc == null ? null : constansDesc.trim();
    }

    public String getActiveFlag() {
        return activeFlag;
    }

    public void setActiveFlag(String activeFlag) {
        this.activeFlag = activeFlag == null ? null : activeFlag.trim();
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