package cn.tzauto.octopus.secsLayer.domain;

/**
 * Created by luosy on 2019/4/3.
 */
public class ProcessFunctionNotSupportException extends Exception {
    /**
     * Constructs a {@code ProcessFunctionNotSupportException} with no detail message.
     */
    public ProcessFunctionNotSupportException() {
        super();
    }

    /**
     * Constructs a {@code ProcessFunctionNotSupportException} with the specified
     * detail message.
     *
     * @param s the detail message.
     */
    public ProcessFunctionNotSupportException(String s) {
        super(s);
    }
}
