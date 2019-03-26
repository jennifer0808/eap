package cn.tzauto.octopus.gui.dialog.svquerypane;

import cn.tzauto.octopus.common.util.language.languageUtil;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * Created by wj_co on 2019/2/12.
 */
public class SVQueryPaneController implements Initializable {


    @Override
    public void initialize(URL location, ResourceBundle resources) {
    //设置设备编号


    }
    @FXML
    private void Btn_Query(){

    }


    private void buttonOKClick(Stage stage) {
        stage.close();
    }

    public void init() {
        // TODO String : deviceId, String : deviceCode
        Stage stage = new Stage();
        Image image = new Image(getClass().getClassLoader().getResourceAsStream("logoTaiZhi.png"));
        stage.getIcons().add(image);
        stage.setTitle("SV数据查询");
        Pane root = new Pane();
        try {
            ResourceBundle resourceBundle =ResourceBundle.getBundle("eap",new languageUtil().getLocale());
            root = FXMLLoader.load(getClass().getClassLoader().getResource("SVQueryPane.fxml"),resourceBundle);
        } catch (IOException ex) {

        }

        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.show();

        stage.setResizable(false);
        Button button = (Button) root.lookup("#button");
        button.setOnAction((value) -> buttonOKClick(stage));
    }



}
