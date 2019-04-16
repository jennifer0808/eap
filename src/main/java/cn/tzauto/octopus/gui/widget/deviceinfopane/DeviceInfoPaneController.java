/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.gui.widget.deviceinfopane;

import cn.tzauto.octopus.biz.device.domain.DeviceInfo;
import cn.tzauto.octopus.biz.device.domain.DeviceInfoExt;
import cn.tzauto.octopus.biz.device.service.DeviceService;
import cn.tzauto.octopus.biz.recipe.domain.Recipe;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.biz.sys.domain.SysOffice;
import cn.tzauto.octopus.biz.sys.service.SysService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.common.globalConfig.GlobalConstants;
import cn.tzauto.octopus.common.util.language.languageUtil;
import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;

import javax.swing.*;
import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.ResourceBundle;


/**
 * FXML Controller class
 *
 * @author luosy
 */
public class DeviceInfoPaneController implements Initializable {
    private static final Logger logger = Logger.getLogger(DeviceInfoPaneController.class.getName());
    @FXML
    private TextField officeName;
    @FXML
    private TextField clientCode;
    @FXML
    private TextField recipeName;
    @FXML
    private TextField deviceCodeField;
    @FXML
    private TextField recipeVersionNo;
    @FXML
    private TextField equipStatus;
    @FXML
    private TextField lotNo;
    @FXML
    private TextField deviceType;

    @FXML
    private RadioButton JRB_EngineerMode;

    /**
     * Initializes the controller class.
     */
    private String deviceCode;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        officeName.setEditable(false);
        clientCode.setEditable(false);
        recipeName.setEditable(false);
        deviceCodeField.setEditable(false);
        recipeVersionNo.setEditable(false);
        equipStatus.setEditable(false);
        lotNo.setEditable(false);
        deviceType.setEditable(false);

    }


    private void buttonOKClick(Stage stage) {
        stage.close();
    }

    public void init(String deviceCode) {
        this.deviceCode = deviceCode;
        // TODO   
        Stage stage = new Stage();
        Pane root = new Pane();
        try {
            ResourceBundle resourceBundle = ResourceBundle.getBundle("eap", new languageUtil().getLocale());
            root = FXMLLoader.load(getClass().getClassLoader().getResource("DeviceInfoPane.fxml"), resourceBundle);
        } catch (IOException ex) {

        }
        Image image = new Image(DeviceInfoPaneController.class.getClassLoader().getResourceAsStream("logoTaiZhi.png"));
        stage.getIcons().add(image);
        stage.setTitle("设备详情");
        Scene scene = new Scene(root);
        stage.setScene(scene);
        initData(deviceCode, root);
        stage.show();
        stage.setResizable(false);
        Button button = (Button) root.lookup("#button");
        button.setOnAction((value) -> buttonOKClick(stage));

    }

    private void initData(String deviceCode, Pane root) {
        SqlSession sqlSession = MybatisSqlSession.getBatchSqlSession();
        DeviceService deviceService = new DeviceService(sqlSession);
        DeviceInfo deviceInfo = deviceService.selectDeviceInfoByDeviceCode(deviceCode);
        if (deviceInfo != null) {
            DeviceInfoExt deviceInfoExt = deviceService.getDeviceInfoExtByDeviceCode(deviceInfo.getDeviceCode());
            clientCode = (TextField) root.lookup("#clientCode");
            clientCode.setText(deviceInfo.getClientId());
            deviceCodeField = (TextField) root.lookup("#deviceCodeField");
            deviceCodeField.setText(deviceInfo.getDeviceCode());
            equipStatus = (TextField) root.lookup("#equipStatus");
            equipStatus.setText(deviceInfo.getDeviceStatus());
            deviceType = (TextField) root.lookup("#deviceType");
            deviceType.setText(deviceInfo.getDeviceType());

            lotNo = (TextField)root.lookup("#lotNo");
            recipeVersionNo = (TextField)root.lookup("#recipeVersionNo");
            JRB_EngineerMode = (RadioButton) root.lookup("#JRB_EngineerMode");
            officeName = (TextField)root.lookup("#officeName");
//            Map map = new HashMap();
            try {
//                map = (Map) statusMap.get(tempDeviceDode);
                Map map = GlobalConstants.stage.hostManager.getEquipInitState(deviceInfo.getDeviceCode());
                if (map == null || map.get("PPExecName") == null || map.isEmpty()) {
                   UiLogUtil.getInstance().appendLog2SecsTab(deviceInfo.getDeviceCode(), "获取设备当前状态信息失败，请检查设备状态.");
                    JOptionPane.showMessageDialog(null, "获取设备当前状态信息失败，请检查设备状态.");
//                    CommonUtil.alert("获取设备当前状态信息失败，请检查设备状态.");
                    recipeName.setText(deviceInfoExt.getRecipeName());
                    recipeName.setTooltip(new Tooltip(deviceInfoExt.getRecipeName()));
                    equipStatus.setText(deviceInfoExt.getDeviceStatus());
                } else {
//                   UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "测试 SwingWorker......");
//                    Thread.sleep(10000);
                    recipeName = (TextField) root.lookup("#recipeName");
                    recipeName.setText(String.valueOf(map.get("PPExecName")));
                    recipeName.setTooltip(new Tooltip(String.valueOf(map.get("PPExecName"))));
                    equipStatus = (TextField) root.lookup("#equipStatus");
                    equipStatus.setText(String.valueOf(map.get("EquipStatus")));
                }

                RecipeService recipeService = new RecipeService(sqlSession);
                if (deviceInfoExt != null) {
                    lotNo.setText(deviceInfoExt.getLotId());
                    if (deviceInfoExt.getRecipeId() != null && !"".equals(deviceInfoExt.getRecipeId())) {
                        Recipe recipe = recipeService.getRecipe(deviceInfoExt.getRecipeId());
                        if (recipe != null) {
                            recipeVersionNo.setText(recipe.getVersionNo().toString());
                        }
                    }
                    //工程模式
                    String businessMod = deviceInfoExt.getBusinessMod();
                    if ("Engineer".equals(businessMod)) {
                        JRB_EngineerMode.setSelected(true);
                    } else {
                        JRB_EngineerMode.setSelected(false);
                    }
                }
                SysService sysService = new SysService(sqlSession);
                if (deviceInfo.getOfficeId() != null && !"".equals(deviceInfo.getOfficeId())) {
                    SysOffice sysOffice = sysService.selectSysOfficeByPrimaryKey(deviceInfo.getOfficeId());
                    if (sysOffice != null) {
                        officeName.setText(sysOffice.getName());
                    }
                }
            } catch (Exception e) {
                logger.error("Exception:", e);
            } finally {
                sqlSession.close();
            }
        }

    }
}
