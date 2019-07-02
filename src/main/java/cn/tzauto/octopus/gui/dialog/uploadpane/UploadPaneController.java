/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.gui.dialog.uploadpane;

import cn.tzauto.octopus.biz.device.domain.DeviceInfo;
import cn.tzauto.octopus.biz.recipe.domain.Recipe;
import cn.tzauto.octopus.biz.recipe.domain.RecipeNameMapping;
import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.common.globalConfig.GlobalConstants;
import cn.tzauto.octopus.common.util.language.languageUtil;
import cn.tzauto.octopus.gui.guiUtil.CommonUiUtil;
import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;
import cn.tzauto.octopus.secsLayer.domain.EquipHost;
import cn.tzauto.octopus.secsLayer.domain.EquipNodeBean;
import cn.tzauto.octopus.secsLayer.domain.MultipleEquipHostManager;
import cn.tzauto.octopus.secsLayer.exception.UploadRecipeErrorException;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ExecutionException;

import static cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession.sqlSession;
import static cn.tzauto.octopus.common.globalConfig.GlobalConstants.*;
import static org.apache.axis.management.ServiceAdmin.start;

/**
 * FXML Controller class
 *
 * @author luosy
 */
public class UploadPaneController implements Initializable {
    private static Logger logger = Logger.getLogger(UploadPaneController.class);
    private MultipleEquipHostManager hostManager = GlobalConstants.stage.hostManager;
    private String deviceId;

    public static  Stage stage= new Stage();
    static {
        stage.setAlwaysOnTop(true);
        stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent event) {
                if (isUpload) {
                    isUpload = false;
                    onlyOnePageUpload = false;
                }
            }
        });


        Image image = new Image(UploadPaneController.class.getClassLoader().getResourceAsStream("logoTaiZhi.png"));

        stage.getIcons().add(image);
        stage.setTitle("Recipe上传");

    }
    List<DeviceInfo> deviceInfos;
    private String deviceType;
    Boolean isAlert = true ;
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
    @FXML
    private Button BTN_ok;
    @FXML
    private ProgressIndicator progressIndicatorLoad;
    @FXML
    private AnchorPane mainPane;
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
            ResourceBundle resourceBundle = ResourceBundle.getBundle("eap", new languageUtil().getLocale());
            rcpMngPane = FXMLLoader.load(getClass().getClassLoader().getResource("UploadPane.fxml"), resourceBundle);

        } catch (IOException ex) {

        }
        dataTable = (TableView<RecipeName>) rcpMngPane.lookup("#dataTable");
        dataTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        fillComboBox(rcpMngPane);
        Scene scene = new Scene(rcpMngPane);
        stage.setScene(scene);
        stage.show();
        stage.setResizable(false);
        Button button = (Button) rcpMngPane.lookup("#BTN_ok");
        button.setOnAction((value) -> btnOKClick());
        Button buttonC = (Button) rcpMngPane.lookup("#BTN_cancel");
        buttonC.setOnAction((value) -> btnCancelClick());
        progressIndicatorLoad=(ProgressIndicator) rcpMngPane.lookup("#progressIndicatorLoad");
        mainPane=(AnchorPane)rcpMngPane.lookup("#mainPane");
        CMB_deviceCode = (ComboBox) rcpMngPane.lookup("#CMB_deviceCode");

        CMB_deviceCode.getSelectionModel().selectedItemProperty().addListener(
                (ObservableValue observable, Object oldValue, Object newValue) -> fillTabView());

    }

    void fillComboBox(Pane rcpMngPane) {
        CMB_deviceCode = (ComboBox) rcpMngPane.lookup("#CMB_deviceCode");
        ObservableList deviceCodeList = FXCollections.observableArrayList();

        for (EquipNodeBean equipNodeBean : GlobalConstants.stage.equipBeans) {
            deviceCodeList.add(equipNodeBean.getDeviceCode());
        }
        CMB_deviceCode.setItems(deviceCodeList);
    }

    @FXML
    void fillTabView() {



        String deviceCodeTmp = CMB_deviceCode.getSelectionModel().getSelectedItem().toString();
            System.out.println(deviceCodeTmp);


            for (DeviceInfo deviceInfo : GlobalConstants.deviceInfos) {
            if (deviceInfo.getDeviceCode().equals(deviceCodeTmp)) {
                this.deviceType = deviceInfo.getDeviceType();
                deviceId = deviceInfo.getDeviceCode();
                Task task = new Task<Map>() {
                    @Override
                    public Map call() {

                        Map resultMap = hostManager.getRecipeListFromDevice(deviceInfo.getDeviceCode());
                        setDataCombox(resultMap);
                        return resultMap;
                    }
                };
                new Thread(task).start();
                dataTable.setItems(recipeNames);
                break;
            }
        }
    }


    public  void setDataCombox(Map resultMap){
        if ( resultMap== null || resultMap.size()==0) {
            Platform.runLater(() -> {
                CommonUiUtil.alert(Alert.AlertType.WARNING, "未正确收到回复，请检查设备通信状态！",stage);
            });
        }else{
            List<String> eppd = new ArrayList<>();
            eppd = (ArrayList) resultMap.get("eppd");
            recipeNames = FXCollections.observableArrayList();
            for (int i = 0; i < eppd.size(); i++) {
                recipeNames.add(new RecipeName(deviceId, eppd.get(i), i + 1));
            }
            dataTable.setItems(recipeNames);
        }
    }
    private void btnOKClick() {
        int flag = 0;
        for (int i = 0; i < recipeNames.size(); i++) {
            RecipeName rn = recipeNames.get(i);
            if (rn.getCheckBox().isSelected()) {
                flag++;
            }
        }
        if (flag == 0) {
            CommonUiUtil.alert(Alert.AlertType.WARNING, "请选中一条或多条Recipe！", stage);
            return;
        }
        if (flag > 20) {
            CommonUiUtil.alert(Alert.AlertType.WARNING, "批量上传一次不得多于20条，请重试！", stage);
            return;
        }
        List rns = new ArrayList<>();

        try {
            for (int i = 0; i < recipeNames.size(); i++) {
                RecipeName rn = recipeNames.get(i);
                if (rn.getCheckBox().isSelected()) {
                    String recipeName = rn.getRecipeName().getValue();
                    rns.add(recipeName);
                    if (CMB_deviceCode.getSelectionModel().getSelectedItem() != null) {
                        String deviceCodeTmp = CMB_deviceCode.getSelectionModel().getSelectedItem().toString();
                        if ("--请选择--".equals(deviceCodeTmp) || deviceCodeTmp.equals(deviceCode)) {
                            deviceCode = "";
                        }
                        deviceCode = deviceCodeTmp;
                    }
                    if (deviceCode.equals("")) {
//                        CommonUiUtil.alert(Alert.AlertType.WARNING, "请输入正确的用户名和密码！", loginStage);
                        GlobalConstants.stage.hostManager.isecsUploadMultiRecipe(deviceId, recipeNames);
                        return;
                    }
                    EquipHost equipHost =   GlobalConstants.stage.equipHosts.get(deviceCode);
                    Task task = new Task<Map>() {
                        @Override
                        public Map call() {
                            if( equipHost.getDeviceType().equals(sysProperties.get("ProgressFlag"))){
                                progressIndicatorLoad.setVisible(true);
                                mainPane.setDisable(true);
                            }
                            Map recipeMap = null;
                            try {
                                recipeMap = GlobalConstants.stage.hostManager.getRecipeParaFromDevice(deviceCode, recipeName);
                                upload(recipeMap,recipeName);
                                progressIndicatorLoad.setVisible(false);
                                mainPane.setDisable(false);
                            } catch (UploadRecipeErrorException e) {
                                progressIndicatorLoad.setVisible(false);
                                mainPane.setDisable(false);
                                Platform.runLater(() -> {
                                    CommonUiUtil.alert(Alert.AlertType.WARNING, "未正确收到回复，请检查设备通信状态！", stage);
                                });
                                e.printStackTrace();
                            }
                            return recipeMap;
                        }
                    };
                    new Thread(task).start();

                }}
            }catch(Exception e){
                sqlSession.rollback();
                logger.error("Exception:", e);
                CommonUiUtil.alert(Alert.AlertType.WARNING, "上传失败！请重试！", stage);
                return;
            }finally{
                sqlSession.close();
            }

//            stage.close();
//            isUpload = false;
//            onlyOnePageUpload = false;


    }

    public void upload(Map recipeMap,String recipeName){
        try {
        SqlSession sqlSession = MybatisSqlSession.getBatchSqlSession();
        RecipeService recipeService = new RecipeService(sqlSession);

            if (recipeMap == null || recipeMap.get("uploadResult").equals(false)) {
                Platform.runLater(() -> {
                    CommonUiUtil.alert(Alert.AlertType.WARNING, "未正确收到回复，请检查设备通信状态！",stage);
                });
                return;
//                        JOptionPane.showMessageDialog(null, "未正确收到回复，请检查设备通信状态！");
            } else if (recipeMap.get("checkResult") != null) {
                Platform.runLater(() -> {
                    CommonUiUtil.alert(Alert.AlertType.WARNING, (String) recipeMap.get("checkResult"),stage);
                });
//                       JOptionPane.showMessageDialog(null, recipeMap.get("checkResult"));
              return;
            }
                //此处从map中获取
                Recipe recipe = null;
                if (recipeMap.get("recipe") != null) {
                    recipe = (Recipe) recipeMap.get("recipe");
                }
                if ("N".equals(recipeMap.get("shortNameOK"))) {
                    Platform.runLater(()->{
                        CommonUiUtil.alert(Alert.AlertType.WARNING, "短号：[" + recipeName + "]在设备 " + deviceCode + " 上已被使用，请重新命名后上传！！", stage);
                        UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "短号：[" + recipeName + "]已被使用，请重新命名后上传！");
                        return;
                    });
//                    JOptionPane.showMessageDialog(null, "短号：[" + recipeName + "]在设备 " + deviceCode + " 上已被使用，请重新命名后上传！！");

                }
                //T640如果三个文件没有全部上传成功，即可认定为上传失败，不走上传流程，rcpAnalyseSucceed为N表示上传失败
                if (recipeMap.get("rcpAnalyseSucceed") == null || "Y".equals(String.valueOf(recipeMap.get("rcpAnalyseSucceed")))) {
                    List<RecipePara> recipeParaList = (List<RecipePara>) recipeMap.get("recipeParaList");
                    RecipeNameMapping recipeNameMapping = (RecipeNameMapping) recipeMap.get("recipeNameMapping");
                    //保存数据
                    boolean re;
                    if (recipeNameMapping != null) {
                        re = recipeService.saveUpLoadRcpInfo(recipe, recipeParaList, deviceCode, recipeNameMapping);
                    } else {
                        re = recipeService.saveUpLoadRcpInfo(recipe, recipeParaList, deviceCode);
                    }

                    //打日志
                    if (!re) {
                        Platform.runLater(()->{
                            CommonUiUtil.alert(Alert.AlertType.WARNING, "上传失败，ftp文件传送失败，请重新上传", stage);
                            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "上传失败，ftp文件传送失败，请重新上传");
                            isAlert = false;
                        });

                    } else {
                        UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "Recipe[" + recipeName + "]上传成功！");
                    }
                    sqlSession.commit();
                } else {
                    UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "Recipe[" + recipeName + "]上传失败，请重试！");
                }

        if (isAlert) {
            Platform.runLater(()->{
                Optional<ButtonType> result = CommonUiUtil.alert(Alert.AlertType.WARNING, "上传 结束，请到Recipe管理界面进行查看！", stage);
                if (result.get() == ButtonType.OK) {
                    stage.close();
                    isUpload = false;
                    onlyOnePageUpload = false;
                    return;
                }
            });
        }
        }catch(Exception e){
            sqlSession.rollback();
            logger.error("Exception:", e);
            Platform.runLater(()->{
                CommonUiUtil.alert(Alert.AlertType.WARNING, "上传失败！请重试！", stage);
                    });
            return;
        }finally{
            sqlSession.close();
        }

    }

    private void btnCancelClick() {
        stage.close();
        isUpload = false;
        onlyOnePageUpload = false;
    }

    String deviceCode = null;

}
