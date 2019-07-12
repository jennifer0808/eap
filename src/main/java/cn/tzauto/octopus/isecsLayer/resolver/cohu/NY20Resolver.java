package cn.tzauto.octopus.isecsLayer.resolver.cohu;


import cn.tzauto.octopus.biz.recipe.dao.RecipeTemplateMapper;
import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.biz.recipe.domain.RecipeTemplate;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.File;
import java.util.*;

/**
 * Created by wj_co
 */
public class NY20Resolver {
    private static Logger logger = Logger.getLogger(NY20Resolver.class);

    public static List<RecipePara> transferFromDB(String deviceType){
        SqlSession sqlSession= MybatisSqlSession.getSqlSession();
        RecipeService recipeService=new RecipeService(sqlSession);//deviceType="NY20"
        List<RecipeTemplate> recipeTemplates = recipeService.searchRecipeTemplateByDeviceTypeCode(deviceType, "RecipePara");
        sqlSession.close();
        List<RecipePara> recipeParaList = new ArrayList<>();
        for (RecipeTemplate recipeTemplate : recipeTemplates) {
            String key = recipeTemplate.getParaName();
            if (key == null || "".equals(key)) {
                continue;
            }
            RecipePara recipePara = new RecipePara();
            recipePara.setParaCode(recipeTemplate.getParaCode());
            recipePara.setParaName(recipeTemplate.getParaName());
            recipePara.setParaShotName(recipeTemplate.getParaShotName());
            recipePara.setParaMeasure(recipeTemplate.getParaUnit());
            recipePara.setSetValue(recipeTemplate.getSetValue());
            recipePara.setMinValue(recipeTemplate.getMinValue());
            recipePara.setMaxValue(recipeTemplate.getMaxValue());
            recipeParaList.add(recipePara);
        }
        return recipeParaList;
    }


    public static Map<String, NY20POJO> getParaFromVision(String filePath, String recipeName) throws DocumentException {
        File file = new File(filePath);
        if (!file.exists()) {
            logger.error("您的路径："+filePath+"中该"+recipeName+"卡控文件不存在!");
            return null;
        }
        SAXReader saxReader = new SAXReader();
        Document document = saxReader.read(file);
        Element root = document.getRootElement();
        return getItemParaMap(root);
    }


    public static Map<String, NY20POJO> getItemParaMap(Element root) {

        Map<String, NY20POJO> itemMap = new HashMap<>();
        itemMap.putAll(getItemLastParaMap(root));
        /*
           XML路径：BasicItem为根节点
        BasicItem/Children/BasicItem(id=i8)/Children(此时判断是否为空)/BasicItem(i9)/Measurands/List（判断是否为空）/KeyValuePairOfMeasurandKeyMeasurandYxVEEYjo/value/
         */

        Element childrenOfRoot2 = root.element("Children");
        List<Element> basicItemOfRoot2s = childrenOfRoot2.elements("BasicItem");
        if (basicItemOfRoot2s.size() > 0) {
            for (Element basicItemOfRoot2 : basicItemOfRoot2s) {
                Element cOfR2 = basicItemOfRoot2.element("Children");
                List<Element> bOfR2s = cOfR2.elements("BasicItem");
                if (bOfR2s.size() > 0) {
                    for (Element bOfR2 : bOfR2s) {
                        itemMap.putAll(getItemLastParaMap(bOfR2));
                    }
                }

            }

        }

        return itemMap;
    }

    public static Map<String, NY20POJO> getItemLastParaMap(Element root) {
        /**
        XML:路径：BasicItem为根节点
       BasicItem/Measurands/List/KeyValuePairOfMeasurandKeyMeasurandYxVEEYjo/value/NominalValue
                                                                               /key/name
                                                                              /Tolerance
                                                                                     /LowLimitFail
                                                                                     /HighLimitFail
        */

        Map<String, NY20POJO> itemMap = new HashMap<>();
        String basicItem = "";
        if ("BasicItem".equals(root.getName())) {
            basicItem = root.elementText("Name");
        }
        Element measurandsOfRoot = root.element("Measurands");
        Element listOfRoot = measurandsOfRoot.element("List");
        List<Element> keyValueOfRoots = listOfRoot.elements("KeyValuePairOfMeasurandKeyMeasurandYxVEEYjo");
        if (keyValueOfRoots.size() > 0) {
            for (Element keyValueOfRoot : keyValueOfRoots) {
                NY20POJO ny20pojo = new NY20POJO();
                if (keyValueOfRoot.element("value").element("MeasurandsGroup") == null) {
                    continue;
                }
//                System.out.println(keyValueOfRoot.element("value").attribute("Id").getText());
                String name = keyValueOfRoot.element("value").element("MeasurandsGroup").elementText("Name");
                String unit = keyValueOfRoot.element("value").element("Unit").elementText("Name");
                String nominalValue = keyValueOfRoot.element("value").elementText("NominalValue");
                /**
                 * LowLimitFail，HighLimitFail有些包含在Tolerances中
                 */
                Element tolerances = keyValueOfRoot.element("value").element("Tolerances");
                String lowLimitFail = "";
                String highLimitFail = "";
                if (tolerances != null) {
                    lowLimitFail = tolerances.elementText("LowLimitFail");
                    highLimitFail = tolerances.elementText("HighLimitFail");
                } else {
                    lowLimitFail = keyValueOfRoot.element("value").elementText("LowLimitFail");
                    highLimitFail = keyValueOfRoot.element("value").elementText("HighLimitFail");
                }
                //将basicItem的名称修改为black/white(黑白两种模式，但是名称可能有变)
                if (basicItem.contains("black") || basicItem.contains("Black")) {
                    basicItem = "Black";
                } else if (basicItem.contains("white") || basicItem.contains("White")) {
                    basicItem = "White";
                }
                ny20pojo.setName(name+"_"+basicItem);
                ny20pojo.setHighLimitFail(highLimitFail);
                ny20pojo.setLowLimitFail(lowLimitFail);
                ny20pojo.setNominalValue(nominalValue);
                ny20pojo.setUnit(unit);
                itemMap.put(name + "_" + basicItem, ny20pojo);
            }
        }
       System.out.println(itemMap);
        return itemMap;
    }


    public static void main(String[] args) throws DocumentException {

       Map<String,NY20POJO> map= NY20Resolver.getParaFromVision("D:\\RECIPE\\NY20\\Generic\\DFNWB3X3_14L_B_T075.xml","");
        System.out.println(map.size());
        Set<String> StringKey= map.keySet();
        SqlSession session = MybatisSqlSession.getSqlSession();
        RecipeTemplateMapper recipeTemplateMapper = session.getMapper(RecipeTemplateMapper.class);
        int n=1;
        for(String s:StringKey){
            RecipeTemplate template = new RecipeTemplate();
            template.setId(UUID.randomUUID().toString());
            template.setDeviceTypeId(null);
            template.setDeviceTypeCode("NY20");
            template.setParaCode(String.valueOf(n++));
            template.setParaName(s);
            template.setParaUnit(map.get(s).getUnit());
            template.setSetValue(map.get(s).getNominalValue());
            template.setMinValue(map.get(s).getLowLimitFail());
            template.setMaxValue(map.get(s).getHighLimitFail());
            template.setDeviceVariableType("RecipePara");
            template.setCreateBy("jen");
            template.setCreateDate(new Date());
            template.setUpdateBy("jen");
            template.setUpdateDate(new Date());
            template.setDelFlag("0");
            int num=  recipeTemplateMapper.insert(template);
            System.out.println("num:"+num);
            System.out.println(s);
        }

        List<RecipePara> list = NY20Resolver.transferFromDB("NY20" );
        for (int i = 0; i < list.size(); i++) {
            System.out.println(list.get(i).getParaCode() + "=====" + list.get(i).getParaName() + "=====" + list.get(i).getSetValue());
        }
        session.commit();
        session.close();

    }



}
