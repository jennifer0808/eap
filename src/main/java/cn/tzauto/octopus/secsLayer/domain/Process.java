package cn.tzauto.octopus.secsLayer.domain;

import java.util.List;

/**
 * Created by luosy
 */
public class Process {
    private String processKey;
    private List<ProcessFunction> function;

    public Process(String processKey, List<ProcessFunction> function) {
        this.processKey = processKey;
        this.function = function;
    }

    public String getProcessKey() {
        return processKey;
    }

    public void setProcessKey(String processKey) {
        this.processKey = processKey;
    }

    public List<ProcessFunction> getFunction() {
        return function;
    }

    public void setFunction(List<ProcessFunction> function) {
        this.function = function;
    }
}
