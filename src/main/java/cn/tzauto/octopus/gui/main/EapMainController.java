/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.gui.main;

import cn.tzauto.octopus.biz.sys.domain.SysUser;
import cn.tzauto.octopus.biz.sys.service.SysService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.common.globalConfig.GlobalConstants;
import cn.tzauto.octopus.common.util.language.languageUtil;
import cn.tzauto.octopus.gui.dialog.download.DownloadPaneController;
import cn.tzauto.octopus.gui.dialog.login.LoginController;
import cn.tzauto.octopus.gui.dialog.uploadpane.UploadPaneController;
import cn.tzauto.octopus.gui.guiUtil.CommonUiUtil;
import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;
import cn.tzauto.octopus.gui.widget.rcpmngpane.RcpMngPaneController;
import cn.tzauto.octopus.gui.widget.svquerypane.SVQueryPaneController;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.apache.ibatis.session.SqlSession;

import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static cn.tzauto.octopus.common.globalConfig.GlobalConstants.*;
import static cn.tzauto.octopus.common.globalConfig.GlobalConstants.isSvQuery;

/**
 * @author luosy
 */
public class EapMainController implements Initializable {

    @FXML
    TabPane TBP_Main;
    @FXML
    public Button JB_MainPage;
    @FXML
    private Button JB_RcpMng;
    @FXML
    private Button JB_Login;
    @FXML
    private Button JB_SignOut;
    @FXML
    private Button localMode;
    @FXML
    private Label L_Clock;

    private TextField userName;

    private PasswordField password;

    private String DEFAULT_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private String time;
    private int ONE_SECOND = 1000;
    public static int loginmark=0;
    //   private Boolean Flag = false ;

    @FXML
    private void logout(ActionEvent event) throws IOException {
        loginOut();
    }
    //登出
    public void loginOut() throws IOException {
        Button JB_MainPage = (Button) EapClient.root.lookup("#JB_MainPage");
        Button JB_RcpMng = (Button) EapClient.root.lookup("#JB_RcpMng");
        Button JB_Login = (Button) EapClient.root.lookup("#JB_Login");
        Button JB_SignOut = (Button) EapClient.root.lookup("#JB_SignOut");
        Button localMode = (Button) EapClient.root.lookup("#localMode");
        ResourceBundle resourceBundle = ResourceBundle.getBundle("eap", new languageUtil().getLocale());
        Parent root = FXMLLoader.load(getClass().getClassLoader().getResource("Login.fxml"), resourceBundle);
        userName = (TextField) root.lookup("#userName");
       UiLogUtil.getInstance().appendLog2EventTab(null, "用户：" + userName.getText() + "注销登录...");
        JB_MainPage.setVisible(false);
        JB_RcpMng.setVisible(false);
        JB_Login.setVisible(true);
        JB_SignOut.setVisible(false);
        localMode.setVisible(false);
        GlobalConstants.sysUser = null;
        GlobalConstants.userFlag = true;
        GlobalConstants.isUpload = false;
        GlobalConstants.isDownload = false;
        GlobalConstants.isSvQuery = false;
        loginmark=0;
        rcpMngClose();
    }

    @FXML
    private void login() throws IOException {

        GlobalConstants.userFlag = false;
        if(loginmark==0){
            loginStage = new Stage();
            loginInterface();
            loginmark=1;
        }

    }

    public void loginInterface() throws IOException {
        ResourceBundle resourceBundle = ResourceBundle.getBundle("eap", new languageUtil().getLocale());//new Locale("zh", "TW");Locale.getDefault()
        Parent root = FXMLLoader.load(getClass().getClassLoader().getResource("Login.fxml"), resourceBundle);
        Scene scene = new Scene(root);
        loginStage.setScene(scene);
        Image image = new Image(EapMainController.class.getClassLoader().getResourceAsStream("logoTaiZhi.png"));
        loginStage.getIcons().add(image);
        loginStage.setResizable(false);
        loginStage.setTitle("用户登录");
        loginStage.show();
        loginStage.setAlwaysOnTop(true);
        Button button = (Button) root.lookup("#loginButton");
        userName = (TextField) root.lookup("#userName");
        userName.setText(GlobalConstants.sysUser == null ? "" : GlobalConstants.sysUser.getLoginName());
        password = (PasswordField) root.lookup("#password");
        String userNameStr = userName.getText();
        String passwordStr = password.getText();

        button.setOnAction((ActionEvent t) -> {
            new LoginController().loginSuc(userNameStr,passwordStr);
        });

        loginStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent event) {
                if (isUpload && !onlyOnePageUpload){
                    isUpload = false;
                } else if (isDownload && !onlyOnePageDownload) {
                    isDownload = false;
                } else if (isSvQuery ) {
                    isSvQuery = false;
                }
                loginmark=0;
            }
        });

        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                userName.requestFocus();
            }
        });

    }


    @FXML
    private void localModeClick(ActionEvent event) {
        if (GlobalConstants.sysUser.getLoginName().equals("admin")) {
            if (!"Local Model  ON".equals(localMode.getText())) {
                localMode.setText("Local Model  ON");
                GlobalConstants.isLocalMode = true;
            } else {
                localMode.setText("Local Model OFF");
                GlobalConstants.isLocalMode = false;
            }
        }
    }

    Tab rcpMngtTab = new Tab("Recipe管理");
    @FXML
    private void rcpMngClick() {
//        TBP_Main.getTabs().add(TB_RcpMng);
        TBP_Main = (TabPane) GlobalConstants.stage.root.lookup("#TBP_Main");

        TBP_Main.getTabs().remove(rcpMngtTab);
        try {
            StackPane rcpMngPane = new RcpMngPaneController().init();// fxmlLoader.load();
            rcpMngtTab.setContent(rcpMngPane);

        } catch (Exception ex) {
            Logger.getLogger(EapMainController.class.getName()).log(Level.SEVERE, null, ex);
        }

        TBP_Main.getTabs().add(rcpMngtTab);
        TBP_Main.getSelectionModel().select(rcpMngtTab);
    }
    @FXML
    private void rcpMngClose() {
//        TBP_Main.getTabs().add(TB_RcpMng);
        TBP_Main = (TabPane) GlobalConstants.stage.root.lookup("#TBP_Main");
        TBP_Main.getTabs().remove(rcpMngtTab);
    }
    /**
     * Timer task to update the time display area
     */

    public class LabelTimerTask extends TimerTask {
        SimpleDateFormat dateFormatter = new SimpleDateFormat(DEFAULT_TIME_FORMAT);

        @Override
        public void run() {
            Platform.runLater(() -> {
                time = dateFormatter.format(Calendar.getInstance().getTime());
                L_Clock.setText(time);
            });

        }
    }


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        //定时器
        Timer tmr = new Timer();
        tmr.scheduleAtFixedRate(new LabelTimerTask(), new Date(), ONE_SECOND);

        //按钮图标
        Image ImageMain = new Image("mainPicture.png");
        Image ImageRcp = new Image("RecipeMngPicture.png");
        Image ImageLogin = new Image("LoginPic.png");
        Image ImageLogOut = new Image("LogoutPic.png");

        ImageView imageViewMain = new ImageView(ImageMain);
        ImageView imageViewRcp = new ImageView(ImageRcp);
        ImageView imageViewLogin = new ImageView(ImageLogin);
        ImageView imageViewLogout = new ImageView(ImageLogOut);

        //给按钮设置图标
        JB_MainPage.setGraphic(imageViewMain);
        JB_RcpMng.setGraphic(imageViewRcp);
        JB_Login.setGraphic(imageViewLogin);
        JB_SignOut.setGraphic(imageViewLogout);

    }


    @FXML
    private void mainPaneClick() {
        TBP_Main.getSelectionModel().select(GlobalConstants.stage.mainTab);
    }

}
