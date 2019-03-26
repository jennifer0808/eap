package cn.tzauto.octopus.isecsLayer.domain;

import java.util.List;

public interface ISecsInterface {
    String gotoScreen(String screenName);

    List<String> executeCommand(String command);

    String getPara(String paraName);

    //     String getMuiltiPara(List<String> paraNameList);
    boolean sendMessage(String message);
}
