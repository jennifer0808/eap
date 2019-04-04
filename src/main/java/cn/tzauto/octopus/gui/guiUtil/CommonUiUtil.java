package cn.tzauto.octopus.gui.guiUtil;

import cn.tzauto.octopus.common.globalConfig.GlobalConstants;
import cn.tzauto.octopus.secsLayer.domain.EquipNodeBean;
import cn.tzauto.octopus.secsLayer.domain.EquipPanel;
import cn.tzauto.octopus.secsLayer.domain.EquipState;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import javax.swing.*;
import java.util.Map;
import java.util.Optional;

import static javafx.scene.control.Alert.AlertType.INFORMATION;

/**
 * Created by zm730 on 2019/2/13.
 */
public class CommonUiUtil {

    public static Optional<ButtonType> alert(Alert.AlertType type, String message) {
        Alert alert = new Alert(type);

        switch (type) {
            case INFORMATION:
                alert.setTitle("Information Dialog");
                break;
            case WARNING:
                alert.setTitle("Warning Dialog");
                break;
            case CONFIRMATION:
                alert.setTitle("Confirmation Dialog");
                break;
        }

        alert.setHeaderText(null);

        alert.setContentText(message);

        return alert.showAndWait();


    }

    /**
     * 更新设备作业信息显示
     *
     * @param resultMap
     */
    public static void changeEquipPanel(Map resultMap, String deviceCode) {
        //ArrayList<EquipNodeBean> equipBeans = GlobalConstants.stage.equipBeans;
        for (EquipNodeBean equipNodeBean : GlobalConstants.stage.equipBeans) {
            if (equipNodeBean.getDeviceCode().equals(deviceCode)) {
                EquipPanel oldPanel = (EquipPanel) equipNodeBean.getEquipPanelProperty();
                EquipPanel newPanel = (EquipPanel) equipNodeBean.getEquipPanelProperty().clone();
                if (resultMap.get("PPExecName") != null) {
                    newPanel.setRunningRcp(resultMap.get("PPExecName").toString());
                }
                if (resultMap.get("EquipStatus") != null) {
                    newPanel.setRunState(resultMap.get("EquipStatus").toString());
                }
                if (resultMap.get("AlarmState") != null) {
                    if (oldPanel.getAlarmState() == 2 && resultMap.get("AlarmState").equals(1)) { //报警变红就不能变黄了，除非已消警
                        newPanel.setAlarmState(oldPanel.getAlarmState());
                    } else {
                        newPanel.setAlarmState(Integer.parseInt(resultMap.get("AlarmState").toString()));
                    }
                }
                if (resultMap.get("ControlState") != null) {
                    newPanel.setControlState(resultMap.get("ControlState").toString());
                }
                if (resultMap.get("WorkLot") != null) {
                    newPanel.setWorkLot(resultMap.get("WorkLot").toString());
                }
                if (resultMap.get("NetState") != null) {
                    newPanel.setNetState(Integer.parseInt(resultMap.get("NetState").toString()));
                }
                if (resultMap.get("CommState") != null) {
                    newPanel.setNetState(Integer.parseInt(resultMap.get("CommState").toString()));
                    EquipState equipState = equipNodeBean.getEquipStateProperty();
                    equipState.setCommOn(true);
                    equipNodeBean.setEquipStateProperty(equipState);
                }
                equipNodeBean.setEquipPanelProperty(newPanel);
                break;
            }
        }
    }

}
