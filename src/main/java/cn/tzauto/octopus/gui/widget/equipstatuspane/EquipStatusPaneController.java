/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.gui.widget.equipstatuspane;

import cn.tzauto.octopus.gui.main.EapClient;
import cn.tzauto.octopus.gui.main.EapMainController;
import cn.tzauto.octopus.common.globalConfig.GlobalConstants;
import cn.tzauto.octopus.gui.widget.deviceinfopane.DeviceInfoPaneController;
import cn.tzauto.octopus.secsLayer.domain.EquipNodeBean;
import cn.tzauto.octopus.secsLayer.util.DeviceComm;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import static cn.tzauto.octopus.gui.main.EapClient.equipBeans;

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


    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // TODO


    }

    public static String tempDeviceDode = null;

    @FXML
    private void mouseClick(MouseEvent event) throws IOException {
//        if (equipNodeBean.getEquipStateProperty().isCommOn()) {
        if (event.getButton().equals(MouseButton.SECONDARY)) {
            String L_DeviceCode = ((Label) P_EquipPane.lookup("#L_DeviceCode")).getText();

            tempDeviceDode = L_DeviceCode;

            for (EquipNodeBean enb : equipBeans) {
                if (L_DeviceCode.equalsIgnoreCase(enb.getDeviceIdProperty())) {
                    equipNodeBean = enb;
                    break;
                }
            }

            if (equipNodeBean.getEquipStateProperty().isCommOn()) {
                contextMenu.hide();
                MenuItem menuItem = new MenuItem("设备详情");

                menuItem.setOnAction(actionEvent -> showDeviceInfo());

                MenuItem menuItem1 = new MenuItem("SV数据查询");
                menuItem1.setOnAction(actionEvent -> showSVQuery());
                //                menuItem1.setOnAction(actionEvent -> {
                //                    System.out.println("cn.tzinfo.htauto.octopus.gui.widget.equipstatu******************************menuItem2");
                //                });
                contextMenu = new ContextMenu(menuItem, menuItem1);

                contextMenu.show(P_EquipPane, event.getScreenX(), event.getScreenY());
            } else {
                contextMenu.hide();
                MenuItem menuItem2 = new MenuItem("开启连接");
                menuItem2.setOnAction(actionEvent -> new EapClient().startModel());
                contextMenu = new ContextMenu(menuItem2);
                contextMenu.show(P_EquipPane, event.getScreenX(), event.getScreenY());
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
    private void showDeviceInfo() {
        new DeviceInfoPaneController().init();

    }

    public void showSVQuery() {
        GlobalConstants.isSvQuery = true;
        try {
            new EapMainController().loginInterface();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
