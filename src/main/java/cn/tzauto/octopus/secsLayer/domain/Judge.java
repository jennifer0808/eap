package cn.tzauto.octopus.secsLayer.domain;

/**
 * Created by luosy on 2019/4/3.
 */
public class Judge {
    //判断字符串，数字，判断方法的返回结果
    private String judgeType;
    private ProcessFunction successFunction;
    private ProcessFunction failedFunction;
    private String judgeStandardStr;
    private double judgeStandardMin;
    private double judgeStandardMax;

    public Judge(String judgeType, ProcessFunction successFunction, ProcessFunction failedFunction, String judgeStandardStr, double judgeStandardMin, double judgeStandardMax) {
        this.judgeType = judgeType;
        this.successFunction = successFunction;
        this.failedFunction = failedFunction;
        this.judgeStandardStr = judgeStandardStr;
        this.judgeStandardMin = judgeStandardMin;
        this.judgeStandardMax = judgeStandardMax;
    }

    public String getJudgeType() {
        return judgeType;
    }

    public void setJudgeType(String judgeType) {
        this.judgeType = judgeType;
    }

    public ProcessFunction getSuccessFunction() {
        return successFunction;
    }

    public void setSuccessFunction(ProcessFunction successFunction) {
        this.successFunction = successFunction;
    }

    public ProcessFunction getFailedFunction() {
        return failedFunction;
    }

    public void setFailedFunction(ProcessFunction failedFunction) {
        this.failedFunction = failedFunction;
    }

    public String getJudgeStandardStr() {
        return judgeStandardStr;
    }

    public void setJudgeStandardStr(String judgeStandardStr) {
        this.judgeStandardStr = judgeStandardStr;
    }

    public double getJudgeStandardMin() {
        return judgeStandardMin;
    }

    public void setJudgeStandardMin(double judgeStandardMin) {
        this.judgeStandardMin = judgeStandardMin;
    }

    public double getJudgeStandardMax() {
        return judgeStandardMax;
    }

    public void setJudgeStandardMax(double judgeStandardMax) {
        this.judgeStandardMax = judgeStandardMax;
    }
}
