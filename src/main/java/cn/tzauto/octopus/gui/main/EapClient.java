/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.gui.main;

import cn.tzauto.octopus.biz.device.domain.DeviceInfo;
import cn.tzauto.octopus.common.globalConfig.GlobalConstants;
import cn.tzauto.octopus.common.mq.SubscribeMessage;
import cn.tzauto.octopus.common.mq.common.MQConstants;
import cn.tzauto.octopus.common.util.language.languageUtil;
import cn.tzauto.octopus.common.util.tool.CommonUtil;
import cn.tzauto.octopus.common.ws.InitService;
import cn.tzauto.octopus.gui.EquipmentEventDealer;
import cn.tzauto.octopus.gui.widget.equipstatuspane.EquipStatusPane;
import cn.tzauto.octopus.isecsLayer.domain.EquipModel;
import cn.tzauto.octopus.isecsLayer.socket.EquipStatusListen;
import cn.tzauto.octopus.secsLayer.domain.EquipHost;
import cn.tzauto.octopus.secsLayer.domain.EquipNodeBean;
import cn.tzauto.octopus.secsLayer.domain.EquipPanel;
import cn.tzauto.octopus.secsLayer.domain.MultipleEquipHostManager;
import cn.tzauto.octopus.secsLayer.util.FengCeConstant;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCharacterCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;
import javax.swing.SwingWorker;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobListener;
import org.quartz.SchedulerException;

/**
 * @author luosy
 */
public class EapClient extends Application implements JobListener, PropertyChangeListener {

    @FXML
    private StackPane mainPane;
    @FXML
    private ScrollPane mainScrollPane;
    @FXML
    private Label label = new Label();
    @FXML
    private ImageView equipImg;
    private static final Logger logger = Logger.getLogger(EapClient.class.getName());
    public static MultipleEquipHostManager hostManager;
    public static ArrayList<EquipNodeBean> equipBeans;
    private String clientId;
    public List<DeviceInfo> deviceInfos;

    public HashMap<String, EquipHost> equipHosts;
    public ConcurrentHashMap<String, EquipModel> equipModels;
    public static ConcurrentHashMap<String, EquipStatusPane> equipStatusPanes = new ConcurrentHashMap<>();

    public static Parent root;
    public Tab mainTab;

    public static ServerSocket server;
    public static HashMap<String, EquipmentEventDealer> watchDogs;

    @Override
    public void start(Stage stage) throws Exception {
//        server = Server.oneServer();

        stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent event) {
                System.exit(0);
            }
        });
        System.out.println(111);
        ProgressIndicator progressIndicator = new ProgressIndicator();

        Stage window = new Stage();

        stage.setMinWidth(1024);
        stage.setMinHeight(600);
        Image image = new Image(getClass().getClassLoader().getResourceAsStream("logoTaiZhi.png"));
        stage.getIcons().add(image);

        window.initStyle(StageStyle.UNDECORATED);
        window.setResizable(false);
        Label loadinglabel = new Label();
        loadinglabel.setStyle("-fx-font-size: 25");
        loadinglabel.setCenterShape(true);
        loadinglabel.setText("Loading...");
        VBox vb = new VBox();
        vb.setAlignment(Pos.CENTER);
        vb.getChildren().addAll(progressIndicator, loadinglabel);
        vb.setStyle("-fx-border-color: lightblue");
        vb.setSpacing(10);
        Scene loadingScene = new Scene(vb, 500, 300);
//        loadingScene.getStylesheets().add(getClass().getResource("/cn/tzinfo/htauto/octopus/gui/main/main.css").toExternalForm());
        window.setScene(loadingScene);

        window.show();

        Platform.runLater(() -> {
            try {
                ResourceBundle resourceBundle = ResourceBundle.getBundle("eap", new languageUtil().getLocale());//new Locale("zh", "TW");Locale.getDefault()
                FXMLLoader fXMLLoader = new FXMLLoader();
                root = FXMLLoader.load(getClass().getClassLoader().getResource("Main.fxml"), resourceBundle);
//                resourceBundle.getString("ClientId");
//                resourceBundle.getString("login");
//                resourceBundle.getString("mainPage");
//                resourceBundle.getString("logout");
//                resourceBundle.getString("name");
                stage.setTitle("EAPClient JFXML");
                Scene scene = new Scene(root);
                stage.setScene(scene);

                // String clientId = GlobalConstants.getProperty("clientId");
//                stage.setFullScreen(true);
                stage.setFullScreenExitHint("");
                KeyCharacterCombination keyCombination = new KeyCharacterCombination("Z", KeyCombination.CONTROL_DOWN);
                stage.setFullScreenExitKeyCombination(keyCombination);
                stage.show();
                logger.info("开始启动...");
                logger.info("从本地数据库读取基础配置数据...");

                if ("1".equals(GlobalConstants.getProperty("INIT_SERVICE"))) {
                    InitService.init(GlobalConstants.getProperty("clientId"));//同步服务端数据
                }

                if (!GlobalConstants.loadPropertyFromDB()) {
                    logger.info("无法从本地数据库获取正确数据，无法运行，退出...");
                    return;
                }
                if (!GlobalConstants.initData()) {
                    logger.info("数据不正确，无法运行，退出...");
                    return;
                }
                //查询数据库元素，显示设备信息
                try {
                    hostManager = new MultipleEquipHostManager();

                    if (hostManager.initialize()) {
                        logger.info("加载工控下配置设备成功...");
                    } else {
                        logger.error("加载工控下配置设备失败...");
                        return;
                    }

                    equipHosts = hostManager.getAllEquipHosts();
                    equipModels = hostManager.getAllEquipModels();
                } catch (Exception e) {
                    logger.error("Exception:", e);
                    System.exit(0);
                }

                //显示界面
                //渲染设备信息界面
                this.initializeEquipNodeBeansAndAddListen();

                //渲染设备状态信息
                this.initializeEquipStatusAndRender();
                //启动Secs通信
//                this.startHost();
//                //启动ISecs通信
//                EquipStatusListen.startListen();

                //启动ISecs通信
                this.startModel();

                GlobalConstants.stage = this;

//                new Thread(){
//                    public void run(){
//                        BaseWebservicePublish baseWebservicePublish=new BaseWebservicePublish();
//                        baseWebservicePublish.publish();
//                    }
//                }.start();
                //自动注销功能
                try {
                    new MainScheduler().schedulerJob();
                } catch (SchedulerException e) {
                    e.printStackTrace();
                }

                //Quartz监控
                if ("1".equals(GlobalConstants.getProperty("MONITOR_PARA"))) {
                    CommonUtil.startMonitorJob(this);
                }
                if ("1".equals(GlobalConstants.getProperty("ISECS_MONITOR_ALARM"))) {
                    CommonUtil.startMonitorAlarmJob(this);
                }
                if ("1".equals(GlobalConstants.getProperty("ISECS_MONITOR_EQUIPSTATUS"))) {
                    CommonUtil.startRefreshEquipStateJob(this);
                }
                if ("1".equals(GlobalConstants.getProperty("NETCHECK"))) {
                    CommonUtil.startNetCheckJob(this);
                }
                if ("1".equals(GlobalConstants.getProperty("COMMCHECK"))) {
                    CommonUtil.startCommuCheckJob(this);
                }
                if ("1".equals(GlobalConstants.getProperty("DATACLEAN"))) {
                    CommonUtil.startCleanDataJob(this);
                }

                //netty监控
                if ("1".equals(GlobalConstants.getProperty("ISECS_EQUIPSTATUS_LISTEN"))) {
                    EquipStatusListen.startListen();
                }
                if ("1".equals(GlobalConstants.getProperty("ISECS_EQUIPALARM_LISTEN"))) {
                    EquipStatusListen.startAlarmListen();
                }

                //开启MQ监听
                if (!GlobalConstants.isLocalMode) {
                    MQConstants.initConenction();
                    new SubscribeMessage().startlistening();
                    //发送开机日志给服务端
                    GlobalConstants.sendStartLog2Server(null);
                }

            } catch (IOException ex) {
                ex.printStackTrace();
            }
            window.close();
        });

//        ap.getChildren().add(sp);
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {

        launch(args);

    }

    public void initializeEquipNodeBeansAndAddListen() {
        //工控编号
        clientId = GlobalConstants.getProperty("clientId");
        Label EngineCode = (Label) root.lookup("#EngineCode");
        EngineCode.setText(clientId);

        equipBeans = hostManager.initEquipNodeBeans();
        Collections.sort(equipBeans,
                new Comparator<EquipNodeBean>() {
            @Override
            public int compare(EquipNodeBean s1, EquipNodeBean s2) {
                return s1.getEquipName().compareToIgnoreCase(s2.getEquipName());
            }
        });
        for (EquipNodeBean value : equipBeans) {
            if (value == null) {
                continue;
            }
            value.addPropertyChangeListener(this);
        }
    }

    public void startHostSwing() {

        SwingWorker worker = new SwingWorker<Void, Void>() {

            @Override
            public Void doInBackground() {
                for (int i = 0; i < equipBeans.size(); i++) {
                    String protocol = equipBeans.get(i).getProtocolTypeProperty();
                    if (!"ISECS".equals(protocol)) {
                        MDC.put(FengCeConstant.WHICH_EQUIPHOST_CONTEXT, equipBeans.get(i).getEquipName());
                        startComByEqp(equipBeans.get(i));
                    }
                }
                return null;
            }
        };
        worker.execute();
    }

    public void startHost() {

        new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < equipBeans.size(); i++) {
                    MDC.put(FengCeConstant.WHICH_EQUIPHOST_CONTEXT, equipBeans.get(i).getEquipName());
                    startComByEqp(equipBeans.get(i));
                }
            }
        }).start();

    }

    public void startModel() {

//        new Thread(new Runnable() {
//            @Override
//            public void run() {
        for (int i = 0; i < equipBeans.size(); i++) {
            MDC.put(FengCeConstant.WHICH_EQUIPHOST_CONTEXT, equipBeans.get(i).getEquipName());
            startIsecsByEqp(equipBeans.get(i));
        }
//            }
//        }).start();
    }

    public void initializeEquipStatusAndRender() throws IOException {
        ScrollPane scrollPane = (ScrollPane) root.lookup("#scPane");
        FlowPane flowPane1 = new FlowPane();
        flowPane1.setStyle("-fx-background-color: white");
        flowPane1.setPadding(new Insets(5));
        flowPane1.setVgap(5);
        flowPane1.setHgap(5);
        for (EquipNodeBean value : equipBeans) {
            if (value == null) {
                continue;
            }
            Pane equipPane = FXMLLoader.load(getClass().getClassLoader().getResource("EquipStatusPane.fxml"));
            EquipStatusPane equipStatusPane = new EquipStatusPane(value, equipPane);

            //"/cn/tzinfo/htauto/octopus/gui/main/resources/Fico-AMS-W.jpg"n
//            equip.setDeviceCode(value.getEquipName());
            equipStatusPane.setDeviceCodeAndDeviceType(value.getEquipName());
            if (value.getEquipStateProperty().isCommOn()) {
                equipStatusPane.setCommLabelForegroundColorCommOn();
            } else {
                equipStatusPane.setCommLabelForegroundColorCommOff();
            }
            equipStatusPane.setRunStatus(value.getEquipStateProperty().getEventString());
            equipStatusPane.setRunningRcp(value.getEquipStateProperty().getRunningRcp());
            equipStatusPanes.put(value.getEquipName(), equipStatusPane);
            flowPane1.getChildren().add(equipStatusPane.equipStatusPane);
        }
        scrollPane.setContent(flowPane1);
        ((TabPane) root.lookup("#TBP_Main")).getTabs().get(0).setClosable(false);
        mainTab = ((TabPane) root.lookup("#TBP_Main")).getTabs().get(0);
        logger.info("Initiaize Host status panel completed!");

    }

    @Override
    public String getName() {
        logger.debug(GlobalConstants.SYNC_CONFIG_JOB_NAME + "：获取job名称...");
        return GlobalConstants.SYNC_CONFIG_JOB_NAME;
    }

    @Override
    public void jobToBeExecuted(JobExecutionContext jec) {
        logger.debug(GlobalConstants.SYNC_CONFIG_JOB_NAME + "：执行前...");
    }

    @Override
    public void jobExecutionVetoed(JobExecutionContext jec) {
        logger.debug(GlobalConstants.SYNC_CONFIG_JOB_NAME + "：运行中...");
    }

    @Override
    public void jobWasExecuted(JobExecutionContext jec, JobExecutionException jee) {
        logger.debug(GlobalConstants.SYNC_CONFIG_JOB_NAME + "：执行后...");
    }

    public void startComByEqp(EquipNodeBean equipNodeBean) {
        EquipmentEventDealer eqpEventDealer = new EquipmentEventDealer(equipNodeBean, this);
        String deviceId = equipNodeBean.getDeviceIdProperty();

        try {
            hostManager.startHostThread(deviceId);
            hostManager.startSECS(deviceId, eqpEventDealer, eqpEventDealer, eqpEventDealer);
        } catch (Exception e1) {
            logger.fatal(equipNodeBean.getEquipName() + " has not been initialized!", e1);
        }

    }

    /**
     * 启动线程初始化nosecs设备界面
     *
     * @param equipNodeBean
     */
    public void startIsecsByEqp(EquipNodeBean equipNodeBean) {
        try {
            EquipModel equipModel = equipModels.get(equipNodeBean.getDeviceIdProperty());
            if (equipModel.iSecsHost.iSecsConnection.getSocketClient() != null && !equipModel.isInterrupted()) {
//                equipModel.getEquipRealTimeState();
                equipModel.start();
            }
        } catch (Exception e) {
            logger.fatal(equipNodeBean.getDeviceIdProperty() + "initialization failed...", e);
        }

    }

    public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
        String property = propertyChangeEvent.getPropertyName();
        Object source = propertyChangeEvent.getSource();
        Object newValue = propertyChangeEvent.getNewValue();
        Object oldValue = propertyChangeEvent.getOldValue();
        if (source instanceof EquipNodeBean) {
            EquipNodeBean src = (EquipNodeBean) source;
            String deviceCode = src.getEquipName();
            if (property.equalsIgnoreCase(EquipNodeBean.EQUIP_PANEL_PROPERTY)) {
                EquipPanel newPanel = (EquipPanel) newValue;
                EquipStatusPane equipStatusPane = this.equipStatusPanes.get(deviceCode);
                equipStatusPane.setRunStatus(newPanel.getRunState());
                equipStatusPane.setLotId(newPanel.getWorkLot());
                equipStatusPane.setAlarmState(newPanel.getAlarmState());
                if ("".equals(newPanel.getRunningRcp())) {
                    equipStatusPane.setRunningRcp("--");
                } else {
                    equipStatusPane.setRunningRcp(newPanel.getRunningRcp());
                }
                if (newPanel.getNetState() == 1) {
                    equipStatusPane.setCommLabelForegroundColorCommOn();
                    if (newPanel.getControlState().equals(FengCeConstant.CONTROL_OFFLINE)) {
                        equipStatusPane.setControlState(FengCeConstant.CONTROL_OFFLINE);
                        equipStatusPane.setCommLabelForegroundColorCommOff();
                        logger.info(deviceCode + "getControlState---------------------off-line");
                    } else {
                        if (newPanel.getAlarmState() == 0) {
                            switch (newPanel.getControlState()) {
                                case FengCeConstant.CONTROL_LOCAL_ONLINE:
                                    equipStatusPane.setControlState(FengCeConstant.CONTROL_LOCAL_ONLINE);
                                    break;
                                case FengCeConstant.CONTROL_REMOTE_ONLINE:
                                    equipStatusPane.setControlState(FengCeConstant.CONTROL_REMOTE_ONLINE);
                                    break;
                                case FengCeConstant.CONTROL_OFFLINE:
                                    equipStatusPane.setControlState(FengCeConstant.CONTROL_OFFLINE);
                                    equipStatusPane.setCommLabelForegroundColorCommOff();
                                    break;
                            }
                        } else if (newPanel.getAlarmState() == 1 || newPanel.getAlarmState() == 2) {
                            equipStatusPane.setAlarmState(newPanel.getAlarmState());
                            switch (newPanel.getControlState()) {
                                case FengCeConstant.CONTROL_LOCAL_ONLINE:
                                    equipStatusPane.setControlStateSpecial(FengCeConstant.CONTROL_LOCAL_ONLINE);
                                    break;
                                case FengCeConstant.CONTROL_REMOTE_ONLINE:
                                    equipStatusPane.setControlStateSpecial(FengCeConstant.CONTROL_REMOTE_ONLINE);
                                    break;
                            }
                        }
                    }
                } else {
                    equipStatusPane.setCommLabelForegroundColorCommOff();
                    logger.error(deviceCode + "---------------------comm-off");
                }
//                equipStatusPane.updateUI();
            }
            if (property.equalsIgnoreCase(EquipNodeBean.EQUIP_STATE_PROPERTY)) {
                if (src.getEquipStateProperty().isCommOn()) {
                    logger.info("CommOn==========================");
                    EquipHost equipHost = equipHosts.get(src.getDeviceIdProperty());
                    Map map = new HashMap();
                    map.put("NetState", 1);
                    equipHost.changeEquipPanel(map);
//                  thePanel.setCommLabelForegroundColorCommOn();
                } else {
                    logger.info("CommOff==========================");
                    EquipHost equipHost = equipHosts.get(src.getDeviceIdProperty());
                    Map map = new HashMap();
                    map.put("NetState", 0);
                    equipHost.changeEquipPanel(map);
                    //监听到通信失败事件，重新启动线程通信
                    startComByEqp(src);
                }
            }
        }
    }

    public MultipleEquipHostManager getMultipleEquipHostManager() {
        return this.hostManager;
    }

    public DeviceInfo getDeviceInfo(String deviceId, String deviceCode) {
        if (deviceId != null) {
            for (DeviceInfo deviceInfo : deviceInfos) {
                if (deviceId.equals(deviceInfo.getDeviceId())) {
                    return deviceInfo;
                }
            }
        }
        if (deviceCode != null) {
            for (DeviceInfo deviceInfo : deviceInfos) {
                if (deviceCode.equals(deviceInfo.getDeviceCode())) {
                    return deviceInfo;
                }
            }
        }
        return null;
    }

    public TextArea getTX_EventLog() {
        return (TextArea) root.lookup("#TX_EventLog");
    }

    public TextArea getTX_SecsLog() {
        return (TextArea) root.lookup("#TX_SecsLog");
    }

    public TextArea getTX_ServerLog() {
        return (TextArea) root.lookup("#TX_ServerLog");
    }

    public static void addWatchDog(String deviceId, EquipmentEventDealer watchDog) {
        EapClient.watchDogs.put(deviceId, watchDog);
    }

    public static EquipmentEventDealer removeWatchDog(String deviceId) {
        return EapClient.watchDogs.remove(deviceId);
    }

    public static EquipmentEventDealer getWatchDog(String deviceId) {
        return EapClient.watchDogs.get(deviceId);
    }

    public EquipStatusPane getThePane(String deviceCode) {
        EquipStatusPane thePane = null;
        ObservableList<Node> nodes = ((StackPane) this.root.lookup("#mainScrollPane")).getChildrenUnmodifiable();
        for (Node node : nodes) {
            if (node instanceof Pane) {
//                if (node.lookup("#L_DeviceCode") != null) {
//                    Label deviceCodeLabel = (Label) node.lookup("#L_DeviceCode");
//                    String deviceCodeTmp = deviceCodeLabel.getText();
//                    if (deviceCodeTmp.equals(deviceCode)) {
                return EapClient.equipStatusPanes.get(deviceCode);
//                    }
//                }
            }
        }
        return thePane;
    }
}
