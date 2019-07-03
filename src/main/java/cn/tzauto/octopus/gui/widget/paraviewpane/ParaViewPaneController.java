package cn.tzauto.octopus.gui.widget.paraviewpane;

import cn.tzauto.octopus.biz.recipe.domain.Recipe;
import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.common.globalConfig.GlobalConstants;
import cn.tzauto.octopus.common.util.language.languageUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.apache.ibatis.session.SqlSession;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;

/**
 * Created by wj_co on 2019/2/13.
 */
public class ParaViewPaneController implements Initializable {

    private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(ParaViewPaneController.class);

    @FXML
    private TableColumn<SimpleRecipeParaProperty, String> paraCodeCol = new TableColumn<>();
    @FXML
    private TableColumn<SimpleRecipeParaProperty, String> paraNameCol = new TableColumn<>();
    @FXML
    private TableColumn<SimpleRecipeParaProperty, String> paraShotNameCol = new TableColumn<>();
    @FXML
    private TableColumn<SimpleRecipeParaProperty, String> setValueCol = new TableColumn<>();
    @FXML
    private TableColumn<SimpleRecipeParaProperty, String> minValueCol = new TableColumn<>();
    @FXML
    private TableColumn<SimpleRecipeParaProperty, String> maxValueCol = new TableColumn<>();
    @FXML
    private TableColumn<SimpleRecipeParaProperty, String> paraMeasureCol = new TableColumn<>();

    @FXML
    private TableView<SimpleRecipeParaProperty> dataTable; //tableView

    @FXML
    private TextField JTF_RcpName;
    @FXML
    private TextField JTF_DvcCode;
    @FXML
    private TextField JTF_CreateBy;
    @FXML
    private TextField JTF_CreateTime;

    public static  Stage stage= new Stage();
    public static boolean flag = false;
    static {
        stage.setTitle("查看");
        stage.setWidth(800);
        stage.setHeight(500);

        stage.setAlwaysOnTop(true);
        stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent event) {
                flag = false;
            }
        });
    }

//    private List<RecipePara> recipeParaList = new ArrayList<>(); //放置数据的集合

    ObservableList<SimpleRecipeParaProperty> list = FXCollections.observableArrayList(); //javaFX 的数据集合

    java.util.List<Recipe> recipes = new ArrayList<>();

    java.util.List<RecipePara> recipeParaList = new ArrayList<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        paraCodeCol.setCellValueFactory(celldata -> celldata.getValue().getParaCode());
        paraNameCol.setCellValueFactory(celldata -> celldata.getValue().getParaName());
        paraShotNameCol.setCellValueFactory(celldata -> celldata.getValue().getParaShotName());
        setValueCol.setCellValueFactory(celldata -> celldata.getValue().getSetValue());
        minValueCol.setCellValueFactory(celldata -> celldata.getValue().getMinValue());
        maxValueCol.setCellValueFactory(celldata -> celldata.getValue().getMaxValue());
        paraMeasureCol.setCellValueFactory(celldata -> celldata.getValue().getParaMeasure());


    }

    public void init(String deviceCode, String recipeName, String versionType, String recipeVersionNo) {
        flag = true;
        Image image = new Image(getClass().getClassLoader().getResourceAsStream("logoTaiZhi.png"));
        stage.getIcons().add(image);
        AnchorPane pvPane = new AnchorPane();
        try {
            ResourceBundle resourceBundle = ResourceBundle.getBundle("eap", new languageUtil().getLocale());//new Locale("zh", "TW");Locale.getDefault()

            pvPane = FXMLLoader.load(getClass().getClassLoader().getResource("ParaViewPane.fxml"),resourceBundle);

        } catch (IOException ex) {
        }
        stage.setResizable(false);
        Scene scene = new Scene(pvPane);
        stage.setScene(scene);


        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        RecipeService recipeService = new RecipeService(sqlSession);
        //查询recipe表
        recipes = recipeService.searchRecipeByPara(recipeName, deviceCode, versionType, recipeVersionNo);

        dataTable = (TableView) pvPane.lookup("#dataTable");


        //查询recipepara表
        recipeParaList = recipeService.searchRecipeParaByRcpRowId(recipes.get(0).getId());

        if (recipeParaList.size() != 0) {
            for (RecipePara r : recipeParaList) {
                SimpleRecipeParaProperty s = new SimpleRecipeParaProperty();
                s.setParaCode(r.getParaCode());
                s.setParaName(r.getParaName());
                s.setParaShotName(r.getParaShotName());
                s.setSetValue(r.getSetValue());
                s.setMinValue(r.getMinValue());
                s.setMaxValue(r.getMaxValue());
                s.setParaMeasure(r.getParaMeasure());
                list.add(s);
            }

            dataTable.setItems(list);
        }
        sqlSession.close();

        JTF_RcpName = (TextField) pvPane.lookup("#JTF_RcpName");
        JTF_RcpName.setEditable(false);
        JTF_RcpName.setText(recipes.get(0).getRecipeName());
        JTF_DvcCode = (TextField) pvPane.lookup("#JTF_DvcCode");
        JTF_DvcCode.setEditable(false);
        JTF_DvcCode.setText(recipes.get(0).getDeviceCode());
        JTF_CreateBy = (TextField) pvPane.lookup("#JTF_CreateBy");
        JTF_CreateBy.setEditable(false);
        JTF_CreateBy.setText(recipes.get(0).getCreateBy());
        JTF_CreateTime = (TextField) pvPane.lookup("#JTF_CreateTime");
        JTF_CreateTime.setEditable(false);
        JTF_CreateTime.setText(GlobalConstants.dateFormat.format(recipes.get(0).getCreateDate()));


        stage.show();
    }

    @FXML
    private void closeClick() {
//        ((Node) (event.getSource())).getScene().getWindow().hide();
        stage.close();
        flag = false;
    }
}
