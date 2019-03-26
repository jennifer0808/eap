package cn.tzauto.octopus.biz.device.domain;

import java.util.Date;

public class DeviceInfoExt {

    private String id;
    private String deviceRowid;
    private String recipeId;
    private String recipeName;
    private String lockFlag;
    private String lotId;
    private String deviceStatus;
    private String edcCron;
    private String recipeDownloadMod;
    private String recipeAutoCheckFlag;
    private String recipeAutoCheckCron;
    private String connectionStatus;
    private String remarks;
    private String createBy;
    private Date createDate;
    private String updateBy;
    private Date updateDate;
    private String delFlag;
    private Integer verNo;
    private String recipeAnalysisMod;
    private String lockSwitch;
    private String startCheckMod;
    private String businessMod;

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

    public String getRecipeId() {
        return recipeId;
    }

    public void setRecipeId(String recipeId) {
        this.recipeId = recipeId == null ? null : recipeId.trim();
    }

    public String getRecipeName() {
        return recipeName;
    }

    public void setRecipeName(String recipeName) {
        this.recipeName = recipeName == null ? null : recipeName.trim();
    }

    public String getLockFlag() {
        return lockFlag;
    }

    public void setLockFlag(String lockFlag) {
        this.lockFlag = lockFlag == null ? null : lockFlag.trim();
    }

    public String getLotId() {
        return lotId;
    }

    public void setLotId(String lotId) {
        this.lotId = lotId == null ? null : lotId.trim();
    }

    public String getDeviceStatus() {
        return deviceStatus;
    }

    public void setDeviceStatus(String deviceStatus) {
        this.deviceStatus = deviceStatus == null ? null : deviceStatus.trim();
    }

    public String getEdcCron() {
        return edcCron;
    }

    public void setEdcCron(String edcCron) {
        this.edcCron = edcCron == null ? null : edcCron.trim();
    }

    public String getRecipeDownloadMod() {
        return recipeDownloadMod;
    }

    public void setRecipeDownloadMod(String recipeDownloadMod) {
        this.recipeDownloadMod = recipeDownloadMod == null ? null : recipeDownloadMod.trim();
    }

    public String getRecipeAutoCheckFlag() {
        return recipeAutoCheckFlag;
    }

    public void setRecipeAutoCheckFlag(String recipeAutoCheckFlag) {
        this.recipeAutoCheckFlag = recipeAutoCheckFlag == null ? null : recipeAutoCheckFlag.trim();
    }

    public String getRecipeAutoCheckCron() {
        return recipeAutoCheckCron;
    }

    public void setRecipeAutoCheckCron(String recipeAutoCheckCron) {
        this.recipeAutoCheckCron = recipeAutoCheckCron == null ? null : recipeAutoCheckCron.trim();
    }

    public String getConnectionStatus() {
        return connectionStatus;
    }

    public void setConnectionStatus(String connectionStatus) {
        this.connectionStatus = connectionStatus == null ? null : connectionStatus.trim();
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

    public String getRecipeAnalysisMod() {
        return recipeAnalysisMod;
    }

    public void setRecipeAnalysisMod(String recipeAnalysisMod) {
        this.recipeAnalysisMod = recipeAnalysisMod == null ? null : recipeAnalysisMod.trim();
    }

    public String getLockSwitch() {
        return lockSwitch;
    }

    public void setLockSwitch(String lockSwitch) {
        this.lockSwitch = lockSwitch == null ? null : lockSwitch.trim();
    }

    public String getStartCheckMod() {
        return startCheckMod;
    }

    public void setStartCheckMod(String startCheckMod) {
        this.startCheckMod = startCheckMod == null ? null : startCheckMod.trim();
    }

    public String getBusinessMod() {
        return businessMod;
    }

    public void setBusinessMod(String businessMod) {
        this.businessMod = businessMod == null ? null : businessMod.trim();
    }
}