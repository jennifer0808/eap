package cn.tzauto.octopus.biz.device.domain;

import java.util.Date;

public class UnitFormula {
    private String id;

    private String srcUnitId;

    private String srcUnitCode;

    private String tgtUnitId;

    private String tgtUnitCode;

    private String formulaDesc;

    private String activeFlag;

    private Date inureDate;

    private Date abateDate;

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

    public String getSrcUnitId() {
        return srcUnitId;
    }

    public void setSrcUnitId(String srcUnitId) {
        this.srcUnitId = srcUnitId == null ? null : srcUnitId.trim();
    }

    public String getSrcUnitCode() {
        return srcUnitCode;
    }

    public void setSrcUnitCode(String srcUnitCode) {
        this.srcUnitCode = srcUnitCode == null ? null : srcUnitCode.trim();
    }

    public String getTgtUnitId() {
        return tgtUnitId;
    }

    public void setTgtUnitId(String tgtUnitId) {
        this.tgtUnitId = tgtUnitId == null ? null : tgtUnitId.trim();
    }

    public String getTgtUnitCode() {
        return tgtUnitCode;
    }

    public void setTgtUnitCode(String tgtUnitCode) {
        this.tgtUnitCode = tgtUnitCode == null ? null : tgtUnitCode.trim();
    }

    public String getFormulaDesc() {
        return formulaDesc;
    }

    public void setFormulaDesc(String formulaDesc) {
        this.formulaDesc = formulaDesc == null ? null : formulaDesc.trim();
    }

    public String getActiveFlag() {
        return activeFlag;
    }

    public void setActiveFlag(String activeFlag) {
        this.activeFlag = activeFlag == null ? null : activeFlag.trim();
    }

    public Date getInureDate() {
        return inureDate;
    }

    public void setInureDate(Date inureDate) {
        this.inureDate = inureDate;
    }

    public Date getAbateDate() {
        return abateDate;
    }

    public void setAbateDate(Date abateDate) {
        this.abateDate = abateDate;
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