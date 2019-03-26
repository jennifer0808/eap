/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.gui.dialog.uploadpane;

import cn.tzauto.octopus.biz.device.domain.DeviceInfo;
import cn.tzauto.octopus.biz.device.service.DeviceService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.common.globalConfig.GlobalConstants;
import cn.tzauto.octopus.common.util.language.languageUtil;
import cn.tzauto.octopus.gui.guiUtil.CommonUtil;
import cn.tzauto.octopus.secsLayer.domain.EquipNodeBean;
import cn.tzauto.octopus.secsLayer.domain.MultipleEquipHostManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import org.apache.ibatis.session.SqlSession;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * FXML Controller class
 *
 * @author luosy
 */
public class UploadPaneController implements Initializable {
    private MultipleEquipHostManager hostManager = GlobalConstants.stage.hostManager;
    private String deviceId;
    List<DeviceInfo> deviceInfos;
    private String deviceType;

    @FXML
    private TableView<RecipeName> dataTable; //tableView
    @FXML
    private TableColumn<RecipeName, String> numberCol = new TableColumn<>();
    @FXML
    private TableColumn<RecipeName, String> recipeNameCol = new TableColumn<>();
    @FXML
    private TableColumn<RecipeName, String> recipeCodeCol = new TableColumn<>();
    @FXML
    private TableColumn saveCol;
    @FXML
    private ComboBox CMB_deviceCode;
//  private TableColumn<SimpleRecipeProperty, String> numberCol = new TableColumn<>();

    ObservableList<RecipeName> recipeNames = FXCollections.observableArrayList();

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        numberCol.setCellValueFactory(celldata -> celldata.getValue().getNum());
        recipeCodeCol.setCellValueFactory(celldata -> celldata.getValue().getRecipeCode());
        recipeNameCol.setCellValueFactory(celldata -> celldata.getValue().getRecipeName());
        saveCol.setCellValueFactory(new PropertyValueFactory<RecipeName, CheckBox>("checkBox"));
    }

    public class RecipeName {
        public SimpleStringProperty recipeCode = new SimpleStringProperty();
        public SimpleStringProperty recipeName = new SimpleStringProperty();
        public SimpleStringProperty num = new SimpleStringProperty();
        public CheckBox checkBox = new CheckBox();

        public RecipeName(String recipeCode, String recipeName, int num) {
            this.recipeCode.setValue(recipeCode);
            this.recipeName.setValue(recipeName);
            this.num.setValue(String.valueOf(num));
        }

        public SimpleStringProperty getRecipeCode() {
            return recipeCode;
        }

        public SimpleStringProperty getRecipeName() {
            return recipeName;
        }

        public SimpleStringProperty getNum() {
            return num;
        }

        public CheckBox getCheckBox() {
            return checkBox;
        }

    }

    public void init() {

        Pane rcpMngPane = new Pane();
        try {
            ResourceBundle resourceBundle = ResourceBundle.getBundle("eap",new languageUtil().getLocale());
            rcpMngPane = FXMLLoader.load(getClass().getClassLoader().getResource("UploadPane.fxml"),resourceBundle);

        } catch (IOException ex) {

        }
        dataTable = (TableView<RecipeName>) rcpMngPane.lookup("#dataTable");
        dataTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        fillComboBox(rcpMngPane);
        Stage stage = new Stage();
        Image image = new Image(getClass().getClassLoader().getResourceAsStream("logoTaiZhi.png"));
        stage.getIcons().add(image);
        stage.setTitle("Recipe上传");
        Scene scene = new Scene(rcpMngPane);
        stage.setScene(scene);
        stage.show();
        stage.setResizable(false);
        Button button = (Button) rcpMngPane.lookup("#BTN_ok");
        button.setOnAction((value) -> btnOKClick(stage));
        Button buttonC = (Button) rcpMngPane.lookup("#BTN_cancel");
        buttonC.setOnAction((value) -> btnCancelClick(stage));
        CMB_deviceCode = (ComboBox) rcpMngPane.lookup("#CMB_deviceCode");

        CMB_deviceCode.getSelectionModel().selectedItemProperty().addListener(
                (ObservableValue observable, Object oldValue, Object newValue) -> fillTabView());

    }

    void fillComboBox(Pane rcpMngPane) {
        CMB_deviceCode = (ComboBox) rcpMngPane.lookup("#CMB_deviceCode");
        ObservableList deviceCodeList = FXCollections.observableArrayList();

        for (EquipNodeBean equipNodeBean : GlobalConstants.stage.equipBeans) {
            deviceCodeList.add(equipNodeBean.getEquipName());
        }
        CMB_deviceCode.setItems(deviceCodeList);
    }

    @FXML
    void fillTabView() {
        String deviceCodeTmp = CMB_deviceCode.getSelectionModel().getSelectedItem().toString();
        System.out.println(deviceCodeTmp);
        List<String> eppd = new ArrayList<>();


//        //获取机台控制的所有设备编号
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        DeviceService deviceService = new DeviceService(sqlSession);
        deviceInfos = deviceService.getDeviceInfoByClientId(GlobalConstants.getProperty("clientId"));

        for (DeviceInfo deviceInfo : deviceInfos) {
            if (deviceInfo.getDeviceCode().equals(deviceCodeTmp)) {
                this.deviceType = deviceInfo.getDeviceType();
                deviceId = deviceInfo.getDeviceId();
                break;
            }
        }

        Map resultMap = hostManager.getRecipeListFromDevice(deviceId);
        if (resultMap == null) {
//            JOptionPane.showMessageDialog(null, "未正确收到回复，请检查设备通信状态！");
            CommonUtil.alert(Alert.AlertType.WARNING,"未正确收到回复，请检查设备通信状态！");
            return;
        }
        eppd = (ArrayList) resultMap.get("eppd");

        for (int i = 0; i < eppd.size(); i++) {
            recipeNames.add(new RecipeName(deviceId, eppd.get(i), i + 1));
        }

        dataTable.setItems(recipeNames);

    }

    private void btnOKClick(Stage stage) {
        int flag = 0;

        for (int i = 0; i < recipeNames.size(); i++) {
            RecipeName rn = recipeNames.get(i);
            if (rn.getCheckBox().isSelected()) {
                flag++;
            }
        }

        if (flag == 0) {
            CommonUtil.alert(Alert.AlertType.WARNING,"请选中一条或多条Recipe！");
            return;
        }

        if (flag > 20) {
            CommonUtil.alert(Alert.AlertType.WARNING,"批量上传一次不得多于20条，请重试！");
            return;
        }

        List rns = new ArrayList<>();

        for (int i = 0; i < recipeNames.size(); i++) {
            RecipeName rn = recipeNames.get(i);
            if (rn.getCheckBox().isSelected()) {
                String recipeName = rn.getRecipeName().getValue();
                rns.add(recipeName);
                System.out.println(recipeName);
                if (CMB_deviceCode.getSelectionModel().getSelectedItem() != null) {
                    String deviceCodeTmp = CMB_deviceCode.getSelectionModel().getSelectedItem().toString();
                    if ("--请选择--".equals(deviceCodeTmp) || deviceCodeTmp.equals(deviceCode)) {
                        deviceCode = "";
                    }
                    deviceCode = deviceCodeTmp;
                }

                if (deviceCode.equals("")) {
                    CommonUtil.alert(Alert.AlertType.WARNING,"请输入正确的用户名和密码！");
                    GlobalConstants.stage.hostManager.isecsUploadMultiRecipe(deviceId, recipeNames);
                    return;
                }

                GlobalConstants.stage.hostManager.getRecipeParaFromDevice(deviceCode, recipeName);
            }
        }

        GlobalConstants.stage.hostManager.isecsUploadMultiRecipe(deviceId, rns);

        CommonUtil.alert(Alert.AlertType.WARNING,"上传结束，请到Recipe管理界面进行查看！");

        stage.close();

    }

    private void btnCancelClick(Stage stage) {
        stage.close();
    }

    String deviceCode = null;

}
