/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.common.mq;

/**
 *
 * @author luosy
 */
public class MqMessageChannel {

    public static MessageUtils C2SRcpQueue = new MessageUtils("C2S.Q.RECIPE_C");
    public static MessageUtils C2SRcpUpLoadQueue = new MessageUtils("C2S.Q.RCPUPLOAD");
    public static MessageUtils C2SRcpDownLoadQueue = new MessageUtils("C2S.Q.RCPDOWNLOAD");
    public static MessageUtils C2SCheckRcpNameQueue = new MessageUtils("C2S.Q.CHECKRCPNAME");
    public static MessageUtils C2SRcpDeleteQueue = new MessageUtils("C2S.Q.RCPDELETE");
    public static MessageUtils C2SAlarmQueue = new MessageUtils("C2S.Q.ALARM_D");
    public static MessageUtils C2SInitQueue = new MessageUtils("C2S.Q.INITIAL_REQUEST");
    public static MessageUtils C2SLogQueue = new MessageUtils("C2S.Q.LOG_D");
    public static MessageUtils C2SEqptLogQueue = new MessageUtils("C2S.Q.EQPT_LOG_D");
    public static MessageUtils C2SEqptRemoteCommand = new MessageUtils("C2S.Q.EQPT_RCMD");
    public static MessageUtils C2SSpecificDataQueue = new MessageUtils("C2S.Q.SPECIFIC_DATA");
    public static MessageUtils C2SSvDataQueue = new MessageUtils("C2S.Q.SV_DATA");
    public static MessageUtils S2CRcpTopic = new MessageUtils("S2C.T.RECIPE_C");
    public static MessageUtils S2CDataTopic = new MessageUtils("S2C.T.DATA_TRANSFER");
    public static MessageUtils S2CEQPT_PARATopic = new MessageUtils("S2C.T.EQPT_PARAMETER");

    public MqMessageChannel() {
        C2SRcpQueue = new MessageUtils("C2S.Q.RECIPE_C");
        C2SRcpUpLoadQueue = new MessageUtils("C2S.Q.RCPUPLOAD");
        C2SRcpDownLoadQueue = new MessageUtils("C2S.Q.RCPDOWNLOAD");
        C2SCheckRcpNameQueue = new MessageUtils("C2S.Q.CHECKRCPNAME");
        C2SRcpDeleteQueue = new MessageUtils("C2S.Q.RCPDELETE");
        C2SAlarmQueue = new MessageUtils("C2S.Q.ALARM_D");
        C2SInitQueue = new MessageUtils("C2S.Q.INITIAL_REQUEST");
        C2SLogQueue = new MessageUtils("C2S.Q.LOG_D");
        C2SEqptLogQueue = new MessageUtils("C2S.Q.EQPT_LOG_D");
        C2SEqptRemoteCommand = new MessageUtils("C2S.Q.EQPT_RCMD");
        C2SSpecificDataQueue = new MessageUtils("C2S.Q.SPECIFIC_DATA");
        C2SSvDataQueue = new MessageUtils("C2S.Q.SV_DATA");
        S2CRcpTopic = new MessageUtils("S2C.T.RECIPE_C");
        S2CDataTopic = new MessageUtils("S2C.T.DATA_TRANSFER");
        S2CEQPT_PARATopic = new MessageUtils("S2C.T.EQPT_PARAMETER");
    }

}
