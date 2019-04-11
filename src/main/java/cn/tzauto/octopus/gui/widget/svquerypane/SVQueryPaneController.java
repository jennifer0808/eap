package cn.tzauto.octopus.gui.widget.svquerypane;

import cn.tzauto.octopus.gui.main.EapClient;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

import javax.swing.*;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;


/**
 * Created by wj_co on 2019/2/12.
 */
public class SVQueryPaneController implements Initializable {
    private String deviceCode;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        //设置设备编号


    }

    private String svid;


    private void Btn_Query(Pane root) {
        TextField JTF_SVID = (TextField) root.lookup("#JTF_SVID");
        TextField JTF_SVValue = (TextField) root.lookup("#JTF_SVValue");
        RadioButton JRB_EC = (RadioButton) root.lookup("#JRB_EC");

        svid = JTF_SVID.getText();
        if (svid == null || !svid.matches("\\d+")) {
            JOptionPane.showMessageDialog(null, "请输入正确格式的SVID");
            return;
        }
        Map resultMap = new HashMap();
        if (JRB_EC.isSelected()) {
            resultMap = EapClient.hostManager.getECValueByECID(deviceCode, svid);
        } else {
            resultMap = EapClient.hostManager.getSVValueBySVID(deviceCode, svid);
        }
        if (resultMap == null) {
            JOptionPane.showMessageDialog(null, "未查到相应值");
            return;
        }
        String SVValue = String.valueOf(resultMap.get("Value"));
        if (SVValue == null || "".equals(SVValue) || "null".equals(SVValue)) {
            JOptionPane.showMessageDialog(null, "未查到相应值");
            return;
        } else {
            JTF_SVValue.setText(SVValue);
            JTF_SVValue.setTooltip(new Tooltip(SVValue));
        }
    }


    private void buttonOKClick(Stage stage) {
        stage.close();
    }

    Pane root = new Pane();

    public void init(String deviceCode) {
        // TODO String : deviceId, String : deviceCode
        this.deviceCode = deviceCode;
        Stage stage = new Stage();
        Image image = new Image(getClass().getClassLoader().getResourceAsStream("logoTaiZhi.png"));
        stage.getIcons().add(image);
        stage.setTitle("SV数据查询");

        try {
            root = FXMLLoader.load(getClass().getClassLoader().getResource("SVQueryPane.fxml"));
        } catch (IOException ex) {

        }


        Label deviceCodeField = (Label) root.lookup("#deviceCodeField");
        deviceCodeField.setText(deviceCode);

        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.show();

        stage.setResizable(false);
        Button button = (Button) root.lookup("#button");
        Button svQuery = (Button) root.lookup("#svQuery");
        button.setOnAction((value) -> buttonOKClick(stage));

        svQuery.setOnAction((value) -> Btn_Query(root));
    }


}
