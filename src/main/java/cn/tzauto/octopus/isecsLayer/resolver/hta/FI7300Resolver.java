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

import java.io.*;
import java.util.*;

/**
 * Created by wj_co on 2018/9/29.
 */
public class FI7300Resolver {
    private static Logger logger = Logger.getLogger(FI7300Resolver.class);
    private static  Map<String, Object> mapPara = new HashMap<>();

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

    public static Map<String, Object> transferFrom2Xml(String handlerPath ,String visionPath , String recipeName) {

        transferFrom2Handler(handlerPath,recipeName);
        transferFrom2Vision(visionPath,recipeName);
        return mapPara;

    }

    public static Map<String, Object> transferFrom2Handler(String filePath,String recipeName) {
        File file = new File(filePath);
        if (!file.exists()) {
            logger.error("您的handler路径："+filePath+"中该"+recipeName+"卡控文件不存在!");
            return null;
        }
        Map<String, Object> map = new HashMap<>();
        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
            String line = "";
            while ((line = br.readLine()) != null) {
                    String[] nAv = line.split("=");
                    mapPara.put(nAv[0] , nAv[1]);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            try {
                if (br != null) {
                    br.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return mapPara;
        }

    }


   public static Map<String, Object> transferFrom2Vision(String filePath,String recipeName) {
    File file = new File(filePath);
    if (!file.exists()) {
        logger.error("您的vision路径："+filePath+"中该"+recipeName+"卡控文件不存在!");
        return null;
    }
    try {
        /**
         * xml文件解析：通过SAXReader去读整个文件
         */
        //创建SAXReader对象
        SAXReader reader = new SAXReader();
        //读取文件 转换成Document
        Document document = reader.read(file);
        //获取根节点元素对象
        Element root = document.getRootElement();
        for ( Iterator iterInner = root.elementIterator(); iterInner.hasNext(); ) {
            Element elementInner = (Element) iterInner.next();

            switch (elementInner.getName()){
                case "BottomLeadPitch":
                    mapPara.putAll(listNodes(elementInner,"BottomLeadPitch"));
                    break;
                case "BottomSpacing":
                    mapPara.putAll(listNodes(elementInner,"BottomSpacing"));
                    break;
                case "BottomLeadLength":
                    mapPara.putAll(listNodes(elementInner,"BottomLeadLength"));
                    break;
                case "BottomLeadWidth":
                    mapPara.putAll(listNodes(elementInner,"BottomLeadWidth"));
                    break;
                case "DimensionX":
                    mapPara.putAll(listNodes(elementInner,"DimensionX"));
                    break;
                case "DimensionY":
                    mapPara.putAll(listNodes(elementInner,"DimensionY"));
                    break;
                case "Spacing":
                    mapPara.putAll(listNodes(elementInner,"Spacing"));
                    break;
                default:
                    break;
            }

        }
    } catch (DocumentException e) {
        e.printStackTrace();
    }
    return mapPara;
}

    public static Map<String, Object> listNodes(Element elementInner,String elementParent){
        Map<String,Object> map=new HashMap<>();
        for(Iterator iterator= elementInner.elementIterator();iterator.hasNext(); ){
            Element element = (Element) iterator.next();
            map.put(elementParent+"_"+element.getName(),element.getData());
        }
        return map;
    }


    public static void main(String[] args) {
        Map<String, Object> visionMap= FI7300Resolver.transferFrom2Vision("D:/RECIPE/RECIPE/FI7300_0001QFN-JCET-A-WBQFN-5X5-40L-T0.75temp/vision/QFN-JCET-A-WBQFN-5X5-40L-T0.75/5sSpec.xml","");
        Map<String, Object> handlerMap = FI7300Resolver.transferFrom2Handler("D://RECIPE/RECIPE/FI7300_0001QFN-JCET-A-WBQFN-5X5-40L-T0.75temp/handler/QFN-JCET-A-WBQFN-5X5-40L-T0.75/JCET-A-WBQFN-5X5-40L-T0.75.dat","");
        System.out.println(visionMap.size()+handlerMap.size());
        handlerMap.putAll(visionMap);
        Set<String> StringKey= handlerMap.keySet();
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

        List<RecipePara> list = FI7300Resolver.transferFromDB(handlerMap, "FI7300" );
        for (int i = 0; i < list.size(); i++) {
            System.out.println(list.get(i).getParaCode() + "=====" + list.get(i).getParaName() + "=====" + list.get(i).getSetValue());
        }
        session.commit();
        session.close();

    }
}
