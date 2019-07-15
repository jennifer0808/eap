package cn.tzauto.octopus.secsLayer.domain;

import java.util.List;

/**
 * Created by luosy
 */
public class ProcessFunction {
    public static final String EDC_SV = "EDC_SV";//执行SV取值
    public static final String EDC_EC = "EDC_EC";//执行EC取值
    public static final String RCMD_STOP = "RCMD_STOP";//执行STOP命令
    public static final String RCMD_PAUSE = "RCMD_PAUSE";//
    public static final String RCMD_START = "RCMD_START";//
    public static final String RCP_UPLOAD = "RECIPE_UPLOAD";//
    public static final String RCP_DOWNLOAD = "RECIPE_DOWNLOAD";//
    public static final String RCP_ANLY = "RECIPE_ANALYSE";//
    public static final String RCMD_TRML_MSG = "RCMD_TERMINAL_MESSAGE";//
    public static final String STRIP_UPLOAD = "STRIP_UPLOAD";//
    public static final String START_CHECK = "START_CHECK";//
    public static final String RCP_NAME_CHECK = "RCP_NAME_CHECK";//
    public static final String RCP_PARA_CHECK = "RCP_PARA_CHECK";//
    public static final String EQP_STATE_CHECK = "EQP_STATE_CHECK";//

    private String functionCode;
    private List<SecsParameter> functionPara;
    private Judge judge;

    public ProcessFunction(String functionCode, List<SecsParameter> functionPara, Judge judge) throws ProcessFunctionNotSupportException {
        //todo 从配置文件中读取已支持的功能类型
        if (true) {
            this.functionCode = functionCode;
            this.functionPara = functionPara;
            this.judge = judge;
        } else {
            throw new ProcessFunctionNotSupportException("The functionCode " + functionCode + " has not been support");
        }
    }

    public String getFunctionCode() {
        return functionCode;
    }

    public void setFunctionCode(String functionCode) throws ProcessFunctionNotSupportException {
        if (true) {
            this.functionCode = functionCode;
        } else {
            throw new ProcessFunctionNotSupportException("The functionCode " + functionCode + " has not been support");
        }
    }

    public List<SecsParameter> getFunctionPara() {
        return functionPara;
    }

    public void setFunctionPara(List<SecsParameter> functionPara) {
        this.functionPara = functionPara;
    }

    public Judge getJudge() {
        return judge;
    }

    public void setJudge(Judge judge) {
        this.judge = judge;
    }
}
