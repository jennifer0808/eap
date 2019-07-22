/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.gui.widget.equipstatuspane;

import cn.tzauto.octopus.common.globalConfig.GlobalConstants;
import cn.tzauto.octopus.common.ws.AvaryAxisUtil;
import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;
import cn.tzauto.octopus.gui.widget.deviceinfopane.DeviceInfoPaneController;
import cn.tzauto.octopus.gui.widget.svquerypane.SVQueryPaneController;
import cn.tzauto.octopus.isecsLayer.domain.EquipModel;
import cn.tzauto.octopus.secsLayer.domain.EquipNodeBean;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;

import javax.xml.rpc.ServiceException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.time.LocalDateTime;
import java.util.ResourceBundle;

/**
 * FXML Controller class
 *
 * @author luosy
 */
public class EquipStatusPaneController implements Initializable {


    private EquipNodeBean equipNodeBean;

    @FXML
    private Pane P_EquipPane;
    @FXML
    private ImageView equipImg;
    ContextMenu contextMenu = new ContextMenu();

    private boolean isOnPM = false;

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // TODO


    }


    @FXML
    private void mouseClick(MouseEvent event) throws IOException {
//        if (equipNodeBean.getEquipStateProperty().isCommOn()) {
        if (event.getButton().equals(MouseButton.PRIMARY)) {
            String deviceCodeTemp = ((Label) P_EquipPane.lookup("#L_DeviceCode")).getText();

            ;

            for (EquipNodeBean enb : GlobalConstants.stage.equipBeans) {
                if (deviceCodeTemp.equalsIgnoreCase(enb.getDeviceCode())) {
                    equipNodeBean = enb;
                    break;
                }
            }

            if (equipNodeBean.getEquipStateProperty().isCommOn() && GlobalConstants.sysUser != null) {
                contextMenu.hide();
                MenuItem menuItem = new MenuItem("设备详情");

                menuItem.setOnAction(actionEvent -> showDeviceInfo(deviceCodeTemp));

//                MenuItem menuItem1 = new MenuItem("SV数据查询");
//                menuItem1.setOnAction(actionEvent -> showSVQuery(deviceCodeTemp));
                //                menuItem1.setOnAction(actionEvent -> {
                //                    System.out.println("cn.tzinfo.htauto.octopus.gui.widget.equipstatu******************************menuItem2");
                //                });
                MenuItem menuItem1 = new MenuItem("保养");
                menuItem1.setOnAction(actionEvent -> showPMInfo(deviceCodeTemp));
                MenuItem menuItem2 = new MenuItem("报表数据重传");
                menuItem2.setOnAction(actionEvent -> reportInfoReUp(deviceCodeTemp));
                contextMenu = new ContextMenu(menuItem, menuItem1, menuItem2);

                contextMenu.show(P_EquipPane, event.getScreenX(), event.getScreenY());
            } else {
//                contextMenu.hide();
//                MenuItem menuItem2 = new MenuItem("开启连接");
//                menuItem2.setOnAction(actionEvent -> new EapClient().startComByEqp(equipNodeBean));
//                contextMenu = new ContextMenu(menuItem2);
//                contextMenu.show(P_EquipPane, event.getScreenX(), event.getScreenY());
            }

        } else {
            contextMenu.hide();
        }
        //   }
//      else{
//            return ;
//            //状态灰色-开启连接
//
//        }
    }


    /**
     * 设备详情
     */
    private void showDeviceInfo(String deviceCode) {
        if (DeviceInfoPaneController.flag.get(deviceCode) == null) {
            DeviceInfoPaneController.flag.put(deviceCode, false);
        } else if (DeviceInfoPaneController.flag.get(deviceCode)) {
            return;
        }
        try {
            new DeviceInfoPaneController(deviceCode).init();
        } catch (Exception e) {
            DeviceInfoPaneController.flag.put(deviceCode, false);
            e.printStackTrace();
        }
    }

    public void showSVQuery(String deviceCode) {

        if (SVQueryPaneController.flag.get(deviceCode) == null) {
            SVQueryPaneController.flag.put(deviceCode, false);
        } else if (SVQueryPaneController.flag.get(deviceCode)) {
            return;
        }
        try {
            new SVQueryPaneController(deviceCode).init();
        } catch (Exception e) {
            SVQueryPaneController.flag.put(deviceCode, false);
            e.printStackTrace();
        }

//        GlobalConstants.isSvQuery = true;
//        try {
//            new EapMainController().loginInterface();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

    }

    private void showPMInfo(String deviceCode) {
        if (isOnPM) {
            isOnPM = false;
            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "保养结束.");
            //todo 执行ocr解锁屏命令
//            GlobalConstants.stage.equipModels.get(deviceCode).iSecsHost.executeCommand("inputunlock");
            GlobalConstants.stage.equipModels.get(deviceCode).pmState.setPM(true);
            LocalDateTime now = LocalDateTime.now();
            GlobalConstants.stage.equipModels.get(deviceCode).pmState.setEndTime(now.format(AvaryAxisUtil.dtf2));
            try {
                GlobalConstants.stage.equipModels.get(deviceCode).uploadData("保养");
            } catch (RemoteException e) {
                e.printStackTrace();
            } catch (ServiceException e) {
                e.printStackTrace();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        } else {
            isOnPM = true;
            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "开始保养.");
            //todo 执行ocr锁屏命令
//            GlobalConstants.stage.equipModels.get(deviceCode).iSecsHost.executeCommand("inputlock");
            GlobalConstants.stage.equipModels.get(deviceCode).pmState.setPM(true);
            LocalDateTime now = LocalDateTime.now();
            GlobalConstants.stage.equipModels.get(deviceCode).pmState.setStartTime(now.format(AvaryAxisUtil.dtf2));
        }
    }

    private void reportInfoReUp(String deviceCode) {
        try {
            EquipModel equipModel = GlobalConstants.stage.equipModels.get(deviceCode);
            boolean data = equipModel.uploadData("生產");
            if (data) {
                equipModel.firstLot = true;
                UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "报表数据重传成功.");
            }
        } catch (Exception e) {
            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "报表数据重传失败.");
        }
    }
}
