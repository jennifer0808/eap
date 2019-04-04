package cn.tzauto.octopus.secsLayer.resolver.besi;


import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.biz.recipe.domain.RecipeTemplate;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.*;
import java.text.DecimalFormat;
import java.util.*;
import java.util.zip.GZIPInputStream;

/**
 * Created by Wang DanFeng.
 */
public class HP_EPL2400RecipeUtil {
    private static final Logger logger = Logger.getLogger(HP_EPL2400RecipeUtil.class.getName());


    private static void doUncompressFile(String inFileName) {

        try {

//            if (!getExtension(inFileName).equalsIgnoreCase("gz")) {
//                System.err.println("File name must have extension of \".gz\"");
//                System.exit(1);
//            }
            System.out.println("Opening the compressed file.");
            GZIPInputStream in = null;
            try {
                in = new GZIPInputStream(new FileInputStream(inFileName));
            } catch (FileNotFoundException e) {
                System.err.println("File not found. " + inFileName);
                System.exit(1);
            }

            System.out.println("Open the output file.");
//            String outFileName = getFileName(inFileName);
            String outFileName = inFileName + "temp";
            FileOutputStream out = null;
            try {
                out = new FileOutputStream(outFileName);
            } catch (FileNotFoundException e) {
                System.err.println("Could not write to file. " + outFileName);
                System.exit(1);
            }

            System.out.println("Transfering bytes from compressed file to the output file.");
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }

            System.out.println("Closing the file and stream");
            in.close();
            out.close();

        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

    }

    /**
     * 递归遍历所有父节点、子节点
     *
     * @param ele
     */
    public static void parserNode(Element ele) {

        System.out.println(ele.getName() + ":" + ele.getText().trim());
        //从Users根节点开始遍历，像【属性=值】的形式存为一个Attribute对象存储在List集合中
        List<Attribute> attrList = ele.attributes();
        for (Attribute attr : attrList) {
            //每循环一次，解析此节点的一个【属性=值】，没有则输出空
            String name = attr.getName();
            String value = attr.getValue();
            System.out.println(name + "=" + value);
        }

        List<Element> eleList = ele.elements();
        //递归遍历父节点下的所有子节点
        for (Element e : eleList) {
            parserNode(e);
        }
    }

    /**
     * 递归遍历所有父节点、子节点
     *
     * @param ele
     */
    public static String rcpGroupName = "";

    public static Map getRecipeParametersMap(Element ele) {
        Map<String, RecipePara> map = new HashMap<>();
        System.out.println(ele.getName() + "====================");
        //只有RecipeParameter为参数
        String paraId = "";
        String paraName = "";
        String paraValue = "";
        if ("RcpGroup".equals(ele.getName())) {
            System.out.println(ele.attribute("RecipeName").getValue() + "*********************");
            rcpGroupName = ele.attribute("RecipeName").getValue();
        }
        if ("RecipeParameter".equals(ele.getName())) {
            RecipePara recipePara = new RecipePara();
            paraId = ele.attribute("RecipeId").getValue();
            paraName = ele.attribute("Name").getValue();
            Element variaType;
            Attribute valueAttribute;
            Element theMatrix;
            Attribute dateAttribute;
            if ((variaType = ele.element("VariaType")) != null && (theMatrix = ele.element("VariaType").element("TheMatrix")) == null) {
                if ((valueAttribute = variaType.attribute("DoubleValue")) != null || (valueAttribute = variaType.attribute("BoolValue")) != null
                        || (valueAttribute = variaType.attribute("LongValue")) != null || (valueAttribute = variaType.attribute("CStringValue")) != null) {
                    paraValue = valueAttribute.getValue();
                }
            } else if (variaType != null && (theMatrix = ele.element("VariaType").element("TheMatrix")) != null && (dateAttribute = theMatrix.attribute("Data")) != null) {
                if (dateAttribute.getValue() != null) {
                    paraValue = dateAttribute.getValue();
                }
            }
            String numConverted = "";
            DecimalFormat decimalFormat = new DecimalFormat("##########.##########");
            if (!"".equals(paraValue)) {
                if (paraValue.matches("^-?[0-9]+(.[0-9]+)?$")) {
                    if (paraValue.contains(".")) {
                        numConverted = decimalFormat.format(Double.parseDouble(paraValue));
//                        System.out.println("转换参数");
                    } else {
                        numConverted = decimalFormat.format(Integer.parseInt(paraValue));
                    }
                }else{
                    numConverted = paraValue;
                }
            }
            String groupName = rcpGroupName;
            recipePara.setParaCode(paraId);
            recipePara.setParaName(paraName);
            recipePara.setSetValue(numConverted);
            recipePara.setRemarks(groupName);
            map.put(paraId, recipePara);
        }
        List<Element> eleList = ele.elements();
        //递归遍历父节点下的所有子节点
        for (Element e : eleList) {
            Map<String, RecipePara> paraMap = getRecipeParametersMap(e);
            if (paraMap.size() != 0) {
                map.putAll(paraMap);
            }
        }
        return map;
    }

    /**
     * 生成表格形式
     *
     * @param ele
     */
    public static Map<String, Object> readXml(Element ele) {
        Map<String, Object> map = new HashMap<>();
//        System.out.println(ele.getName() + "====================");
        //只有RecipeParameter为参数

        String paraName = "";
        String paraValue = "";
        String rcpGroupName = "";
        if ("RcpGroup".equals(ele.getName())) {
            System.out.println(ele.attribute("RecipeName").getValue() + "*********************");
            rcpGroupName = ele.attribute("RecipeName").getValue();
            List<Element> eleList = ele.elements();
            //递归遍历父节点下的所有子节点
            List<Map> parameterList = new ArrayList<>();
            for (Element e : eleList) {
                Map<String, Object> paraMap = readXml(e);
                if (paraMap.size() != 0) {
                    parameterList.add(paraMap);
                }
            }
            map.put(rcpGroupName, parameterList);
            return map;
        }
        if ("RecipeParameter".equals(ele.getName())) {
            paraName = ele.attribute("Name").getValue();
            Element variaType;
            Attribute valueAttribute;
            Element theMatrix;
            Attribute dateAttribute;
            if ((variaType = ele.element("VariaType")) != null && (theMatrix = ele.element("VariaType").element("TheMatrix")) == null) {
                if ((valueAttribute = variaType.attribute("DoubleValue")) != null || (valueAttribute = variaType.attribute("BoolValue")) != null
                        || (valueAttribute = variaType.attribute("LongValue")) != null || (valueAttribute = variaType.attribute("CStringValue")) != null) {
                    paraValue = valueAttribute.getValue();
                }
            } else if (variaType != null && (theMatrix = ele.element("VariaType").element("TheMatrix")) != null && (dateAttribute = theMatrix.attribute("Data")) != null) {
                if (dateAttribute.getValue() != null) {
                    paraValue = dateAttribute.getValue();
                }
            }
            map.put(paraName, paraValue);
//            List<Element> eleList = ele.elements();
//            //递归遍历父节点下的所有子节点
//            for (Element e : eleList) {
//                Map<String, String> paraMap = getRecipeParametersMap(e);
//                if (paraMap.size() != 0) {
//                    map.putAll(paraMap);
//                }
//            }
        }
//        List<Element> eleList = ele.elements();
//        //递归遍历父节点下的所有子节点
//        for (Element e : eleList) {
//            Map<String, String> paraMap = getRecipeParametersMap(e);
//            if (paraMap.size() != 0) {
//                map.putAll(paraMap);
//            }
//        }
        return map;
    }


    /**
     * 遍历当前节点元素下面的所有(元素的)子节点
     *
     * @param node
     */
    public static void listNodes(Element node) {

//        if ("MeasurandsGroup".equals(node.getName())||"LowLimitFail".equals(node.getName())) {
        //List<Node> lists = node.selectNodes("//Name");
        //System.out.println(lists.get(0).getText());
        System.out.println("当前节点的名称：：" + node.getName());
        // 获取当前节点的所有属性节点
        List<Attribute> list = node.attributes();
        // 遍历属性节点
        for (Attribute attr : list) {
            System.out.println(attr.getText() + "-----" + attr.getName() + "---" + attr.getValue());
        }

        if (!(node.getTextTrim().equals(""))) {
            System.out.println("文本内容：：：：" + node.getText());
        }
        //List<Node> lisst=node.selectNodes("MeasurandsGroup//Name");
        List<Element> lisst = node.elements();
        if (lisst != null) {
            for (Element n : lisst) {
//                System.out.println("====" + n.getText() + "====");
            }
        }
//        }
        // 当前节点下面子节点迭代器
        Iterator<Element> it = node.elementIterator();
        // 遍历
        while (it.hasNext()) {
            // 获取某个子节点对象
            Element e = it.next();
            // 对子节点进行遍历
            listNodes(e);
        }
    }

    public static List<RecipePara> transferRcpFromDB(String filePath, String deviceType) {
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        RecipeService recipeService = new RecipeService(sqlSession);
        List<RecipeTemplate> recipeTemplates = recipeService.searchRecipeTemplateByDeviceTypeCode(deviceType, "RecipePara");
        sqlSession.close();
        List<RecipePara> recipeParas = new ArrayList<>();
        doUncompressFile(filePath);
        SAXReader saxReader = new SAXReader();
        try {
            Document document = saxReader.read(new File(filePath + "temp"));
            Element root = document.getRootElement();
            Map<String, RecipePara> map  = getRecipeParametersMap(root);
            for(RecipePara recipePara:map.values()){
                for(RecipeTemplate recipeTemplate:recipeTemplates){
                    if(recipePara.getRemarks().equals(recipeTemplate.getGroupName())&&recipePara.getParaName().equals(recipeTemplate.getParaName())){
                        recipePara.setParaCode(recipeTemplate.getParaCode());
                        recipeParas.add(recipePara);
                        break;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            File file = new File(filePath + "temp");
            if (file.exists()) {
//                file.delete();
            }
        }


        return recipeParas;
    }


    public static void main(String[] args) throws DocumentException {
        SAXReader saxReader = new SAXReader();
        String str = "D:\\桌面文件\\长电\\HP_EPL2400\\besiRecipeFile";
        String str2 = "C:\\Users\\WDF\\Desktop\\QFN LL small CRT_V4.txt";
        List<RecipePara> recipeParas = transferRcpFromDB(str2,"BESIHP_EPL2400");
        for (RecipePara recipePara:recipeParas){
            System.out.println(recipePara.getParaCode()+"======"+recipePara.getParaName()+"======"+recipePara.getSetValue());
        }
//        Document document = saxReader.read(new File(str2));
//        Element root = document.getRootElement();
//        parserNode(root);
//        Map<String, RecipePara> map = getRecipeParametersMap(root);
//        for (Map.Entry<String, RecipePara> entry : map.entrySet()) {
//            int i = 100-entry.getKey().length();
//            System.out.println(entry.getKey() + "======" + String.format("%-"+i+"s",entry.getValue().getParaName())+entry.getValue().getRemarks());
//        }


//        Map<String, Object> map = readXml(root);
//        for (Map.Entry<String, Object> entry : map.entrySet()) {
//            String groupName = entry.getKey();
//            List<HashMap<String,String>> list = (List<HashMap<String,String>>)entry.getValue();
//            for (int i = 0; i < list.size(); i++) {
//                for(Map.Entry<String, String> entry1:list.get(i).entrySet()){
//                    System.out.println(groupName+"============="+entry1.getKey());
//                }
//
//            }
//        }
//        transferRcpFromDB(str,"");
    }

}
