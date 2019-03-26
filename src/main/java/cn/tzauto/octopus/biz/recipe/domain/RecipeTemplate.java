package cn.tzauto.octopus.biz.recipe.domain;

import java.util.Date;

public class RecipeTemplate {

    private String id;
    private String deviceTypeId;
    private String deviceTypeCode;
    private String deviceTypeName;
    private String paraCode;
    private String paraName;
    private String paraShotName;
    private String paraUnit;
    private String setValue;
    private String minValue;
    private String maxValue;
    private String specType;
    private String showFlag;
    private String monitorFlag;
    private String paraLevel;
    private String paraType;
    private String countRate;
    private Integer decodeLength;
    private Integer decodeStart;
    private Integer decodeEnd;
    private String groupName;
    private String deviceVariableId;
    private String deviceVariableType;
    private String paraDesc;
    private Integer sortNo;
    private String createBy;
    private Date createDate;
    private String updateBy;
    private Date updateDate;
    private String remarks;
    private String delFlag;
    private String deviceVariableUnit;
    private String goldPara;

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

    public String getDeviceTypeCode() {
        return deviceTypeCode;
    }

    public void setDeviceTypeCode(String deviceTypeCode) {
        this.deviceTypeCode = deviceTypeCode == null ? null : deviceTypeCode.trim();
    }

    public String getDeviceTypeName() {
        return deviceTypeName;
    }

    public void setDeviceTypeName(String deviceTypeName) {
        this.deviceTypeName = deviceTypeName == null ? null : deviceTypeName.trim();
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

    public String getParaUnit() {
        return paraUnit;
    }

    public void setParaUnit(String paraUnit) {
        this.paraUnit = paraUnit == null ? null : paraUnit.trim();
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

    public String getSpecType() {
        return specType;
    }

    public void setSpecType(String specType) {
        this.specType = specType == null ? null : specType.trim();
    }

    public String getShowFlag() {
        return showFlag;
    }

    public void setShowFlag(String showFlag) {
        this.showFlag = showFlag == null ? null : showFlag.trim();
    }

    public String getMonitorFlag() {
        return monitorFlag;
    }

    public void setMonitorFlag(String monitorFlag) {
        this.monitorFlag = monitorFlag == null ? null : monitorFlag.trim();
    }

    public String getParaLevel() {
        return paraLevel;
    }

    public void setParaLevel(String paraLevel) {
        this.paraLevel = paraLevel == null ? null : paraLevel.trim();
    }

    public String getParaType() {
        return paraType;
    }

    public void setParaType(String paraType) {
        this.paraType = paraType == null ? null : paraType.trim();
    }

    public String getCountRate() {
        return countRate;
    }

    public void setCountRate(String countRate) {
        this.countRate = countRate;
    }

    public Integer getDecodeLength() {
        return decodeLength;
    }

    public void setDecodeLength(Integer decodeLength) {
        this.decodeLength = decodeLength;
    }

    public Integer getDecodeStart() {
        return decodeStart;
    }

    public void setDecodeStart(Integer decodeStart) {
        this.decodeStart = decodeStart;
    }

    public Integer getDecodeEnd() {
        return decodeEnd;
    }

    public void setDecodeEnd(Integer decodeEnd) {
        this.decodeEnd = decodeEnd;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName == null ? null : groupName.trim();
    }

    public String getDeviceVariableId() {
        return deviceVariableId;
    }

    public void setDeviceVariableId(String deviceVariableId) {
        this.deviceVariableId = deviceVariableId == null ? null : deviceVariableId.trim();
    }

    public String getDeviceVariableType() {
        return deviceVariableType;
    }

    public void setDeviceVariableType(String deviceVariableType) {
        this.deviceVariableType = deviceVariableType == null ? null : deviceVariableType.trim();
    }

    public String getParaDesc() {
        return paraDesc;
    }

    public void setParaDesc(String paraDesc) {
        this.paraDesc = paraDesc == null ? null : paraDesc.trim();
    }

    public Integer getSortNo() {
        return sortNo;
    }

    public void setSortNo(Integer sortNo) {
        this.sortNo = sortNo;
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

    public String getDeviceVariableUnit() {
        return deviceVariableUnit;
    }

    public void setDeviceVariableUnit(String deviceVariableUnit) {
        this.deviceVariableUnit = deviceVariableUnit;
    }

    public String getGoldPara() {
        return goldPara;
    }

    public void setGoldPara(String goldPara) {
        this.goldPara = goldPara;
    }

}
