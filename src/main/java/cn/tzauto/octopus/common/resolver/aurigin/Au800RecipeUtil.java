/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.common.resolver.aurigin;

import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.biz.recipe.domain.RecipeTemplate;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.common.resolver.IOUtil;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class Au800RecipeUtil {

    private static final Logger logger = Logger.getLogger(Au800RecipeUtil.class);

    public static List transferAu800Rcp(String recipePath, String deviceType) {
        FileReader fr = null;
        BufferedReader bfr = null;
        OutputStream os = null;

        File file = new File(recipePath);
        List<RecipePara> recipeParas = new ArrayList<>();
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        RecipeService recipeService = new RecipeService(sqlSession);
        List<RecipeTemplate> recipeTemplates = recipeService.searchRecipeTemplateByDeviceTypeCode(deviceType, "RecipePara");
        int lines = 0;
        try {
            fr = new FileReader(file);
            bfr = new BufferedReader(fr);
            String ss = null;
            String para = null;

            int[] lineNumber = {1257, 1258, 1259, 1260, 1423, 1424, 1425, 1426, 1597, 1598, 1599, 1600, 2405, 2406, 2407, 2408, 2597, 2598, 2599, 2600};
//            for (RecipeTemplate recipeTemp : recipeTemplates) {
            for (int j = 0; j < recipeTemplates.size(); j++) {
                ok:
                while ((ss = bfr.readLine()) != null) {
                    lines++;
                    RecipePara recipePara = new RecipePara();
                    if (ss.contains(recipeTemplates.get(j).getParaName())) {

                        System.out.println("==========lines" + lines);
                        if (ss.contains("href")) {
                            para = ss.substring(ss.indexOf("\"") + 1, ss.lastIndexOf("\""));
                        } else {
                            String[] str1 = ss.split(">");
                            String[] str2 = str1[1].split("<");
                            para = str2[0];
                        }
                        recipePara.setParaCode(recipeTemplates.get(j).getParaCode());
                        recipePara.setParaDesc(recipeTemplates.get(j).getParaDesc());
                        recipePara.setParaMeasure(recipeTemplates.get(j).getParaUnit());
                        recipePara.setParaName(recipeTemplates.get(j).getParaName());
                        recipePara.setParaShotName(recipeTemplates.get(j).getParaShotName());
                        recipePara.setSetValue(para);
                        recipeParas.add(recipePara);
                        break;
                    } else {
                        for (int i : lineNumber) {
                            if (lines == i) {
                                if (ss.contains("href")) {
                                    para = ss.substring(ss.indexOf("\"") + 1, ss.lastIndexOf("\""));
                                } else {
                                    String[] str1 = ss.split(">");
                                    String[] str2 = str1[1].split("<");
                                    para = str2[0];
                                }
                                recipePara.setParaCode(recipeTemplates.get(j).getParaCode());
                                recipePara.setParaDesc(recipeTemplates.get(j).getParaDesc());
                                recipePara.setParaMeasure(recipeTemplates.get(j).getParaUnit());
                                recipePara.setParaName(recipeTemplates.get(j).getParaName());
                                recipePara.setParaShotName(recipeTemplates.get(j).getParaShotName());
                                recipePara.setSetValue(para);
                                recipeParas.add(recipePara);
                                break ok;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Exception:", e);
        } finally {
            IOUtil.closeQuietly(os, bfr, fr);
        }
        sqlSession.close();
        return recipeParas;
    }

    public static void main(String[] args) {
        List<RecipePara> recipeParas = transferAuriginRcp("D:\\TFBGAtestrecipezhangsen83873-TEST.xml_V0", "AURIGINAU850Z2");
        System.out.println(recipeParas.size());
        for (RecipePara para : recipeParas) {
            System.out.println(para.getParaName() + "   " + para.getSetValue());
        }
    }

    public static List<RecipePara> transferAuriginRcp(String recipePath, String deviceType) {
        List<RecipePara> recipeParas = new ArrayList<>();
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        RecipeService recipeService = new RecipeService(sqlSession);
        List<RecipeTemplate> recipeTemplates = recipeService.searchRecipeTemplateByDeviceTypeCode(deviceType, "RecipePara");
        sqlSession.close();
        Map map = new HashMap();
        map = decodeRcp(recipePath);
        for (RecipeTemplate recipeTemplate : recipeTemplates) {
            RecipePara recipePara = new RecipePara();
            recipePara.setParaCode(recipeTemplate.getParaCode());
            recipePara.setParaDesc(recipeTemplate.getParaDesc());
            recipePara.setParaMeasure(recipeTemplate.getParaUnit());
            recipePara.setParaName(recipeTemplate.getParaName());
            recipePara.setParaShotName(recipeTemplate.getParaShotName());
            recipePara.setSetValue(String.valueOf(map.get(recipeTemplate.getParaName())));
            recipeParas.add(recipePara);
        }
        return recipeParas;
    }

    public static String FluxPickDwell[] = {"FluxDipDwell"};
    public static String FluxPlaceDwell[] = {"FluxAssemblyDwell"};
    public static String BallPickDwell[] = {"BallPickDwell"};
    public static String BallPlaceDwell[] = {"BallAssemblyDwell"};

    public static String BallPickXOffset[] = {"BallPickOffset", "_axisPoint", "1"};
    public static String BallPickYGlobal[] = {"BallPlatformFlat", "_axisPoint", "1"};
    public static String BallPickZOffset[] = {"BallPickOffset", "_axisPoint", "2"};
    public static String BallPickRXGlobal[] = {"BallPlatformFlat", "_axisPoint", "2"};

    public static String FluxPlaceXOffsetStep1[] = {"FluxAssemblyVisionOffsets", "1", "_axisPoint", "1"};
    public static String FluxPlaceXOffsetStep2[] = {"FluxAssemblyVisionOffsets", "2", "_axisPoint", "1"};
    public static String FluxPlaceYOffsetStep1[] = {"FluxAssemblyVisionOffsets", "1", "_axisPoint", "2"};
    public static String FluxPlaceYOffsetStep2[] = {"FluxAssemblyVisionOffsets", "2", "_axisPoint", "2"};
    public static String FluxPlaceZOffsetStep1[] = {"FluxAssemblyVisionOffsets", "1", "_axisPoint", "3"};
    public static String FluxPlaceZOffsetStep2[] = {"FluxAssemblyVisionOffsets", "2", "_axisPoint", "3"};
    public static String FluxPlaceRZOffsetStep1[] = {"FluxAssemblyVisionOffsets", "1", "_axisPoint", "4"};
    public static String FluxPlaceRZOffsetStep2[] = {"FluxAssemblyVisionOffsets", "2", "_axisPoint", "4"};

    public static String BallPlaceXOffsetStep1[] = {"BallAssemblyVisionOffsets", "1", "_axisPoint", "1"};
    public static String BallPlaceXOffsetStep2[] = {"BallAssemblyVisionOffsets", "2", "_axisPoint", "1"};
    public static String BallPlaceYOffsetStep1[] = {"BallAssemblyVisionOffsets", "1", "_axisPoint", "2"};
    public static String BallPlaceYOffsetStep2[] = {"BallAssemblyVisionOffsets", "2", "_axisPoint", "2"};
    public static String BallPlaceZOffsetStep1[] = {"BallAssemblyVisionOffsets", "1", "_axisPoint", "3"};
    public static String BallPlaceZOffsetStep2[] = {"BallAssemblyVisionOffsets", "2", "_axisPoint", "3"};
    public static String BallPlaceRZOffsetStep1[] = {"BallAssemblyVisionOffsets", "1", "_axisPoint", "4"};
    public static String BallPlaceRZOffsetStep2[] = {"BallAssemblyVisionOffsets", "2", "_axisPoint", "4"};

    public static String FluxPickZOffset[] = {"FluxDipOffset", "_axisPoint", "2"};

    public static Map<String, String[]> paraConfMap = new HashMap<String, String[]>();

    public static void init() {
        paraConfMap.put("FluxPickDwell" + "_Conf", FluxPickDwell);
        paraConfMap.put("FluxPlaceDwell" + "_Conf", FluxPlaceDwell);
        paraConfMap.put("BallPickDwell" + "_Conf", BallPickDwell);
        paraConfMap.put("BallPlaceDwell" + "_Conf", BallPlaceDwell);

        paraConfMap.put("BallPickXOffset" + "_Conf", BallPickXOffset);
        paraConfMap.put("BallPickYGlobal" + "_Conf", BallPickYGlobal);
        paraConfMap.put("BallPickZOffset" + "_Conf", BallPickZOffset);
        paraConfMap.put("BallPickRXGlobal" + "_Conf", BallPickRXGlobal);

        paraConfMap.put("FluxPlaceXOffsetStep1" + "_Conf", FluxPlaceXOffsetStep1);
        paraConfMap.put("FluxPlaceXOffsetStep2" + "_Conf", FluxPlaceXOffsetStep2);
        paraConfMap.put("FluxPlaceYOffsetStep1" + "_Conf", FluxPlaceYOffsetStep1);
        paraConfMap.put("FluxPlaceYOffsetStep2" + "_Conf", FluxPlaceYOffsetStep2);
        paraConfMap.put("FluxPlaceZOffsetStep1" + "_Conf", FluxPlaceZOffsetStep1);
        paraConfMap.put("FluxPlaceZOffsetStep2" + "_Conf", FluxPlaceZOffsetStep2);
        paraConfMap.put("FluxPlaceRZOffsetStep1" + "_Conf", FluxPlaceRZOffsetStep1);
        paraConfMap.put("FluxPlaceRZOffsetStep2" + "_Conf", FluxPlaceRZOffsetStep2);

        paraConfMap.put("BallPlaceXOffsetStep1" + "_Conf", BallPlaceXOffsetStep1);
        paraConfMap.put("BallPlaceXOffsetStep2" + "_Conf", BallPlaceXOffsetStep2);
        paraConfMap.put("BallPlaceYOffsetStep1" + "_Conf", BallPlaceYOffsetStep1);
        paraConfMap.put("BallPlaceYOffsetStep2" + "_Conf", BallPlaceYOffsetStep2);
        paraConfMap.put("BallPlaceZOffsetStep1" + "_Conf", BallPlaceZOffsetStep1);
        paraConfMap.put("BallPlaceZOffsetStep2" + "_Conf", BallPlaceZOffsetStep2);
        paraConfMap.put("BallPlaceRZOffsetStep1" + "_Conf", BallPlaceRZOffsetStep1);
        paraConfMap.put("BallPlaceRZOffsetStep2" + "_Conf", BallPlaceRZOffsetStep2);

        paraConfMap.put("FluxPickZOffset" + "_Conf", FluxPickZOffset);

    }

    public static Map decodeRcp(String fileName) {
        init();
        Map paraValueMap = new HashMap();
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document document = db.parse(new InputSource(fileName));
            Element root = document.getDocumentElement();

            Map recipeAuData = new HashMap();
            Map a2Locations = new HashMap();
            Map SoapArrays = new HashMap();

            NodeList recipeDataNode = root.getElementsByTagName("a1:Recipe_x002B_AuData");
            NodeList locationNode = root.getElementsByTagName("a2:Location");
            NodeList arrayNode = root.getElementsByTagName("SOAP-ENC:Array");

            //遍历recipeDataNode并保存
            if (recipeDataNode.getLength() > 0) {
                Node dataNode = recipeDataNode.item(0);
                NodeList childNodes = dataNode.getChildNodes();
                if (childNodes.getLength() > 0) {
                    for (int i = 0; i < childNodes.getLength(); i++) {
                        Node recipeNode = childNodes.item(i);
                        if (!"".equals(recipeNode.getTextContent().trim())) {
                            recipeAuData.put(recipeNode.getNodeName(), recipeNode.getTextContent());
                        } else {
                            if (!recipeNode.getNodeName().equals("\n") && !recipeNode.getTextContent().equals("\n")) {
                                if (recipeNode.getAttributes().getNamedItem("href") != null) {
                                    recipeAuData.put(recipeNode.getNodeName(), recipeNode.getAttributes().getNamedItem("href").getNodeValue().replace("#", ""));
                                } else {
                                    recipeAuData.put(recipeNode.getNodeName(), recipeNode.getAttributes().getNamedItem("id").getNodeValue().replace("#", ""));
                                }

                            }
                        }
                    }
                }

                //处理Location
                if (locationNode.getLength() > 0) {
                    for (int i = 0; i < locationNode.getLength(); i++) {
                        Node locationDataNode = locationNode.item(i);
                        String refid = locationDataNode.getAttributes().getNamedItem("id").getNodeValue();

                        NodeList childList = locationDataNode.getChildNodes();
                        String _axisPoint = "-1";
                        for (int j = 0; j < childList.getLength(); j++) {
                            if (childList.item(j).getNodeName().equals("_axisPoint")) {
                                _axisPoint = childList.item(j).getAttributes().getNamedItem("href").getNodeValue().replace("#", "");
                                break;
                            }
                        }
                        a2Locations.put(refid, _axisPoint);
                    }
                }

                //处理Array
                if (arrayNode.getLength() > 0) {
                    for (int i = 0; i < arrayNode.getLength(); i++) {
                        List itemList = new ArrayList();
                        Node arrayDataNode = arrayNode.item(i);
                        String refid = arrayDataNode.getAttributes().getNamedItem("id").getNodeValue();

                        NodeList childList = arrayDataNode.getChildNodes();
                        for (int j = 0; j < childList.getLength(); j++) {
                            if (childList.item(j).getNodeName().equals("item")) {
                                Node arrNode = childList.item(j);
                                String itemId = "";
//                                System.out.println(refid+"===="+j+"====="+arrNode.getTextContent()+"===="+arrNode.getAttributes().getLength());
                                if ("".equals(arrNode.getTextContent())) {
                                    itemId = arrNode.getAttributes().getNamedItem("href").getNodeValue().replace("#", "");
                                } else {
//                                    System.out.println(refid+"===="+j+"====="+arrNode.getTextContent()+arrNode.getAttributes().getLength());
                                    if (arrNode.getAttributes().getLength() == 0) {
                                        itemId = arrNode.getTextContent();
//                                        System.out.println(refid+"===="+j+"====="+itemId);
                                    } else {
                                        itemId = arrNode.getAttributes().getNamedItem("id").getNodeValue().replace("#", "");
                                    }
                                }
//                                System.out.println(refid + "----" + itemId);
                                itemList.add(itemId);
                            }
                        }
                        SoapArrays.put(refid, itemList);
                    }
                }

            }

            if (recipeDataNode.getLength() == 0 || locationNode.getLength() == 0 || arrayNode.getLength() == 0) {
                logger.error("获取到数据异常，不能正确解析参数");
            }

            //循环获取所需要的Recipe值，然后放入Map中
            for (Map.Entry<String, String[]> entry : paraConfMap.entrySet()) {
                String paraName = entry.getKey();
                String[] configParaChain = entry.getValue();
                String netxValue = "";
                if (configParaChain.length >= 1) {
                    String topValue = String.valueOf(recipeAuData.get(configParaChain[0]));
                    netxValue = topValue;
                    for (int i = 1; i < configParaChain.length; i++) {
                        if (configParaChain[i].matches("[0-9]+")) {
                            netxValue = ((ArrayList) SoapArrays.get(netxValue)).get(Integer.parseInt(configParaChain[i]) - 1).toString();
                        } else {
                            netxValue = a2Locations.get(netxValue).toString();
                        }
                    }
//                       System.out.println(paraName.split("_")[0]);
//                    System.out.println(paraName.split("_")[0] + "::" + netxValue);
                    paraValueMap.put(paraName.split("_")[0], netxValue);
                } else {
                    System.out.println("Paraconf is incorrect!");
                }
            }
            return paraValueMap;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

    }

    public static List transferAu800Rcp(String recipePath) {
        FileReader fr = null;
        BufferedReader bfr = null;
        OutputStream os = null;

        File file = new File(recipePath);
        List<RecipePara> recipeParas = new ArrayList<>();
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        RecipeService recipeService = new RecipeService(sqlSession);
        List<RecipeTemplate> recipeTemplates = recipeService.searchRecipeTemplateByDeviceTypeCode("AU800", "RecipePara");
        try {
            fr = new FileReader(file);
            bfr = new BufferedReader(fr);
            String ss = null;
            String para = null;
            for (RecipeTemplate recipeTemp : recipeTemplates) {
                while ((ss = bfr.readLine()) != null) {
                    RecipePara recipePara = new RecipePara();
                    if (ss.contains(recipeTemp.getParaName())) {
                        if (ss.contains("href")) {
                            para = ss.substring(ss.indexOf("\"") + 1, ss.lastIndexOf("\""));
                        } else {
                            String[] str1 = ss.split(">");
                            String[] str2 = str1[1].split("<");
                            para = str2[0];
                        }

                        recipePara.setParaCode(recipeTemp.getParaCode());
                        recipePara.setParaDesc(recipeTemp.getParaDesc());
                        recipePara.setParaMeasure(recipeTemp.getParaUnit());
                        recipePara.setParaName(recipeTemp.getParaName());
                        recipePara.setParaShotName(recipeTemp.getParaShotName());
                        recipePara.setSetValue(para);
                        recipeParas.add(recipePara);
                        break;
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Exception:", e);
        } finally {
            IOUtil.closeQuietly(os, bfr, fr);
        }
        sqlSession.close();
        return recipeParas;
    }
}
