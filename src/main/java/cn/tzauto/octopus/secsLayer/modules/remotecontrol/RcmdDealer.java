package cn.tzauto.octopus.secsLayer.modules.remotecontrol;

import cn.tzauto.generalDriver.exceptions.*;
import cn.tzauto.octopus.common.globalConfig.GlobalConstants;
import cn.tzauto.octopus.secsLayer.domain.ProcessFunction;
import cn.tzauto.octopus.secsLayer.modules.JudgeResult;
import org.apache.log4j.Logger;

import java.io.IOException;

/**
 * Created by luosy on 2019/4/3.
 */
public class RcmdDealer {
    private static Logger logger = Logger.getLogger(RcmdDealer.class);

    public static JudgeResult deal(ProcessFunction processFunction, String deviceCode) throws IOException, BrokenProtocolException, T6TimeOutException, HsmsProtocolNotSelectedException, T3TimeOutException, MessageDataException, StreamFunctionNotSupportException, ItemIntegrityException, InterruptedException {
        //todo 完善每个判断逻辑
        if (processFunction.getFunctionCode().equals(ProcessFunction.RCMD_STOP)) {
            String rcmd = processFunction.getFunctionPara().get(0).getValue();
            GlobalConstants.stage.equipHosts.get(deviceCode).getActiveWrapper().sendS2F41out(rcmd, null, null, null, null);
        } else if (processFunction.getFunctionCode().equals(ProcessFunction.RCMD_TRML_MSG)) {
            String text = processFunction.getFunctionPara().get(0).getValue();
            GlobalConstants.stage.equipHosts.get(deviceCode).getActiveWrapper().sendS10F3out((byte) 0, text);
        }
        return null;
    }

    /*
 * towa 支持的命令有:PP-SELECT STOP ABORT RELEASE LOCK
 * fico 支持的命令有:PP-SELECT STOP START PAUSE RESUME
 * yamada 170t支持的命令有:PP-SELECT STOP START LOCK UNLOCK LOCAL REMOTE
 * DISCO DGP8761 :PP_SELECT START PAUSE RESUME UNLOAD GO_LOCAL GO_REMOTE END_ACK UNLOAD_GP ABORT
 * DISCO WS DFD6361:START_S  PP_SELECT_S STOP PAUSE_H RESUME_H ABORT
 * DISCO LS DFL7160 DFL7161:START_S  PP_SELECT_S STOP PAUSE_H RESUME_H ABORT
 */

}
