/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.gui.dialog.login;

import cn.tzauto.octopus.biz.sys.domain.SysUser;
import cn.tzauto.octopus.biz.sys.service.SysService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.common.globalConfig.GlobalConstants;
import cn.tzauto.octopus.gui.dialog.download.DownloadPaneController;
import cn.tzauto.octopus.gui.dialog.uploadpane.UploadPaneController;
import cn.tzauto.octopus.gui.guiUtil.CommonUiUtil;
import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;
import cn.tzauto.octopus.gui.main.EapClient;
import cn.tzauto.octopus.gui.widget.rcpmngpane.SimpleRecipeProperty;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import org.apache.ibatis.session.SqlSession;

import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;

import static cn.tzauto.octopus.common.globalConfig.GlobalConstants.loginStage;
import static cn.tzauto.octopus.gui.widget.rcpmngpane.RcpMngPaneController.list;

/**
 * FXML Controller class
 *
 * @author luosy
 */
public class LoginController implements Initializable {
    @FXML
    private TextField userName;
    @FXML
    private PasswordField password;
    private boolean loginFlag = false;
    @FXML
    private Pane ent;

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // 键盘响应事件
        ent.setOnKeyPressed(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent event) {
                if (event.getCode() == KeyCode.ENTER) {
                    String userNameStr = userName.getText();
                    String passwordStr = password.getText();
                    if (userNameStr != null && !"".equalsIgnoreCase(userNameStr)) {
                        if (userName.isFocused()) {
                            Platform.runLater(new Runnable() {
                                @Override
                                public void run() {
                                    password.requestFocus();
                                }
                            });
                        }
                    }

                    if (passwordStr != null && !"".equalsIgnoreCase(passwordStr)) {
                        if (password.isFocused()) {
                            //登录验证
                            Boolean isSuc = loginSuc(userNameStr, passwordStr);
                            if (isSuc) {
                                ((Node) (event.getSource())).getScene().getWindow().hide();
                            }
                        }
                    }
                }
            }
        });
    }

    //登录验证
    public Boolean loginSuc(String userNameStr, String passwordStr) {
        Button JB_MainPage = (Button) EapClient.root.lookup("#JB_MainPage");
        Button JB_RcpMng = (Button) EapClient.root.lookup("#JB_RcpMng");
        Button JB_Login = (Button) EapClient.root.lookup("#JB_Login");
        Button JB_SignOut = (Button) EapClient.root.lookup("#JB_SignOut");
        Button localMode = (Button) EapClient.root.lookup("#localMode");
        if (userNameStr.equals("") || userNameStr == null || passwordStr.equals("") || passwordStr == null) {
            return false;
        }
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        SysService sysService = new SysService(sqlSession);
        List<SysUser> userList = sysService.searchSysUsersByLoginNamePassword(userNameStr, passwordStr);
        sqlSession.close();
        if (userList.size() > 0 && userList != null) {
            if (GlobalConstants.isUpload) {
                new UploadPaneController().init();
                GlobalConstants.onlyOnePageUpload = true;
                loginStage.close();
                return true;
            }
            if (GlobalConstants.isDownload) {
                for (int i = 0; i < list.size(); i++) {
                    SimpleRecipeProperty srp = list.get(i);
                    if (srp.getDelCheckBox().isSelected()) {
                        String deviceCode = srp.getDeviceCode().getValue();
                        String recipeName = srp.getRecipeName().getValue();
                        String versionType = srp.getVersionType().getValue();
                        String recipeVersionNo = srp.getVersionNo().getValue();
                        new DownloadPaneController().init(deviceCode, recipeName, versionType, recipeVersionNo);
                    }
                }


//                    TablePosition pos = (TablePosition) GlobalConstants.table.getSelectionModel().getSelectedCells().get(0);
//
//                    int row = pos.getRow();
//                    ObservableList columns = pos.getTableView().getColumns();
//
//                    TableColumn column = (TableColumn) columns.get(2);
//
//                    String deviceCode = column.getCellData(row).toString();
//
//                    column = (TableColumn) columns.get(3);
//
//                    String recipeName = column.getCellData(row).toString();
//
//                    column = (TableColumn) columns.get(4);
//
//                    String versionType = column.getCellData(row).toString();
//
//                    column = (TableColumn) columns.get(5);
//
//                    String recipeVersionNo = column.getCellData(row).toString();


                GlobalConstants.isDownload = false;
                loginStage.close();
                return true;
            }
            //todo sv查询的用户验证
//                if (GlobalConstants.isSvQuery) {
//                    if (GlobalConstants.userFlag) {
//                        userName.setText(GlobalConstants.sysUser == null ? "" : GlobalConstants.sysUser.getLoginName());
//                        GlobalConstants.userFlag = false;
//                    }
//                    new SVQueryPaneController().init("");
//                    GlobalConstants.isSvQuery = false;
//                    loginStage.close();
//                    return true;
//                }

//********************************************************************************************
//            for (SysUser user : userList) {
//                String dbPasswords = DigestUtil.passwordDeEncrypt(user.getPassword());
//                System.out.println("==========================="+dbPasswords);
//                if (dbPasswords.equals(passwordStr)) {
//                    GlobalConstants.sysUser = user;
//
//                    JB_MainPage.setVisible(true);
//                    JB_RcpMng.setVisible(true);
//                    JB_Login.setVisible(false);
//                    JB_SignOut.setVisible(true);
//                    localMode.setVisible(true);
//                    loginStage.close();
//                    UiLogUtil.getInstance().appendLog2EventTab(null, "用户：" + userNameStr + "登录系统...");
//
//                 //todo   eapgv.setPartsVisible();
//                    if (loginFlag) {
//                        GlobalConstants.loginValid = true;
//                    }
//                    GlobalConstants.loginTime = new Date();
//                    break;
//                } else {
//                    CommonUiUtil.alert(Alert.AlertType.WARNING, "用户名与密码不匹配！");
//                }
//            }


 //********************************************************************************************
            GlobalConstants.sysUser = userList.get(0);
            GlobalConstants.loginValid = true;
            GlobalConstants.loginTime = new Date();

            JB_MainPage.setVisible(true);
            JB_RcpMng.setVisible(true);
            JB_Login.setVisible(false);
            JB_SignOut.setVisible(true);
         //todo   localMode.setVisible(false);

            UiLogUtil.getInstance().appendLog2EventTab(null, "用户：" + userNameStr + "登录系统...");
            loginStage.close();

        } else {
            CommonUiUtil.alert(Alert.AlertType.WARNING, "请输入正确的用户名和密码！");
            return false;
        }

        return true;
    }

    //登录按钮鼠标点击事件
    @FXML
    private void loginAction() {
        String userNameStr = userName.getText();
        String passwordStr = password.getText();
        if (passwordStr != null && !"".equalsIgnoreCase(passwordStr) && userNameStr != null && !"".equalsIgnoreCase(userNameStr)) {
            //登录验证
            Boolean isSuc = loginSuc(userNameStr, passwordStr);
            if (isSuc) {
                loginStage.close();
            }
        }
    }

}
