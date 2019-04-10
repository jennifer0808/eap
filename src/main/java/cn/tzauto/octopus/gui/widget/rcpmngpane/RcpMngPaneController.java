/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.gui.widget.rcpmngpane;


import cn.tzauto.octopus.biz.recipe.domain.Recipe;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.common.globalConfig.GlobalConstants;
import cn.tzauto.octopus.common.util.language.languageUtil;
import cn.tzauto.octopus.gui.guiUtil.CommonUiUtil;
import cn.tzauto.octopus.gui.main.EapMainController;
import cn.tzauto.octopus.gui.widget.paraviewpane.ParaViewPaneController;
import cn.tzauto.octopus.secsLayer.domain.EquipNodeBean;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import org.apache.ibatis.session.SqlSession;

import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * FXML Controller class
 *
 * @author luosy
 */
public class RcpMngPaneController implements Initializable {

    private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(RcpMngPaneController.class.getName());


    @FXML
    private TableView<SimpleRecipeProperty> dataTable; //tableView
    @FXML
    private TableColumn<SimpleRecipeProperty, String> numberCol = new TableColumn<>();
    @FXML
    private TableColumn<SimpleRecipeProperty, String> deviceCodeCol = new TableColumn<>();
    @FXML
    private TableColumn<SimpleRecipeProperty, String> recipeNameCol = new TableColumn<>();

    @FXML
    private TableColumn<SimpleRecipeProperty, String> recipeVersionNoCol = new TableColumn<>();

    @FXML
    private TableColumn<SimpleRecipeProperty, String> recipeVersionTypeCol = new TableColumn<>();

    @FXML
    private TableColumn<SimpleRecipeProperty, String> uploadByCol = new TableColumn<>();

    @FXML
    private TableColumn<SimpleRecipeProperty, String> createDateCol = new TableColumn<>();

    @FXML
    private TableColumn saveCol;

    @FXML
    private TableColumn delCol;

    private List<SimpleRecipeProperty> recipeList = new ArrayList<>(); //放置数据的集合

    public static ObservableList<SimpleRecipeProperty> list = FXCollections.observableArrayList(); //javaFX 的数据集合
    @FXML
    private ComboBox CMB_deviceCode;
    @FXML
    private ComboBox CMB_rcpType;
    @FXML
    private TextField TF_rcpName;

    @FXML
    private Button BTN_save;

    @FXML
    private Button BTN_query;

    @FXML
    private Button BTN_view;

    @FXML
    private Button BTN_download;

    @FXML
    private Button BTN_upload;

    @FXML
    private Button BTN_delete;

    @FXML
    private FlowPane functionPane;


    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        /**
         * 配置表格，绑定表格的每列
         */
        numberCol.setCellValueFactory(celldata -> celldata.getValue().getNumber());
        deviceCodeCol.setCellValueFactory(celldata -> celldata.getValue().getDeviceCode());
        recipeNameCol.setCellValueFactory(celldata -> celldata.getValue().getRecipeName());
        recipeVersionTypeCol.setCellValueFactory(celldata -> celldata.getValue().getVersionType());
        recipeVersionNoCol.setCellValueFactory(celldata -> celldata.getValue().getVersionNo());
        uploadByCol.setCellValueFactory(celldata -> celldata.getValue().getUploadBy());
        createDateCol.setCellValueFactory(celldata -> celldata.getValue().getUploadDate());
        saveCol.setCellValueFactory(new PropertyValueFactory<SimpleRecipeProperty, CheckBox>("checkBox"));
        delCol.setCellValueFactory(new PropertyValueFactory<SimpleRecipeProperty, CheckBox>("delCheckBox"));


    }

    public StackPane init() {


        StackPane rcpMngPane = new StackPane();
        try {

            ResourceBundle resourceBundle = ResourceBundle.getBundle("eap", new languageUtil().getLocale());//new Locale("zh", "TW");Locale.getDefault()

            rcpMngPane = FXMLLoader.load(getClass().getClassLoader().getResource("RcpMngPane.fxml"), resourceBundle);


        } catch (IOException ex) {
            Logger.getLogger(RcpMngPaneController.class.getName()).log(Level.SEVERE, null, ex);
        }

        functionPane = (FlowPane) rcpMngPane.lookup("#functionPane");

        BTN_save = (Button) rcpMngPane.lookup("#BTN_save");
        BTN_query = (Button) rcpMngPane.lookup("#BTN_query");
        BTN_view = (Button) rcpMngPane.lookup("#BTN_view");
        BTN_download = (Button) rcpMngPane.lookup("#BTN_download");
        BTN_upload = (Button) rcpMngPane.lookup("#BTN_upload");
        BTN_delete = (Button) rcpMngPane.lookup("#BTN_delete");

        if ("0".equals(GlobalConstants.getProperty("RECIPE_SAVE_CONTROL"))) {
            Platform.runLater(new Runnable() {
                public void run() {
                    functionPane.getChildren().remove(BTN_save);
                }
            });
        }
        if ("0".equals(GlobalConstants.getProperty("RECIPE_QUERY_CONTROL"))) {
            Platform.runLater(new Runnable() {
                public void run() {
                    functionPane.getChildren().remove(BTN_query);
                }
            });
        }
        if ("0".equals(GlobalConstants.getProperty("RECIPE_VIEW_CONTROL"))) {
            Platform.runLater(new Runnable() {
                public void run() {
                    functionPane.getChildren().remove(BTN_view);
                }
            });
        }
        if ("0".equals(GlobalConstants.getProperty("RECIPE_DOWNLOAD_CONTROL"))) {
            Platform.runLater(new Runnable() {
                public void run() {
                    functionPane.getChildren().remove(BTN_download);
                }
            });
        }
        if ("0".equals(GlobalConstants.getProperty("RECIPE_UPLOAD_CONTROL"))) {
            Platform.runLater(new Runnable() {
                public void run() {
                    functionPane.getChildren().remove(BTN_upload);
                }
            });
        }
        if ("0".equals(GlobalConstants.getProperty("RECIPE_DELETE_CONTROL"))) {
            Platform.runLater(new Runnable() {
                public void run() {
                    functionPane.getChildren().remove(BTN_delete);
                }
            });
        }

        dataTable = (TableView<SimpleRecipeProperty>) rcpMngPane.lookup("#dataTable");
        dataTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        fillComboBox(rcpMngPane);
//        initButton();
        showData();
        return rcpMngPane;
    }

    void fillComboBox(Pane rcpMngPane) {
        CMB_deviceCode = (ComboBox) rcpMngPane.lookup("#CMB_deviceCode");
        ObservableList deviceCodeList = FXCollections.observableArrayList();
        ObservableList versionTypeList = FXCollections.observableArrayList();
        for (EquipNodeBean equipNodeBean : GlobalConstants.stage.equipBeans) {
            deviceCodeList.add(equipNodeBean.getDeviceCode());
        }

        CMB_deviceCode.setItems(deviceCodeList);
        CMB_rcpType = (ComboBox) rcpMngPane.lookup("#CMB_rcpType");
        versionTypeList.add("Engineer");
        versionTypeList.add("Unique");
        versionTypeList.add("GOLD");

        CMB_rcpType.setItems(versionTypeList);
        TF_rcpName = (TextField) rcpMngPane.lookup("#TF_rcpName");
    }

    protected void updateData() {
        // TODO Auto-generated method stub

    }

    /**
     * 展示数据
     */
    private void showData() {


//        dataTable.setItems(generateDate());
//        dataTable.refresh();
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        RecipeService recipeService = new RecipeService(sqlSession);
        String deviceCode = null;
//        ComboBox CMB_deviceCode = (ComboBox) rcpMngPane.lookup("#CMB_deviceCode");
        if (CMB_deviceCode.getSelectionModel().getSelectedItem() != null) {
            deviceCode = CMB_deviceCode.getSelectionModel().getSelectedItem().toString();
            if ("--请选择--".equals(deviceCode)) {
                deviceCode = null;
            }
        }
        String recipeName = null;
//        TextField TF_rcpName = (TextField) rcpMngPane.lookup("#TF_rcpName");
        if (TF_rcpName.getText() != null) {
            recipeName = "%" + TF_rcpName.getText() + "%";
        }
        String versionType = null;
//        ComboBox CMB_rcpType = (ComboBox) rcpMngPane.lookup("#CMB_rcpType");
        if (CMB_rcpType.getSelectionModel().getSelectedItem() != null) {
            versionType = String.valueOf(CMB_rcpType.getSelectionModel().getSelectedItem());
            if ("--请选择--".equals(versionType)) {
                versionType = null;
            }
        }
        List<Recipe> recipes = null;
        if (recipeName == null || "".equals(recipeName) || deviceCode == null || "".equals(deviceCode)) {
            recipes = recipeService.searchRecipeRecent(GlobalConstants.clientId);
        } else {
            recipes = recipeService.searchRecipeByPara(recipeName, deviceCode, versionType, null);
        }
        list = FXCollections.observableArrayList();
        for (int i = 0; i < recipes.size(); i++) {
            list.add(new SimpleRecipeProperty(recipes.get(i), i + 1, recipes.get(i).getRecipeType()));
        }
        sqlSession.close();
        CMB_deviceCode.setPromptText("--请选择--");//.getSelectionModel().select("--请选择--");
        if (deviceCode != null) {
            CMB_deviceCode.getSelectionModel().select(deviceCode);
        }

        CMB_rcpType.setPromptText("--请选择--");
        if (versionType != null) {
            CMB_rcpType.getSelectionModel().select(versionType);
        }
        dataTable.setItems(list);
    }

    @FXML
    private void btnQueryClick() {
        dataTable.getItems().clear();
        showData();
    }

    @FXML
    private void btnUploadClick() throws IOException {
        GlobalConstants.isUpload = true;
        new EapMainController().loginInterface();
//        new UploadPaneController().init();
    }

    @FXML
    private void btnDownloadClick() throws IOException {
        GlobalConstants.table = dataTable;
        ObservableList<TablePosition> selectedCells = dataTable.getSelectionModel().getSelectedCells();
        if (selectedCells.size() == 0) {
            CommonUiUtil.alert(Alert.AlertType.WARNING, "没有选中的Recipe信息!");
            return;
        }
        GlobalConstants.isDownload = true;
        new EapMainController().loginInterface();


    }

    @FXML
    private void btnDelClick() {
        int flag = 0;

        for (int i = 0; i < list.size(); i++) {
            SimpleRecipeProperty srp = list.get(i);
            if (srp.getDelCheckBox().isSelected()) {
                flag++;
            }
        }

        if (flag == 0) {
            CommonUiUtil.alert(Alert.AlertType.WARNING, "没有选中的Recipe信息！");
            return;
        }

//        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
//        alert.setTitle("");
//        alert.setHeaderText("确定要删除所选数据吗?");
//        alert.setContentText("");
//        Optional result = alert.showAndWait();

        Optional<ButtonType> result = CommonUiUtil.alert(Alert.AlertType.CONFIRMATION, "确定要删除所选数据吗?");


        if (result.get() == ButtonType.OK) {
            for (int i = 0; i < list.size(); i++) {
                SimpleRecipeProperty srp = list.get(i);
                if (srp.getDelCheckBox().isSelected()) {
                    String deviceCode = srp.getDeviceCode().getValue();
                    String recipeName = srp.getRecipeName().getValue();
                    String versionType = srp.getVersionType().getValue();
                    String versionNo = srp.getVersionNo().getValue();
                    SqlSession sqlSession = MybatisSqlSession.getSqlSession();
                    RecipeService recipeService = new RecipeService(sqlSession);
                    java.util.List<Recipe> recipes = recipeService.searchRecipeByPara(recipeName, deviceCode, versionType, versionNo);
                    Recipe recipe = recipes.get(0);
//                    Attach attach = recipeService.searchAttachByRcpRowId(recipe.getId());
                    if (recipe.getVersionType().equals("Engineer")) {
                        if (recipeService.deleteRcp(recipe) > 0) {
                            //TODO 删除attach表的小关数据
                            recipeService.deleteRcpParaByRecipeId(recipe.getId());
                            sqlSession.commit();
                            Map mqMap = new HashMap();
                            mqMap.put("msgName", "DeleteRecipe");
                            mqMap.put("recipeId", recipe.getId());
                            GlobalConstants.C2SRcpDeleteQueue.sendMessage(mqMap);
                        } else {
                            sqlSession.rollback();
//                            CommonUiUtil.alert("删除失败");
                        }
                        sqlSession.close();

                    }

                }
            }
            CommonUiUtil.alert(Alert.AlertType.WARNING, "删除成功");
            dataTable.getItems().clear();
            showData();

        }


    }

    @FXML
    private void btnViewClick() throws IOException {
        ObservableList<TablePosition> selectedCells = dataTable.getSelectionModel().getSelectedCells();

        if (selectedCells.size() == 0) {
            CommonUiUtil.alert(Alert.AlertType.WARNING, "没有选中的Recipe信息!");
            return;
        }

        TablePosition pos = (TablePosition) dataTable.getSelectionModel().getSelectedCells().get(0);

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

        new ParaViewPaneController().init(deviceCode, recipeName, versionType, recipeVersionNo);

    }


    @FXML
    private void JB_RcpClear(ActionEvent event) {

        ObservableList<TablePosition> selectedCells = dataTable.getSelectionModel().getSelectedCells();

        if (selectedCells.size() == 0) {
            CommonUiUtil.alert(Alert.AlertType.WARNING, "没有选中的Recipe信息!");
            return;
        }

        TablePosition pos = (TablePosition) dataTable.getSelectionModel().getSelectedCells().get(0);

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

        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        RecipeService recipeService = new RecipeService(sqlSession);

        try {
            List<Recipe> recipes = recipeService.searchRecipeByPara(recipeName, deviceCode, versionType, recipeVersionNo);
            Recipe recipe = recipes.get(0);
            if (recipe.getRecipeType() == null || "".equals(recipe.getRecipeType()) || "N".equals(recipe.getRecipeType())) {
                recipe.setRecipeType("Y");
            } else {
                recipe.setRecipeType("N");
            }
            recipeService.modifyRecipe(recipe);
            sqlSession.commit();
            CommonUiUtil.alert(Alert.AlertType.INFORMATION, "当前Recipe " + recipe.getRecipeName() + "设置成功！");
//            reFreshRcpMng();
            dataTable.getItems().clear();
            showData();
        } catch (Exception e) {
            CommonUiUtil.alert(Alert.AlertType.WARNING, "当前Recipe设置失败！");
            sqlSession.rollback();
            logger.error("Exception:", e);
        } finally {
            sqlSession.close();
        }

    }

}
