package cn.tzauto.octopus.secsLayer.modules;

/**
 * Created by luosy
 */
public class JudgeResult {
    private boolean isJudgePass;
    private String resultDescription;
    private Object attach;

    public boolean isJudgePass() {
        return isJudgePass;
    }

    public void setJudgePass(boolean judgePass) {
        isJudgePass = judgePass;
    }

    public String getResultDescription() {
        return resultDescription;
    }

    public void setResultDescription(String resultDescription) {
        this.resultDescription = resultDescription;
    }

    public Object getAttach() {
        return attach;
    }

    public void setAttach(Object attach) {
        this.attach = attach;
    }
}
