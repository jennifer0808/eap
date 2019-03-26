/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.secsLayer.domain.remoteCommand;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author gavin
 */
public class CommandDomain {
    
    private String rcmd;
    private List<CommandParaPair> paraList;
    
    public CommandDomain(){
       paraList =new ArrayList();
    }
    
    public List<CommandParaPair> getParaList() {
        return paraList;
    }

    public void setParaList(List<CommandParaPair> paraList) {
        this.paraList = paraList;
    }


    public String getRcmd() {
        return rcmd;
    }

    public void setRcmd(String rcmd) {
        this.rcmd = rcmd;
    }
    
    
}
