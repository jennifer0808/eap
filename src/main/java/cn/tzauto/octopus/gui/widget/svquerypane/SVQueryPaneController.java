package cn.tzauto.octopus.gui.widget.svquerypane;

import cn.tzauto.octopus.common.util.language.languageUtil;
import cn.tzauto.octopus.gui.guiUtil.CommonUiUtil;
import cn.tzauto.octopus.gui.main.EapClient;
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

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;


/**
 * Created by wj_co
 */
public class SVQueryPaneController implements Initializable {
    private final String deviceCode;

    @FXML
    private TextField deviceCodeField;

    public   Stage stage= new Stage();
    public static Map<String,Boolean>  flag = new HashMap<>();

    public SVQueryPaneController() {
        this.deviceCode = null;
        stage.setTitle("SV数据查询");
        stage.setAlwaysOnTop(true);
        stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent event) {
                flag.put(deviceCode,false) ;
            }
        });
    }
    public SVQueryPaneController(String deviceCode) {

        this.deviceCode = deviceCode;
        stage.setTitle("SV数据查询");
        stage.setAlwaysOnTop(true);
        stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent event) {
                flag.put(deviceCode,false) ;
            }
        });
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        //设置设备编号


    }

    private String svid;


    private void Btn_Query(Pane root,Stage stage) {
        try{

            TextField JTF_SVID = (TextField) root.lookup("#JTF_SVID");
            TextField JTF_SVValue = (TextField) root.lookup("#JTF_SVValue");
            RadioButton JRB_EC = (RadioButton) root.lookup("#JRB_EC");

            svid = JTF_SVID.getText();
            if (svid == null || !svid.matches("\\d+")) {
                CommonUiUtil.alert(Alert.AlertType.ERROR, "请输入正确格式的SVID！",stage);
                return;
            }
            Map resultMap = new HashMap();
            if (JRB_EC.isSelected()) {
                resultMap = EapClient.hostManager.getECValueByECID(deviceCode, svid);
            } else {
                resultMap = EapClient.hostManager.getSVValueBySVID(deviceCode, svid);
            }
            if (resultMap == null || resultMap.size()==0 ) {
                CommonUiUtil.alert(Alert.AlertType.WARNING, "未查到相应值！",stage);
                return;
            }
            String SVValue = String.valueOf(resultMap.get("Value"));
            if (SVValue == null || "".equals(SVValue) || "null".equals(SVValue)) {
                CommonUiUtil.alert(Alert.AlertType.WARNING, "未查到相应值！",stage);
                return;
            } else {
                JTF_SVValue.setText(SVValue);
                JTF_SVValue.setTooltip(new Tooltip(SVValue));
            }
        }catch (Exception e){
            CommonUiUtil.alert(Alert.AlertType.WARNING, e.toString(),stage);
        }
    }


    private void buttonOKClick(Stage stage) {
        stage.close();
        flag.put(deviceCode,false) ;
    }

    Pane root = new Pane();

    public void init() {
        flag.put(deviceCode,true) ;
        // TODO String : deviceId, String : deviceCode
        Image image = new Image(getClass().getClassLoader().getResourceAsStream("logoTaiZhi.png"));
        stage.getIcons().add(image);

        try {
            ResourceBundle resourceBundle = ResourceBundle.getBundle("eap", new languageUtil().getLocale());
            root = FXMLLoader.load(getClass().getClassLoader().getResource("SVQueryPane.fxml"),resourceBundle);
        } catch (IOException ex) {

        }


        deviceCodeField = (TextField) root.lookup("#deviceCodeField");
        deviceCodeField.setText(deviceCode);

        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.show();

        stage.setResizable(false);
        Button button = (Button) root.lookup("#button");
        Button svQuery = (Button) root.lookup("#svQuery");
        button.setOnAction((value) -> buttonOKClick(stage));

        svQuery.setOnAction((value) -> Btn_Query(root,stage));
    }


}
