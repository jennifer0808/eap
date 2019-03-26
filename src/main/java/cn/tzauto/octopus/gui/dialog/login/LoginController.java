/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.gui.dialog.login;

import java.net.URL;
import java.util.ResourceBundle;


import java.io.IOException;

import cn.tzauto.octopus.gui.main.EapClient;
import cn.tzauto.octopus.gui.main.EapMainController;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import static cn.tzauto.octopus.common.globalConfig.GlobalConstants.*;


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
        // TODO
        ent.setOnKeyPressed(new EventHandler<KeyEvent>() {

            @Override
            public void handle(KeyEvent event) {
                if (event.getCode() == KeyCode.ENTER) {

                    // do something
                    Button JB_MainPage = (Button) EapClient.root.lookup("#JB_MainPage");
                    Button JB_RcpMng = (Button) EapClient.root.lookup("#JB_RcpMng");
                    Button JB_Login = (Button) EapClient.root.lookup("#JB_Login");
                    Button JB_SignOut = (Button) EapClient.root.lookup("#JB_SignOut");
                    Button localMode = (Button) EapClient.root.lookup("#localMode");
//                    ((Node) (event.getSource())).getScene().getWindow().hide();
//

                    if (userName.getText() != null && !"".equalsIgnoreCase(userName.getText())) {
                        if (userName.isFocused()) {
                            Platform.runLater(new Runnable() {
                                @Override
                                public void run() {
                                    password.requestFocus();
                                }
                            });
                        }
                    }

                    if (userName.getText() != null && !"".equalsIgnoreCase(userName.getText()) && password.getText() != null && !"".equalsIgnoreCase(password.getText())) {
                        if (password.isFocused()) {
                            Boolean isSuc = new EapMainController().loginSuc(userName, password, new Stage(), JB_MainPage,
                                    JB_RcpMng, JB_Login, JB_SignOut, localMode);

                            if (isSuc) {
                                ((Node) (event.getSource())).getScene().getWindow().hide();
                            }
                        }
                    }

//                    new EapMainController().loginSuc(userName, password, new Stage(), JB_MainPage,
//                            JB_RcpMng, JB_Login, JB_SignOut, localMode);
                }
            }

        });

        loginStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent event) {
                if (isUpload) {
                    isUpload = false;
                } else if (isDownload) {
                    isDownload = false;
                } else if (isSvQuery) {
                    isSvQuery = false;
                }

            }
        });

        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                userName.requestFocus();
            }
        });


    }

    public void init() throws IOException {

        Stage window = new Stage();
        Parent root = FXMLLoader.load(getClass().getResource("Login.fxml"));
        Scene scene = new Scene(root);
        window.setScene(scene);
        window.setResizable(false);
        window.setTitle("用户登录");
        window.show();
        Button button = (Button) root.lookup("#loginButton");

        userName = (TextField) root.lookup("#userName");
        password = (PasswordField) root.lookup("#password");


    }


}
