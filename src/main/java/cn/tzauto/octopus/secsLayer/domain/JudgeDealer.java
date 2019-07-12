package cn.tzauto.octopus.secsLayer.domain;


/**
 * Created by luosy
 */
public class JudgeDealer {
    public static boolean deal(SecsParameter secsParameter, String judgeObject) {
        Judge judge = secsParameter.getJudge();
        ProcessFunction success = judge.getSuccessFunction();
        ProcessFunction failed = judge.getFailedFunction();
        String judgeType = judge.getJudgeType();
        if (judgeType.equals("NUM")) {
            //todo 这里没有判断数字是否可以相等
            if (judge.getJudgeStandardMin() > Double.parseDouble(judgeObject) || judge.getJudgeStandardMax() < Double.parseDouble(judgeObject)) {
                return true;
            } else {
                return false;
            }
        } else {
            if (judgeObject.equals(judge.getJudgeStandardStr())) {
                return true;
            } else {
                return false;
            }
        }
    }

    private static boolean isNumber(String judgeObject) {
        try {
            Double.parseDouble(judgeObject);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

}
