/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.gui.widget.equipstatuspane;

import cn.tzauto.octopus.biz.device.domain.DeviceInfo;
import cn.tzauto.octopus.biz.device.service.DeviceService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.secsLayer.domain.EquipNodeBean;
import cn.tzauto.octopus.secsLayer.util.FengCeConstant;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Paint;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;

import java.util.HashMap;

/**
 * @author luosy
 */
public class EquipStatusPane {

    public EquipNodeBean equipNodeBean;
    public String deviceId;
    public String deviceCode;
    public int alarmState;
    public String controlState;
    @FXML
    private Label L_DeviceCode;
    @FXML
    private Label L_DeviceType;
    @FXML
    private Label L_RunStatus;
    @FXML
    private Label L_LotId;
    @FXML
    private Label L_RecipeName;
    @FXML
    private Pane P_EquipPane;
    @FXML
    private ImageView equipImg;
    public Pane equipStatusPane;
    private static final Logger logger = Logger.getLogger(EquipStatusPaneController.class);

    Background bgYellow = new Background(new BackgroundFill(Paint.valueOf("#e3d81c"), CornerRadii.EMPTY, Insets.EMPTY));
    Background bgRed = new Background(new BackgroundFill(Paint.valueOf("#ff4500"), CornerRadii.EMPTY, Insets.EMPTY));
    Background bgBlue = new Background(new BackgroundFill(Paint.valueOf("#472EF5"), CornerRadii.EMPTY, Insets.EMPTY));
    Background bgGreen = new Background(new BackgroundFill(Paint.valueOf("#009a44"), CornerRadii.EMPTY, Insets.EMPTY));
    public static   Background bgGray = new Background(new BackgroundFill(Paint.valueOf("#494c53"), CornerRadii.EMPTY, Insets.EMPTY));
    Border borderWhite = new Border(new BorderStroke(Paint.valueOf("#ffffff"), BorderStrokeStyle.NONE, CornerRadii.EMPTY, BorderWidths.EMPTY));
    Border borderBlue = new Border(new BorderStroke(Paint.valueOf("#472EF5"), BorderStrokeStyle.NONE, CornerRadii.EMPTY, BorderWidths.EMPTY));

    public EquipStatusPane(EquipNodeBean equipNodeBean, Pane equipStatusPane) {
        this.equipNodeBean = equipNodeBean;
        L_DeviceCode = (Label) equipStatusPane.lookup("#L_DeviceCode");
        L_DeviceType = (Label) equipStatusPane.lookup("#L_DeviceType");
        L_RunStatus = (Label) equipStatusPane.lookup("#L_RunStatus");
        L_LotId = (Label) equipStatusPane.lookup("#L_LotId");
        L_RecipeName = (Label) equipStatusPane.lookup("#L_RecipeName");
        P_EquipPane = (Pane) equipStatusPane.lookup("#P_EquipPane");
        equipImg = (ImageView) equipStatusPane.lookup("#equipImg");

//        P_EquipPane.addEventHandler(MouseEvent.MOUSE_CLICKED, (event) -> {
//            if (MouseButton.SECONDARY.equals(event.getButton())) {
//                MenuItem menuItem = new MenuItem("123456123");
//                ContextMenu contextMenu = new ContextMenu(menuItem);
////                MenuBar menuBar = new MenuBar();
//                contextMenu.show(P_EquipPane, event.getX(), event.getY());
////                menuBar.setContextMenu(contextMenu);
//
////                P_EquipPane.getChildren().add(menuBar);
////                Popup popup = new Popup();
////                popup.show(P_EquipPane, event.getX(), event.getY());
//            }
//        });
        this.equipStatusPane = equipStatusPane;
    }


    public void setDeviceCode(String deviceCode) {
        this.L_DeviceCode.setText(deviceCode);
    }

    public void setDeviceCodeAndDeviceType(String deviceCode) {
        this.L_DeviceCode.setText(deviceCode);
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        DeviceService deviceService = new DeviceService(sqlSession);
        DeviceInfo deviceInfo = deviceService.selectDeviceInfoByDeviceCode(deviceCode);
        sqlSession.close();
        this.L_DeviceType.setText(deviceInfo.getDeviceType());
        this.L_DeviceType.setTooltip(new Tooltip(deviceInfo.getDeviceType()));
    }

    public String getDeviceCode() {
        return this.L_DeviceCode.getText();
    }

    private static HashMap images=new HashMap();
    public void setCommLabelForegroundColorCommOn() {
//        Icon eqpIcon = new ImageIcon(EquipStatusPanel.class.getResource(equipNodeBean.getIconPath()));
        String iconPath = equipNodeBean.getIconPath();
        Image image=null;
        if (iconPath.contains("/")) {
            iconPath = iconPath.substring(iconPath.lastIndexOf("/") + 1);
        }
        if(images.get(iconPath)!=null){
            image=(Image) images.get(iconPath);
        }else{
            image = new Image(getClass().getClassLoader().getResource(iconPath).toString());
            images.put(iconPath, image);
        }
        logger.info("on"+deviceCode +"图片为"+image);
        this.equipImg.setImage(image);
        this.P_EquipPane.setBackground(bgGreen);
    }

    public void setCommLabelForegroundColorCommOff() {
//        Icon eqpIcon = new ImageIcon(EquipStatusPanel.class.getResource(equipNodeBean.getIconPath().replaceAll(".jpg", "-commoff.jpg")));
        String iconPath = equipNodeBean.getIconPath();
        if (iconPath.contains("/")) {
            iconPath = iconPath.substring(iconPath.lastIndexOf("/") + 1);
        }
        Image image=null;
        String lastName = iconPath.split("\\.")[1];
        String commofficonpath = iconPath.replaceAll("." + lastName, "-commoff." + lastName);
        if(images.get(iconPath)==null){
            image = new Image(getClass().getClassLoader().getResource(commofficonpath).toString());
            images.put(iconPath, image);

        }else if(images.get(iconPath)!=null){
            image=(Image) images.get(iconPath);
        }
        logger.info("off"+deviceCode +"图片为"+image);
        this.equipImg.setImage(image);
        this.P_EquipPane.setBackground(bgGray);
    }

    public void setRunStatus(String eventText) {
//        if (eventText != null && eventText.length() > 15) {
//            this.L_RunStatus.setText(eventText.substring(0, 12) + "...");
//        } else {
//            this.L_RunStatus.setText(eventText);
//        }
        Platform.runLater(()
                -> this.L_RunStatus.setText(eventText)
        );



    }

    public String getRunStatus() {
        return this.L_RunStatus.getText();
    }

    public void setLotId(String lotId) {
        L_LotId = (Label) equipStatusPane.lookup("#L_LotId");
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                L_LotId.setTooltip(new Tooltip(lotId));
                if (lotId != null && lotId.length() > 15) {
                    L_LotId.setText(lotId.substring(0, 12) + "...");
                } else {

                    L_LotId.setText(lotId);

                }
            }
        });



    }

    public String getLotId() {
        return this.L_LotId.getTooltip().getText();
    }

    public void setRunningRcp(String runningRcp) {
        this.L_RecipeName.setTooltip(new Tooltip(runningRcp));
        if (runningRcp != null && runningRcp.length() > 15) {
            Platform.runLater(()
                    -> this.L_RecipeName.setText(runningRcp.substring(0, 12) + "...")
            );
        } else {
            Platform.runLater(()
                    -> this.L_RecipeName.setText(runningRcp)
            );
        }
    }

    public String getRunningRcp() {
        return this.L_RecipeName.getTooltip().getText();
    }

    public int getAlarmState() {
        return alarmState;
    }

    public void setAlarmState(int alarmState) {
        this.alarmState = alarmState;
        switch (alarmState) {
            case 0:
                this.P_EquipPane.setBackground(bgGreen);//深绿色
                break;
            case 1:
                this.P_EquipPane.setBackground(bgYellow);//黄色
                break;
            case 2:
                this.P_EquipPane.setBackground(bgRed);//浅红色
                break;
            default:
                break;
        }
    }

    public String getControlState() {
        return controlState;
    }

    public void setControlState(String controlState) {
        this.controlState = controlState;
        P_EquipPane.setBorder(borderWhite);
        switch (controlState) {
            case FengCeConstant.CONTROL_LOCAL_ONLINE:
//                Platform.runLater(() -> P_EquipPane.setBackground(bgBlue));//蓝色,实现onLocal/offLocal功能按钮
                Platform.runLater(() -> P_EquipPane.setBackground(bgGreen));//深绿色
                break;
            case FengCeConstant.CONTROL_REMOTE_ONLINE:
                Platform.runLater(() -> P_EquipPane.setBackground(bgGreen));//深绿色
                break;
            case FengCeConstant.CONTROL_OFFLINE:
                Platform.runLater(() -> P_EquipPane.setBackground(bgGray));//灰色
                break;
        }
    }

    public void setControlStateSpecial(String controlState) {
        this.controlState = controlState;
        switch (controlState) {
            case FengCeConstant.CONTROL_LOCAL_ONLINE:
                this.P_EquipPane.setBorder(borderBlue);
                break;
            case FengCeConstant.CONTROL_REMOTE_ONLINE:
                this.P_EquipPane.setBorder(borderWhite);
                break;
        }
    }

}
