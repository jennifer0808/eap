package cn.tzauto.octopus.gui.dialog.download;

import cn.tzauto.octopus.biz.device.domain.DeviceInfo;
import cn.tzauto.octopus.biz.device.domain.DeviceInfoExt;
import cn.tzauto.octopus.biz.device.service.DeviceService;
import cn.tzauto.octopus.biz.recipe.domain.Recipe;
import cn.tzauto.octopus.biz.recipe.domain.RecipeOperationLog;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.common.globalConfig.GlobalConstants;
import cn.tzauto.octopus.common.util.language.languageUtil;
import cn.tzauto.octopus.common.ws.AxisUtility;
import cn.tzauto.octopus.gui.guiUtil.CommonUtil;
import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;
import cn.tzauto.octopus.isecsLayer.domain.EquipModel;
import cn.tzauto.octopus.secsLayer.domain.EquipHost;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.net.URL;
import java.util.*;

/**
 * Created by wj_co on 2019/2/15.
 */
public class DownloadPaneController implements Initializable {
    private static final Logger logger = Logger.getLogger(DownloadPaneController.class);
    @FXML
    private TableColumn<SimpleRecipeUploadProperty, String> deviceCodeCol = new TableColumn<>();
    @FXML
    private TableColumn<SimpleRecipeUploadProperty, String> deviceNameCol = new TableColumn<>();
    @FXML
    private TableColumn isSelectCol;
    @FXML
    private TableView<SimpleRecipeUploadProperty> dataTable; //tableView

    @FXML
    private Label RcpName;

    private Recipe recipe;

//    @FXML
//    private TextField TX_EventLog;

    ObservableList<SimpleRecipeUploadProperty> list = FXCollections.observableArrayList(); //javaFX 的数据集合

    java.util.List<Recipe> recipes = new ArrayList<>();

    Parent root;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        deviceCodeCol.setCellValueFactory(celldata -> celldata.getValue().getDeviceCode());
        deviceNameCol.setCellValueFactory(celldata -> celldata.getValue().getDeviceName());
        isSelectCol.setCellValueFactory(new PropertyValueFactory<SimpleRecipeUploadProperty, CheckBox>("checkBox"));

    }


    public void init(String deviceCode, String recipeName, String versionType, String recipeVersionNo) {
        Stage stage = new Stage();
        stage.setTitle("Recipe 下载");
        AnchorPane downloadPane = new AnchorPane();
        try {
            ResourceBundle resourceBundle = ResourceBundle.getBundle("eap", new languageUtil().getLocale());
            downloadPane = FXMLLoader.load(getClass().getClassLoader().getResource("DownloadPane.fxml"), resourceBundle);

        } catch (IOException ex) {
        }
        stage.setResizable(false);
        Image image = new Image(getClass().getClassLoader().getResourceAsStream("logoTaiZhi.png"));
        stage.getIcons().add(image);
        Scene scene = new Scene(downloadPane);
        stage.setScene(scene);

        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        RecipeService recipeService = new RecipeService(sqlSession);
        //查询recipe表
        recipes = recipeService.searchRecipeByPara(recipeName, deviceCode, versionType, recipeVersionNo);

        recipe = recipes.get(0);

        DeviceService deviceService = new DeviceService(sqlSession);
        String deviceTypeId = recipe.getDeviceTypeId();

        List<DeviceInfo> deviceInfos = deviceService.getDeviceInfoByClientId(GlobalConstants.getProperty("clientId"));
        List<DeviceInfo> deviceInfostmp = new ArrayList<>();
        for (DeviceInfo deviceInfo : deviceInfos) {
            EquipHost equipHost = GlobalConstants.stage.equipHosts.get(deviceInfo.getDeviceId());
            if (equipHost != null && AxisUtility.isEngineerMode(deviceInfo.getDeviceCode()) && equipHost.getEquipState().isCommOn()) {
                deviceInfostmp.add(deviceInfo);
            }
            EquipModel equipModel = GlobalConstants.stage.equipModels.get(deviceInfo.getDeviceCode());
            if (equipModel != null) {
                deviceInfostmp.add(deviceInfo);
            }
        }
        sqlSession.close();

        RcpName = (Label) downloadPane.lookup("#RcpName");
        RcpName.setText(recipeName);

        //表数据
        dataTable = (TableView) downloadPane.lookup("#dataTable");

        if ("unique".equalsIgnoreCase(recipe.getVersionType())) {
            SimpleRecipeUploadProperty srp = new SimpleRecipeUploadProperty();
            srp.setDeviceName(recipe.getDeviceName());
            srp.setDeviceCode(recipe.getDeviceCode());
            list.add(srp);
        } else if (deviceInfostmp.size() > 0) {
            for (DeviceInfo deviceInfo : deviceInfostmp) {
                SimpleRecipeUploadProperty srp = new SimpleRecipeUploadProperty();
                if (deviceInfo.getDeviceTypeId().equals(deviceTypeId)) {
                    srp.setDeviceName(recipe.getDeviceName());
                    srp.setDeviceCode(recipe.getDeviceCode());
                    list.add(srp);
                }
            }
        }
        dataTable.setItems(list);


        stage.show();
        stage.setResizable(false);

        Button button = (Button) downloadPane.lookup("#BTNOK");
        button.setOnAction((value) -> btnOKClick(stage));

        Button buttonC = (Button) downloadPane.lookup("#BTNCancle");
        buttonC.setOnAction((value) -> btnCancelClick(stage));
    }

    private void btnOKClick(Stage stage) {
        java.util.List deviceCodes = new ArrayList();
        int flag = 0;

        for (int i = 0; i < list.size(); i++) {
            SimpleRecipeUploadProperty downList = list.get(i);
            if (downList.getCheckBox().isSelected()) {
                deviceCodes.add(downList.getDeviceCode().getValue());
                flag++;
            }
        }

        if (flag == 0) {
            CommonUtil.alert(Alert.AlertType.WARNING, "请选中一条或多条Recipe！");
            return;
        }
        if (flag > 1) {
            CommonUtil.alert(Alert.AlertType.WARNING, "目前只支持单台设备下载！");
            return;
        }
        Optional<ButtonType> alert = CommonUtil.alert(Alert.AlertType.CONFIRMATION, "将Recipe下载到已选设备?");
        if (alert.get() == ButtonType.OK) {
            SqlSession sqlSession = MybatisSqlSession.getSqlSession();
            RecipeService recipeService = new RecipeService(sqlSession);
            DeviceService deviceService = new DeviceService(sqlSession);
            Map mqMap = new HashMap();
            mqMap.put("msgName", "ReplyMessage");
            mqMap.put("eventId", "");
            mqMap.put("deviceCode", deviceCodes.get(0).toString());
            mqMap.put("eventName", "recipe下载结果");

            try {
                DeviceInfo deviceInfo = deviceService.selectDeviceInfoByDeviceCode(deviceCodes.get(0).toString());
                DeviceInfoExt deviceInfoExt = deviceService.getDeviceInfoExtByDeviceCode(deviceInfo.getDeviceCode());
                //recipe下载分为4种方式
                //1、Download   只下载
                //2、Select 做PPSelect
                //3、DeleteDownloadSelect 删除当前，并且下载当前的Recipe
                //4、DeleteAllDownloadSelect 删除设备上所有的，并且下载当前的，并且选中
                if (deviceInfoExt != null && deviceInfoExt.getRecipeDownloadMod() != null && !"".equals(deviceInfoExt.getRecipeDownloadMod())) {
                    GlobalConstants.sysLogger.info("设备模型表中配置设备" + deviceInfo.getDeviceCode() + "的Recipe下载方式为" + deviceInfoExt.getRecipeDownloadMod());
                    RecipeOperationLog recipeOperationLog = recipeService.setRcpOperationLog(recipe, "download");
                    //根据不同的下载模式，下载recipe
                    String downloadResult = recipeService.downLoadRcp2DeviceByType(deviceInfo, recipe, "", deviceInfoExt.getRecipeDownloadMod());
                    if ("0".equals(downloadResult)) {
                        deviceInfoExt.setRecipeId(recipe.getId());
                        deviceInfoExt.setRecipeName(recipe.getRecipeName());
                        deviceInfoExt.setVerNo(recipe.getVersionNo());
                        deviceService.modifyDeviceInfoExt(deviceInfoExt);
                        sqlSession.commit();
                        mqMap.put("eventDesc", "下载成功！");
                        recipeOperationLog.setOperationResult("Y");
                        //手动下成功给服务端发mq
                        sendDownloadResult2Server(deviceInfo.getDeviceCode());
                        CommonUtil.alert(Alert.AlertType.INFORMATION, "下载成功！");

                        UiLogUtil.appendLog2EventTab(deviceInfo.getDeviceCode(), "Recipe[" + recipe.getRecipeName() + "]下载成功");
                    } else {
                        CommonUtil.alert(Alert.AlertType.WARNING, "下载失败，请重试！");

                        UiLogUtil.appendLog2EventTab(deviceInfo.getDeviceCode(), "Recipe[" + recipe.getRecipeName() + "]下载失败，" + downloadResult);
                        mqMap.put("eventDesc", downloadResult);
                        recipeOperationLog.setOperationResult("N");
                        recipeOperationLog.setOperationResultDesc(downloadResult);
                    }
                    //保存下载结果至数据库并发送至服务端
                    recipeService.saveRecipeOperationLog(recipeOperationLog);
                    GlobalConstants.C2SRcpDownLoadQueue.sendMessage(mqMap);
                }

            } catch (Exception e) {
                CommonUtil.alert(Alert.AlertType.WARNING, "下载失败，请重试！");
                GlobalConstants.sysLogger.error(e.toString());
                sqlSession.rollback();
                logger.error("Exception:", e);
            } finally {
                sqlSession.close();
            }

            stage.close();
        }


    }

    private void btnCancelClick(Stage stage) {
        stage.close();
    }


    private void sendDownloadResult2Server(String deviceCode) {
        Map mqMap = new HashMap();
        mqMap.put("msgName", "RecipeDownloadResult");
        mqMap.put("deviceCode", deviceCode);
        mqMap.put("flag", "manual");
        GlobalConstants.C2SRcpDownLoadQueue.sendMessage(mqMap);
    }


}
