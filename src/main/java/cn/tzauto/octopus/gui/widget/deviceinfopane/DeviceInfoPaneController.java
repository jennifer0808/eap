/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.gui.widget.deviceinfopane;

import cn.tzauto.generalDriver.exceptions.BrokenProtocolException;
import cn.tzauto.generalDriver.exceptions.HsmsProtocolNotSelectedException;
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
import cn.tzauto.octopus.gui.guiUtil.CommonUiUtil;
import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;
import cn.tzauto.octopus.gui.main.EapClient;
import cn.tzauto.octopus.secsLayer.domain.EquipHost;
import cn.tzauto.octopus.secsLayer.util.FengCeConstant;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;

import javax.swing.*;
import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.*;

import static cn.tzauto.generalDriver.mid.HsmsProtocol.NOT_CONNECTED;
import static cn.tzauto.octopus.secsLayer.domain.EquipHost.*;



/**
 * FXML Controller class
 *
 * @author luosy
 */
public class DeviceInfoPaneController implements Initializable {
    private static final Logger logger = Logger.getLogger(DeviceInfoPaneController.class);
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

    public   Stage stage= new Stage();
    public static Map<String,Boolean>  flag = new HashMap<>();
    /**
     * Initializes the controller class.
     */
    private final String deviceCode;
    private  String statu="";
    private  String lotId="";
    private  String panerecipeName="";
    public DeviceInfoPaneController( ) {
        this.deviceCode = null;
        stage.setTitle("设备详情");
        stage.setAlwaysOnTop(true);
        stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent event) {
                flag.put(deviceCode,false) ;
            }
        });
    }
    public DeviceInfoPaneController( String dc,String statu,String lotId,String recipeName) {
        this.deviceCode = dc;
        this.statu=statu;
        this.lotId=lotId;
        this.panerecipeName=recipeName;
        stage.setTitle("设备详情");
        stage.setAlwaysOnTop(true);
        stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent event) {
                flag.put(deviceCode,false) ;
            }
        });
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
//        officeName.setEditable(false);
//        clientCode.setEditable(false);
//        recipeName.setEditable(false);
//        deviceCodeField.setEditable(false);
//        recipeVersionNo.setEditable(false);
//        equipStatus.setEditable(false);
//        lotNo.setEditable(false);
//        deviceType.setEditable(false);

    }


    private void buttonOKClick(Stage stage) {
        stage.close();
        flag.put(deviceCode,false) ;
    }

    public void init() {
        flag.put(deviceCode,true) ;
//        this.deviceCode = deviceCode;
        // TODO   
        Pane root = new Pane();
        try {
            ResourceBundle resourceBundle = ResourceBundle.getBundle("eap", new languageUtil().getLocale());
            root = FXMLLoader.load(getClass().getClassLoader().getResource("DeviceInfoPane.fxml"), resourceBundle);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        initData(deviceCode, root);
        Image image = new Image(DeviceInfoPaneController.class.getClassLoader().getResourceAsStream("logoTaiZhi.png"));
        stage.getIcons().add(image);

        Scene scene = new Scene(root);
        stage.setScene(scene);

        stage.show();
        stage.setResizable(false);
        Button button = (Button) root.lookup("#button");
        button.setOnAction((value) -> buttonOKClick(stage));

    }

    private void initData(String deviceCode, Pane root) {
        SqlSession sqlSession = MybatisSqlSession.getBatchSqlSession();
        DeviceService deviceService = new DeviceService(sqlSession);
        DeviceInfo    deviceInfo = deviceService.selectDeviceInfoByDeviceCode(deviceCode);
        DeviceInfoExt  deviceInfoExt = deviceService.getDeviceInfoExtByDeviceCode(deviceInfo.getDeviceCode());
        RecipeService recipeService = new RecipeService(sqlSession);
        SysService sysService = new SysService(sqlSession);
        EquipHost equipHost = GlobalConstants.stage.equipHosts.get(deviceCode);

//        boolean  testInitLink=false;
//        try {
//              testInitLink=equipHost.testInitLink();
//        } catch (BrokenProtocolException e) {
//            e.printStackTrace();
//        } catch (HsmsProtocolNotSelectedException e) {
//            e.printStackTrace();
//        }
        Task taskCheckComm=new Task() {
            @Override
            protected Object call() throws Exception {
                checkCommState();
                return null;
            }
        };
        new Thread(taskCheckComm).start();


        boolean isCommon= equipHost.getEquipState().isCommOn();
        int commState= equipHost.commState;
        logger.info("isCommon=====>"+isCommon+";commState=====>"+commState+";testInitLink=====>");
        if(commState==COMMUNICATING ||isCommon==true ){
            Task task = new Task<Map>() {
                @Override
                public Map call() {
                    Map map1 = new HashMap();
                    map1 = GlobalConstants.stage.hostManager.getEquipInitState(deviceInfo.getDeviceCode());
                    setData(map1, root);
                    return null;
                }
            };
            new Thread(task).start();
        }else if(commState==NOT_COMMUNICATING||isCommon==false ){
//            CommonUiUtil.alert(Alert.AlertType.WARNING, "设备不在通讯状态", stage);
            equipHost.setControlState(FengCeConstant.CONTROL_OFFLINE);
            UiLogUtil.getInstance().appendLog2SecsTab(deviceCode, "设备不在通讯状态");

        }
        getData(deviceInfoExt , deviceInfo, root, recipeService, sysService);
        sqlSession.close();
    }

    public void checkCommState(){
                EquipHost equipHost = GlobalConstants.stage.equipHosts.get(deviceCode);
                String state = equipHost.checkCommState();
                if (!"0".equals(state)) {
                    equipHost.setControlState(FengCeConstant.CONTROL_OFFLINE);
                    UiLogUtil.getInstance().appendLog2SecsTab(deviceCode, "设备不在通讯状态");
                }
    }
    public void setData(Map map,Pane root){
       if(map!=null){
//                   UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "测试 SwingWorker......");
//                    Thread.sleep(10000);
            recipeName = (TextField) root.lookup("#recipeName");
            recipeName.setText(String.valueOf(map.get("PPExecName")));
            recipeName.setTooltip(new Tooltip(String.valueOf(map.get("PPExecName"))));
            equipStatus = (TextField) root.lookup("#equipStatus");
            equipStatus.setText(String.valueOf(map.get("EquipStatus")));
        }

    }
   public void  getData(DeviceInfoExt deviceInfoExt, DeviceInfo    deviceInfo,Pane root ,RecipeService recipeService,SysService sysService){
       if (deviceInfo != null) {
           clientCode = (TextField) root.lookup("#clientCode");
           clientCode.setText(deviceInfo.getClientId());
           deviceCodeField = (TextField) root.lookup("#deviceCodeField");
           deviceCodeField.setText(deviceInfo.getDeviceCode());
//           equipStatus = (TextField) root.lookup("#equipStatus");
//           equipStatus.setText(deviceInfo.getDeviceStatus());
           deviceType = (TextField) root.lookup("#deviceType");
           deviceType.setText(deviceInfo.getDeviceType());

           lotNo = (TextField) root.lookup("#lotNo");
           recipeVersionNo = (TextField) root.lookup("#recipeVersionNo");
           JRB_EngineerMode = (RadioButton) root.lookup("#JRB_EngineerMode");
           officeName = (TextField) root.lookup("#officeName");
//            Map map = new HashMap();
           try {
//                map = (Map) statusMap.get(tempDeviceDode);

//                    CommonUtil.alert("获取设备当前状态信息失败，请检查设备状态.");
               recipeName = (TextField) root.lookup("#recipeName");
               equipStatus = (TextField) root.lookup("#equipStatus");
               recipeName.setText(panerecipeName);
               recipeName.setTooltip(new Tooltip(panerecipeName));
               equipStatus.setText(statu);
               lotNo.setText(lotId);
               if (deviceInfoExt != null) {
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

               if (deviceInfo.getOfficeId() != null && !"".equals(deviceInfo.getOfficeId())) {
                   SysOffice sysOffice = sysService.selectSysOfficeByPrimaryKey(deviceInfo.getOfficeId());
                   if (sysOffice != null) {
                       officeName.setText(sysOffice.getName());
                   }
               }
           } catch (Exception e) {
               logger.error("Exception:", e);
           }
       }

   }
}
