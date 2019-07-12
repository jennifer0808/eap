package cn.tzauto.octopus.isecsLayer.resolver.vision;


import cn.tzauto.octopus.biz.recipe.dao.RecipeTemplateMapper;
import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.biz.recipe.domain.RecipeTemplate;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import org.apache.ibatis.session.SqlSession;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.File;
import java.util.*;

/**
 * Created by wj_co
 */
public class XMLRecipeUtil {
    private static List vaList = new ArrayList();

    public static List<RecipePara> transferFromDB(Map paraMap, String deviceType){
        SqlSession sqlSession= MybatisSqlSession.getSqlSession();
        RecipeService recipeService=new RecipeService(sqlSession);//deviceType="XWSTS"
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

    public static Map<String,String> transferFromXml(String filePath){
        File file=new File(filePath);
        if(!file.exists()){
            return null;
        }
        System.out.println(filePath);
        Map<String, String> map = new HashMap<>();
        try{
            SAXReader reader=new SAXReader();
            Document document= reader.read(file);
            Element root= document.getRootElement();
            Iterator iterator= root.elementIterator();
               while(iterator.hasNext()){
                   Element elementIter = (Element) iterator.next();
                   System.out.println(elementIter);
                   map.put(elementIter.getName(), (String) elementIter.getData());

               }
        }catch (Exception e){
            e.printStackTrace();
        }
        return map;
    }

    public static Map<String, Object> transferFrom2Xml(String filePath) {
        File file = new File(filePath);
        if (!file.exists()) {
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
                    //获取根节点元素对象
                    Element root = document.getRootElement();
                /*    //从根节点遍历子节点
                    Iterator iterator= root.elementIterator();
                    System.out.println(iterator);*/
                    for ( Iterator iterInner = root.elementIterator(); iterInner.hasNext(); ) {
                        Element elementInner = (Element) iterInner.next();
                        if(elementInner.getName().equals("PrintParameter")){
                           Map mapResult= listNodes(elementInner);
                            map=mapResult;
                            break;
                        }
                    }
                } catch (DocumentException e) {
                    e.printStackTrace();
                }
        return map;
    }

    public static Map<String, Object> listNodes(Element elementInner){
        Map<String,Object> map=new HashMap<>();
        for(Iterator iterator= elementInner.elementIterator();iterator.hasNext(); ){
        Element element = (Element) iterator.next();
            System.out.println(element);
            map.put(element.getName(),element.getData());
        }
        return map;
    }


    public static void main(String[] args) {

        Map<String, String> map=  XMLRecipeUtil.transferFromXml("D:\\autotz\\meiya\\Recipe\\Plasma Recipes\\F04-2.flextrak.recipe.xml");
      //  Map<String,Object> map=  XMLRecipeUtil.transferFrom2Xml("D:\\autotz\\ekra\\Recipe\\PrintPrograms\\8 inch notch w175 s160 8006.espdatx");
        System.out.println(map.size());
        Set<String> StringKey= map.keySet();
        SqlSession session = MybatisSqlSession.getSqlSession();
        RecipeTemplateMapper recipeTemplateMapper = session.getMapper(RecipeTemplateMapper.class);
        int n=1;
        for(String s:StringKey){
            RecipeTemplate template = new RecipeTemplate();
            template.setId(UUID.randomUUID().toString());
            template.setDeviceTypeId(null);
            template.setDeviceTypeCode("FasTRAK");
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

        List<RecipePara> list = FI7300RecipeUtil.transferFromDB(map, "FasTRAK" );
        for (int i = 0; i < list.size(); i++) {
            System.out.println(list.get(i).getParaCode() + "=====" + list.get(i).getParaName() + "=====" + list.get(i).getSetValue());
        }
        session.commit();
        session.close();
    }

}
