package cn.tzauto.octopus.isecsLayer.resolver.hta;


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
public class FI7300Resolver {
    private static Logger logger = Logger.getLogger(FI7300Resolver.class);

    public static List<RecipePara> transferFromDB(Map paraMap, String deviceType){
        SqlSession sqlSession= MybatisSqlSession.getSqlSession();
        RecipeService recipeService=new RecipeService(sqlSession);//deviceType="FI7300"
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
            recipePara.setSetValue((String) paraMap.get(key));
            recipePara.setMinValue(recipeTemplate.getMinValue());
            recipePara.setMaxValue(recipeTemplate.getMaxValue());
            recipePara.setParaMeasure(recipeTemplate.getParaUnit());
            recipeParaList.add(recipePara);
        }
        return recipeParaList;
    }


public static Map<String, Object> transferFrom2Xml(String filePath,String recipeName) {
    File file = new File(filePath);
    if (!file.exists()) {
        logger.error("您的路径："+filePath+"中该"+recipeName+"卡控文件不存在!");
        return null;
    }
    System.out.println(filePath);
    Map<String, Object> map = new HashMap<>();
    try {
        /**
         * xml文件解析：通过SAXReader去读整个文件
         */
        //创建SAXReader对象
        SAXReader reader = new SAXReader();
        //读取文件 转换成Document
        Document document = reader.read(file);
        System.out.println("document:"+document);
        //获取根节点元素对象
        Element root = document.getRootElement();
        for ( Iterator iterInner = root.elementIterator(); iterInner.hasNext(); ) {
            Element elementInner = (Element) iterInner.next();

            switch (elementInner.getName()){
                case "BottomLeadPitch":
                    map.putAll(listNodes(elementInner,"BottomLeadPitch"));
                    break;
                case "BottomSpacing":
                    map.putAll(listNodes(elementInner,"BottomSpacing"));
                    break;
                case "BottomLeadLength":
                    map.putAll(listNodes(elementInner,"BottomLeadLength"));
                    break;
                case "BottomLeadWidth":
                    map.putAll(listNodes(elementInner,"BottomLeadWidth"));
                    break;
                case "DimensionX":
                    map.putAll(listNodes(elementInner,"DimensionX"));
                    break;
                case "DimensionY":
                    map.putAll(listNodes(elementInner,"DimensionY"));
                    break;
                case "Spacing":
                    map.putAll(listNodes(elementInner,"Spacing"));
                    break;
                default:
                    break;
            }

        }
    } catch (DocumentException e) {
        e.printStackTrace();
    }
    System.out.println(map);
    return map;
}

    public static Map<String, Object> listNodes(Element elementInner,String elementParent){
        Map<String,Object> map=new HashMap<>();
        for(Iterator iterator= elementInner.elementIterator();iterator.hasNext(); ){
            Element element = (Element) iterator.next();
            System.out.println(element);
            map.put(elementParent+"_"+element.getName(),element.getData());
        }
        return map;
    }


    public static void main(String[] args) {
        Map<String, Object> map= FI7300Resolver.transferFrom2Xml("D:\\autotz\\hta\\Recipe\\FI7300 AutoRecipe\\JCET-A-FCQFN-4X4-26L-T0.85.xml","");
        System.out.println(map.size());
        Set<String> StringKey= map.keySet();
        SqlSession session = MybatisSqlSession.getSqlSession();
        RecipeTemplateMapper recipeTemplateMapper = session.getMapper(RecipeTemplateMapper.class);
        int n=1;
        for(String s:StringKey){
            RecipeTemplate template = new RecipeTemplate();
            template.setId(UUID.randomUUID().toString());
            template.setDeviceTypeId(null);
            template.setDeviceTypeCode("FI7300");
            template.setParaCode(String.valueOf(n++));
            template.setParaName(s);
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

        List<RecipePara> list = FI7300Resolver.transferFromDB(map, "FI7300" );
        for (int i = 0; i < list.size(); i++) {
            System.out.println(list.get(i).getParaCode() + "=====" + list.get(i).getParaName() + "=====" + list.get(i).getSetValue());
        }
        session.commit();
        session.close();

    }
}
