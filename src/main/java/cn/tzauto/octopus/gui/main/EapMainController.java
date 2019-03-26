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
import cn.tzauto.octopus.gui.dialog.svquerypane.SVQueryPaneController;
import cn.tzauto.octopus.gui.dialog.uploadpane.UploadPaneController;
import cn.tzauto.octopus.gui.guiUtil.CommonUtil;
import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;
import cn.tzauto.octopus.gui.widget.rcpmngpane.RcpMngPaneController;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
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
import org.apache.ibatis.session.SqlSession;

import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static cn.tzauto.octopus.common.globalConfig.GlobalConstants.loginStage;

/**
 * @author luosy
 */
public class EapMainController implements Initializable {

    @FXML
    private Label label;
    @FXML
    private Button loginButton;
    @FXML
    private TextField userName;
    @FXML
    private PasswordField password;
    @FXML
    private GridPane mainPane;
    @FXML
    private VBox vBox;
    @FXML
    Tab TB_RcpMng;
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

    private String DEFAULT_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private String time;
    private int ONE_SECOND = 1000;
    private String userNameStr;
    //   private Boolean Flag = false ;

    @FXML
    private GridPane functionPane;

    @FXML
    private void logout(ActionEvent event) throws IOException {
        loginOut(JB_MainPage, JB_RcpMng, JB_Login, JB_SignOut, localMode);
//        }

    }

    public void loginOut(Button JB_MainPage, Button JB_RcpMng, Button JB_Login, Button JB_SignOut, Button localMode) throws IOException {

        ResourceBundle resourceBundle = ResourceBundle.getBundle("eap", new languageUtil().getLocale());
        Parent root = FXMLLoader.load(getClass().getResource("Login.fxml"), resourceBundle);
        userName = (TextField) root.lookup("#userName");
        // userNameStr = userName.getText();
        UiLogUtil.appendLog2EventTab(null, "用户：" + userName.getText() + "注销登录...");
//        int i = CommonUtil.loginOut("是否注销？");
//
//        if (i == 0) {
        JB_MainPage.setVisible(false);
        JB_RcpMng.setVisible(false);
        JB_Login.setVisible(true);
        JB_SignOut.setVisible(false);
        if ("1".equals(GlobalConstants.getProperty("LOCALMODE_CONTROL"))) {
            localMode.setVisible(false);
        }
        GlobalConstants.userFlag = true;
        GlobalConstants.sysUser = null;

        GlobalConstants.isUpload = false;
        GlobalConstants.isDownload = false;
        GlobalConstants.isSvQuery = false;
    }

    @FXML
    private void login() throws IOException {
        GlobalConstants.userFlag = false;
        loginInterface();
    }

    public void loginInterface() throws IOException {
        loginStage = new Stage();

        ResourceBundle resourceBundle = ResourceBundle.getBundle("eap", new languageUtil().getLocale());//new Locale("zh", "TW");Locale.getDefault()

        Parent root = FXMLLoader.load(getClass().getResource("Login.fxml"), resourceBundle);
        Scene scene = new Scene(root);
        loginStage.setScene(scene);
        Image image = new Image(EapMainController.class.getResourceAsStream("logoTaiZhi.png"));
        loginStage.getIcons().add(image);
        loginStage.setResizable(false);
        loginStage.setTitle("用户登录");
        loginStage.show();
        Button button = (Button) root.lookup("#loginButton");
        userName = (TextField) root.lookup("#userName");


//        if (!GlobalConstants.userFlag) {
        userName.setText(GlobalConstants.sysUser == null ? "" : GlobalConstants.sysUser.getLoginName());

//        }
        password = (PasswordField) root.lookup("#password");

        button.setOnAction((ActionEvent t) -> {
            loginSuc(userName, password, loginStage, JB_MainPage, JB_RcpMng, JB_Login, JB_SignOut, localMode);
        });


    }

    public Boolean loginSuc(TextField userName, TextField password, Stage window, Button JB_MainPage, Button JB_RcpMng, Button JB_Login,
                            Button JB_SignOut, Button localMode) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("用户登录");
        alert.setHeaderText("注意");
        String userNameStr = userName.getText();
        String passwordStr = password.getText();
        if (userNameStr.equals("") || userNameStr == null || passwordStr.equals("") || passwordStr == null) {
//            window.close();
//
            return false;
        }
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        SysService sysService = new SysService(sqlSession);
        List<SysUser> userList = sysService.searchSysUsersByLoginName(userNameStr);
        sqlSession.close();
        if (userList == null || userList.isEmpty()) {
//            window.close();
//            CommonUtil.alert("请输入正确的用户名和密码！");
            CommonUtil.alert(Alert.AlertType.WARNING, "请输入正确的用户名和密码！");
            return false;
        }
        for (SysUser user : userList) {
            if (passwordStr.equals(user.getPassword())) {
                if (GlobalConstants.isUpload) {
                    new UploadPaneController().init();
                    GlobalConstants.isUpload = false;
                    window.close();
                    return true;
                }
                if (GlobalConstants.isDownload) {
                    TablePosition pos = (TablePosition) GlobalConstants.table.getSelectionModel().getSelectedCells().get(0);

                    int row = pos.getRow();
                    ObservableList columns = pos.getTableView().getColumns();

                    TableColumn column = (TableColumn) columns.get(2);

                    String deviceCode = column.getCellData(row).toString();

                    column = (TableColumn) columns.get(3);

                    String recipeName = column.getCellData(row).toString();

                    column = (TableColumn) columns.get(4);

                    String versionType = column.getCellData(row).toString();

                    column = (TableColumn) columns.get(5);

                    String recipeVersionNo = column.getCellData(row).toString();

                    new DownloadPaneController().init(deviceCode, recipeName, versionType, recipeVersionNo);

                    GlobalConstants.isDownload = false;
                    window.close();
                    return true;
                }




                if (GlobalConstants.isSvQuery) {
                    if (GlobalConstants.userFlag) {
                        userName.setText(GlobalConstants.sysUser == null ? "" : GlobalConstants.sysUser.getLoginName());
                        GlobalConstants.userFlag = false;
                    }
                    new SVQueryPaneController().init();
                    GlobalConstants.isSvQuery = false;

                    window.close();
                    return true;

                }

                GlobalConstants.sysUser = user;
                GlobalConstants.loginValid = true;
                GlobalConstants.loginTime = new Date();

                JB_MainPage.setVisible(true);
                JB_RcpMng.setVisible(true);
                JB_Login.setVisible(false);
                JB_SignOut.setVisible(true);
                if ("1".equals(GlobalConstants.getProperty("LOCALMODE_CONTROL"))) {
                    localMode.setVisible(true);
                }

                userNameStr = userName.getText();
                UiLogUtil.appendLog2EventTab(null, "用户：" + userNameStr + "登录系统...");
                window.close();

                break;
            } else {
//                window.close();
                CommonUtil.alert(Alert.AlertType.WARNING, "请输入正确的用户名和密码！");
                return false;
            }
        }

        return true;
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

        if ("0".equals(GlobalConstants.getProperty("LOCALMODE_CONTROL"))) {
            Platform.runLater(new Runnable() {
                public void run() {
                    functionPane.getChildren().remove(localMode);
                }
            });
        }

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

    @FXML
    private void homeExited() {
        JB_MainPage.setStyle("-fx-background-color: #0081CC;");
    }

    @FXML
    private void homeMoved() {
        JB_MainPage.setStyle("-fx-background-color: #FBC372;");
    }

    @FXML
    private void homePressed() {
        JB_MainPage.setStyle("-fx-background-color: #F9A125;");
    }

    @FXML
    private void homeReleased() {
        JB_MainPage.setStyle("-fx-background-color: #0081CC;");
    }

    @FXML
    private void recipeExited() {
        JB_RcpMng.setStyle("-fx-background-color: #0081CC;");
    }

    @FXML
    private void recipeMoved() {
        JB_RcpMng.setStyle("-fx-background-color: #FBC372;");
    }

    @FXML
    private void recipePressed() {
        JB_RcpMng.setStyle("-fx-background-color: #F9A125;");
    }

    @FXML
    private void recipeReleased() {
        JB_RcpMng.setStyle("-fx-background-color: #0081CC;");
    }

    @FXML
    private void loginExited() {
        JB_Login.setStyle("-fx-background-color: #0081CC;");
    }

    @FXML
    private void loginMoved() {
        JB_Login.setStyle("-fx-background-color: #FBC372;");
    }

    @FXML
    private void loginPressed() {
        JB_Login.setStyle("-fx-background-color: #F9A125;");
    }

    @FXML
    private void loginReleased() {
        JB_Login.setStyle("-fx-background-color: #0081CC;");
    }

    @FXML
    private void signoutExited() {
        JB_SignOut.setStyle("-fx-background-color: #0081CC;");
    }

    @FXML
    private void signoutMoved() {
        JB_SignOut.setStyle("-fx-background-color: #FBC372;");
    }

    @FXML
    private void signoutPressed() {
        JB_SignOut.setStyle("-fx-background-color: #F9A125;");
    }

    @FXML
    private void signoutReleased() {
        JB_SignOut.setStyle("-fx-background-color: #0081CC;");
    }

    @FXML
    private void localModeExited() {
        localMode.setStyle("-fx-background-color: #0081CC;");
    }

    @FXML
    private void localModeMoved() {
        localMode.setStyle("-fx-background-color: #FBC372;");
    }

    @FXML
    private void localModePressed() {
        localMode.setStyle("-fx-background-color: #F9A125;");
    }

    @FXML
    private void localModeReleased() {
        localMode.setStyle("-fx-background-color: #0081CC;");
    }


}
