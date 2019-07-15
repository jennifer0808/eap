package cn.tzauto.octopus.secsLayer.resolver.besi;


import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;


public class FicoRecipeUtil {

    private static List vaList = new ArrayList();

    public static List transferFicoRcp(String ppbody) throws Exception {
        vaList.clear();
        //创建SAXReader对象  
        SAXReader reader = new SAXReader();
        //删除开头注释
        // ppbody=ppbody.replace("<!--Recipe PPM Export File-->", "");

        // ppbody="<?xml version=\"1.0\" encoding=\"utf-8\"?>"+ppbody;
        //读取文件 转换成Document  
        Document document = reader.read(ppbody);
        //获取根节点元素对象  
        Element root = document.getRootElement();
        //遍历 

        return listNodes(root);
    }
    //遍历当前节点下的所有节点  

    public static List listNodes(Element node) {


        //首先获取当前节点的所有属性节点  
        List<Attribute> list = node.attributes();
        //遍历属性节点  
        for (Attribute attribute : list) {
            if (node.getName().equals("Parameter")) {
                //     RecipePara recipePara = new RecipePara();
                String paraname = attribute.getName();
                String paravalue = attribute.getValue();
//                if (paraname.equals("Name")) {
//                    recipePara.setParaName(paravalue);
//                } else if (paraname.equals("Value")) {
//                    recipePara.setSetValue(paravalue);
//                } else if (paraname.equals("Unit")) {
//                    recipePara.setParaMeasure(paravalue);
//                } else if (paraname.equals("Min")) {
//                    recipePara.setMinValue(paravalue);
//                } else if (paraname.equals("Max")) {
//                    recipePara.setMaxValue(paravalue);
//                } else if (paraname.equals("Id")) {
//                    recipePara.setParaCode(paravalue);
//                }
                vaList.add(paravalue);
            }
        }
        //如果当前节点内容不为空，则输出  
        if (!(node.getTextTrim().equals(""))) {
            System.out.println(node.getName() + " ：" + node.getText());
        }
        //同时迭代当前节点下面的所有子节点  
        //使用递归  
        Iterator<Element> iterator = node.elementIterator();
        while (iterator.hasNext()) {
            Element e = iterator.next();
            listNodes(e);
        }
        List<RecipePara> recipeParaList = new LinkedList<>();
        for (int j = 0; j < vaList.size(); j++) {
            RecipePara recipePara = new RecipePara();
            recipePara.setParaName(vaList.get(j).toString());
            recipePara.setSetValue(vaList.get(j + 1).toString());
            recipePara.setParaMeasure(vaList.get(j + 2).toString());
//            recipePara.setMinValue(vaList.get(j + 3).toString());
//            recipePara.setMaxValue(vaList.get(j + 4).toString());
            recipePara.setMinValue("");
            recipePara.setMaxValue("");
            recipePara.setParaCode(vaList.get(j + 7).toString());
            j = j + 8;
            recipeParaList.add(recipePara);
        }

        return recipeParaList;
    }
//    public void parserFicoRecipeXml(String fileName) {
//        try {
//            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
//            DocumentBuilder db = dbf.newDocumentBuilder();
//            Document document = db.parse(fileName);
//            
//            //获取根节点
//            NodeList nodeList = document.getChildNodes();
//            Node rootDoc=null;
//            if(nodeList!=null&&nodeList.getLength()>0){
//                rootDoc=nodeList.item(0);
//                for(Node attr:rootDoc.()){
//                    
//                }
//            }
//            if(rootDoc!=null){
//               Node RecipeBody =rootDoc.getFirstChild();
//                for (int i = 0; i < RecipeBody.getAttributes(); i++) {
//                    Node employee = root.item(i);
//                    NodeList employeeInfo = employee.getChildNodes();
//                    for (int j = 0; j < employeeInfo.getLength(); j++) {
//                        Node node = employeeInfo.item(j);
//                        NodeList employeeMeta = node.getChildNodes();
//                        for (int k = 0; k < employeeMeta.getLength(); k++) {
//                            System.out.println(employeeMeta.item(k).getNodeName()
//                                    + ":" + employeeMeta.item(k).getTextContent());
//                        }
//                    }
//            }
//            }
//            
//            System.out.println("解析完毕");
//        } catch (FileNotFoundException e) {
//            System.out.println(e.getMessage());
//        } catch (ParserConfigurationException e) {
//            System.out.println(e.getMessage());
//        } catch (SAXException e) {
//            System.out.println(e.getMessage());
//        } catch (IOException e) {
//            System.out.println(e.getMessage());
//        }
//    }
}