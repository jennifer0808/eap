package cn.tzauto.octopus.secsLayer.modules.edc;


import cn.tzauto.generalDriver.exceptions.*;
import cn.tzauto.octopus.common.globalConfig.GlobalConstants;
import cn.tzauto.octopus.secsLayer.domain.JudgeDealer;
import cn.tzauto.octopus.secsLayer.domain.ProcessFunction;
import cn.tzauto.octopus.secsLayer.domain.ProcessFunctionNotSupportException;
import cn.tzauto.octopus.secsLayer.domain.SecsParameter;
import cn.tzauto.octopus.secsLayer.modules.Dealer;
import cn.tzauto.octopus.secsLayer.modules.JudgeResult;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by luosy on 2019/4/3.
 */
public class EdcDealer {
    public static JudgeResult deal(ProcessFunction processFunction, String deviceCode) throws IOException, BrokenProtocolException, T6TimeOutException, T3TimeOutException, InterruptedException, ProcessFunctionNotSupportException, StateException, IntegrityException, InvalidDataException {
        List idlist = new ArrayList();
        for (SecsParameter secsParameter : processFunction.getFunctionPara()) {
            idlist.add(secsParameter.getValue());
        }
        List valueList = new ArrayList();
        if (processFunction.getFunctionCode().equals(ProcessFunction.EDC_SV)) {
            valueList = (ArrayList) GlobalConstants.stage.equipHosts.get(deviceCode).getActiveWrapper().sendS1F3out(idlist, processFunction.getFunctionPara().get(0).getFormat()).get("SV");
        } else if (processFunction.getFunctionCode().equals(ProcessFunction.EDC_EC)) {
            valueList = (ArrayList) GlobalConstants.stage.equipHosts.get(deviceCode).getActiveWrapper().sendS2F13out(idlist, processFunction.getFunctionPara().get(0).getFormat()).get("EC");
        } else {
            throw new ProcessFunctionNotSupportException("The functionCode " + processFunction.getFunctionCode() + " has not been support at EdcDealer");
        }
        for (int i = 0; i < valueList.size(); i++) {
            if (!JudgeDealer.deal(processFunction.getFunctionPara().get(i), String.valueOf(valueList.get(i)))) {
                return Dealer.deal(processFunction.getFunctionPara().get(i).getJudge().getFailedFunction(),deviceCode);
            }
        }
        return Dealer.deal(processFunction.getJudge().getSuccessFunction(),deviceCode);
    }
}
