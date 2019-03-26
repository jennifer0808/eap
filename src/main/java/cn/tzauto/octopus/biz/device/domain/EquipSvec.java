package cn.tzauto.octopus.biz.device.domain;

import java.util.Date;

public class EquipSvec {
    private Integer id;

    private String paraName;

    private String paraType;

    private String paraId;

    private String paraMin;

    private String paraMax;

    private String paraDefault;

    private String paraMeasure;

    private String equipType;

    private String createById;

    private String createBy;

    private Date createDate;

    private String updateById;

    private String updateBy;

    private Date updateDate1;

    private Integer updateCnt;

    private String delFlag;

    private String remarks;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getParaName() {
        return paraName;
    }

    public void setParaName(String paraName) {
        this.paraName = paraName == null ? null : paraName.trim();
    }

    public String getParaType() {
        return paraType;
    }

    public void setParaType(String paraType) {
        this.paraType = paraType == null ? null : paraType.trim();
    }

    public String getParaId() {
        return paraId;
    }

    public void setParaId(String paraId) {
        this.paraId = paraId == null ? null : paraId.trim();
    }

    public String getParaMin() {
        return paraMin;
    }

    public void setParaMin(String paraMin) {
        this.paraMin = paraMin == null ? null : paraMin.trim();
    }

    public String getParaMax() {
        return paraMax;
    }

    public void setParaMax(String paraMax) {
        this.paraMax = paraMax == null ? null : paraMax.trim();
    }

    public String getParaDefault() {
        return paraDefault;
    }

    public void setParaDefault(String paraDefault) {
        this.paraDefault = paraDefault == null ? null : paraDefault.trim();
    }

    public String getParaMeasure() {
        return paraMeasure;
    }

    public void setParaMeasure(String paraMeasure) {
        this.paraMeasure = paraMeasure == null ? null : paraMeasure.trim();
    }

    public String getEquipType() {
        return equipType;
    }

    public void setEquipType(String equipType) {
        this.equipType = equipType == null ? null : equipType.trim();
    }

    public String getCreateById() {
        return createById;
    }

    public void setCreateById(String createById) {
        this.createById = createById == null ? null : createById.trim();
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

    public String getUpdateById() {
        return updateById;
    }

    public void setUpdateById(String updateById) {
        this.updateById = updateById == null ? null : updateById.trim();
    }

    public String getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(String updateBy) {
        this.updateBy = updateBy == null ? null : updateBy.trim();
    }

    public Date getUpdateDate1() {
        return updateDate1;
    }

    public void setUpdateDate1(Date updateDate1) {
        this.updateDate1 = updateDate1;
    }

    public Integer getUpdateCnt() {
        return updateCnt;
    }

    public void setUpdateCnt(Integer updateCnt) {
        this.updateCnt = updateCnt;
    }

    public String getDelFlag() {
        return delFlag;
    }

    public void setDelFlag(String delFlag) {
        this.delFlag = delFlag == null ? null : delFlag.trim();
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks == null ? null : remarks.trim();
    }
}