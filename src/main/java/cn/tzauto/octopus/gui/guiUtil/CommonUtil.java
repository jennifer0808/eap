package cn.tzauto.octopus.gui.guiUtil;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import javax.swing.*;
import java.util.Optional;

import static javafx.scene.control.Alert.AlertType.INFORMATION;

/**
 * Created by zm730 on 2019/2/13.
 */
public class CommonUtil {

    public static Optional<ButtonType> alert(Alert.AlertType type, String message) {
        Alert alert = new Alert(type);

        switch (type) {
            case INFORMATION:
                alert.setTitle("Information Dialog");
                break;
            case WARNING:
                alert.setTitle("Warning Dialog");
                break;
            case CONFIRMATION:
                alert.setTitle("Confirmation Dialog");
                break;
        }

        alert.setHeaderText(null);

        alert.setContentText(message);

        return alert.showAndWait();


    }


}
