/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.isecsLayer.domain;

import java.util.Map;

/**
 *
 * @author gavin
 */
public class EquipEevent {
    private String CEID;
    private String RPTID;
    private Map paraMap;
    private String dataID;

    public String getCEID() {
        return CEID;
    }

    public void setCEID(String CEID) {
        this.CEID = CEID;
    }

    public String getRPTID() {
        return RPTID;
    }

    public void setRPTID(String RPTID) {
        this.RPTID = RPTID;
    }

    public Map getParaMap() {
        return paraMap;
    }

    public void setParaMap(Map paraMap) {
        this.paraMap = paraMap;
    }

    public String getDataID() {
        return dataID;
    }

    public void setDataID(String dataID) {
        this.dataID = dataID;
    }
    
    
    
    
}
