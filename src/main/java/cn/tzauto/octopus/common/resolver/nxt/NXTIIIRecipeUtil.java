/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.common.resolver.nxt;

import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.biz.recipe.domain.RecipeTemplate;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.ibatis.session.SqlSession;

public class NXTIIIRecipeUtil {

    //filePath 是PPBODY原文件的存储路径(非文件夹)
    public static Map transferFromFile(String filePath) {
        Map map = new LinkedHashMap();
        BufferedReader br = null;
        try {
            String cfgline = null;
            String key = "";
            String value = "";

            File cfgfile = new File(filePath);
            br = new BufferedReader(new InputStreamReader(new FileInputStream(cfgfile), "GBK"));
            while ((cfgline = br.readLine()) != null) {
                StringBuilder trimmedStr = new StringBuilder();
                if (cfgline.contains("JobModLog.txt+")) {
                    break;
                }
                cfgline = cfgline.replaceAll("@", "");
                for (int i = 0; i < cfgline.length(); i++) {
                    char c = cfgline.charAt(i);
                    // 当从Unicode编码向某个字符集转换时，如果在该字符集中没有对应的编码，则得到0x3f（即问号字符?）
                    //从其他字符集向Unicode编码转换时，如果这个二进制数在该字符集中没有标识任何的字符，则得到的结果是0xfffd
                    //System.out.println("--- " + (int) c);
                    if (Integer.valueOf(c) < 127 && Integer.valueOf(c) > 32) {
//                    if ((int) c == 0xfffd) {
                        // 存在乱码
//                        System.out.println("存在乱码 " + (int) c);
                        trimmedStr.append(c);
                    } else {
                        trimmedStr.append(" ");
                    }
                }
//                System.out.println("整理前:" + cfgline);
                cfgline = trimmedStr.toString();
                while (cfgline.contains("  ")) {
                    cfgline = cfgline.replaceAll("  ", " ");
                }
//                System.out.println("整理后:" + cfgline);
//                System.out.println("整理后:" + revert(cfgline));

//                if (key.contains("[")) { //去除[]
//                    key = key.substring(0, key.indexOf("["));
//                }
                String[] cfg2 = cfgline.split(" ");
                for (int i = 0; i < cfg2.length - 1; i++) {
                    String string = cfg2[i];
                    if (string.contains("(")) {
                        String[] strTmps = string.split("\\(");
                        if (strTmps.length > 1) {

                            for (int j = 0; j < strTmps.length - 1; j++) {
                                System.out.println("para：" + strTmps[j]);
                                System.out.println("value：" + strTmps[j + 1]);
                                j++;
                            }
                        }
                    }
//                    System.out.println("" + cfg2[i]);
                    map.put(cfg2[i], cfg2[i + 1]);
//                    System.out.println("para：" + cfg2[i]);
//                    System.out.println("value：" + cfg2[i + 1]);
                    i++;
                }
//                value = cfg2[0];
//                if (value.contains("{")) {  //去除{}
//                    value = value.substring(value.indexOf("{") + 1, value.indexOf("}"));
//                }
//                if (value.contains("\"")) { //去除引号
//                    value = value.replaceAll("\"", "");
//                }
//                key = key.replaceAll(" ", ""); //去除空格
//                value = value.replaceAll(" ", "");
//                if (value.contains(",")) {
//                    String[] values = value.split(",");
//                    String keyTemp = "";
//                    //如果参数值为数组，参数名后面加数字并分别列出
//                    for (int j = 0; j < values.length; j++) {
////                            if (j == 0) { //数组第一个的参数名不带数字
////                                map.put(key, values[j]);
////                            } else {
//                        keyTemp = key + String.valueOf(j + 1);
//                        map.put(keyTemp, values[j]);
////                            }
//
//                    }
//                } else {
//                    map.put(key, value);
//                }
//
            }
            br.close();
            return map;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static List transferFromDB(Map paraMap, String deviceType) {
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        RecipeService recipeService = new RecipeService(sqlSession);
        List<RecipeTemplate> recipeTemplates = recipeService.searchRecipeTemplateByDeviceTypeCode(deviceType, "RecipePara");
        sqlSession.close();
        List<String> paraNameList = new ArrayList<>();
        for (int i = 0; i < recipeTemplates.size(); i++) {
            paraNameList.add(recipeTemplates.get(i).getParaName());
        }
        List<RecipePara> recipeParaList = new ArrayList<>();
        Set<Map.Entry<String, String>> entry = paraMap.entrySet();
        for (Map.Entry<String, String> e : entry) {
            if (paraNameList.contains(e.getKey())) {
                RecipePara recipePara = new RecipePara();
                recipePara.setParaCode(recipeTemplates.get(paraNameList.indexOf(e.getKey())).getParaCode());
                recipePara.setParaName(recipeTemplates.get(paraNameList.indexOf(e.getKey())).getParaName());
                recipePara.setParaShotName(recipeTemplates.get(paraNameList.indexOf(e.getKey())).getParaShotName());
                recipePara.setSetValue(e.getValue());
                recipePara.setMinValue(recipeTemplates.get(paraNameList.indexOf(e.getKey())).getMinValue());
                recipePara.setMaxValue(recipeTemplates.get(paraNameList.indexOf(e.getKey())).getMaxValue());
                recipePara.setParaMeasure(recipeTemplates.get(paraNameList.indexOf(e.getKey())).getParaUnit());
                recipePara.setParaShotName(recipeTemplates.get(paraNameList.indexOf(e.getKey())).getParaShotName());
                recipeParaList.add(recipePara);
            }
        }
        return recipeParaList;
    }

    public static void main(String[] args) throws UnsupportedEncodingException, FileNotFoundException, IOException {
        //8-260PG-200-180-D175-OS10  8-260PG-200-180-D175-OS10TEST   8-575B-200-90-F125E   8-E8180-75P-0-9011-20   DISCO-AP-8-thin   IC-8-575B-90P-0-DU-2385KS
//        Map map = transferFromFile("D:\\MT6580DOE.txt");//MCXT-010.txt   AMIT-552
//        Set<Map.Entry<String, String>> entry = map.entrySet();
//        for (Map.Entry<String, String> e : entry) {
//            System.out.println(e.getKey() + "——" + e.getValue());
//        }
//        System.out.println(entry.size());
//        List<RecipePara> list = transferFromDB(map, "DISCODFL7161");
//        System.out.println(list.size());
//        Map paraMap = DiscoRecipeUtil.transferFromFile("D:\\RECIPE\\AKJ@12009.txt");
        Map paraMap = NXTIIIRecipeUtil.transferFromFile("D:\\RECIPE\\FCGBA(1515-0.8)289P-SIP-ZJ26-D271~G00~2.JOB");

        List<RecipePara> list = NXTIIIRecipeUtil.transferFromDB(paraMap, "NXTIIIZ2");
        for (int i = 0; i < list.size(); i++) {
            System.out.println(list.get(i).getParaCode() + "=====" + list.get(i).getParaName() + "=====" + list.get(i).getSetValue());
        }
//        String path = "D:\\RECIPE\\FCGBA(1515-0.8)289P-SIP-ZJ26-D271~G00~2.JOB";
//        File file = null;
//        BufferedReader br = null;
//        file = new File(path);
//        br = new BufferedReader(new InputStreamReader(new FileInputStream(file), "gbk"));
//        StringBuilder sb = new StringBuilder();
//        String length = "";
//        while ((length = br.readLine()) != null) {
//            sb.append(length);
//        }
//        System.out.println(ascii2Native(sb.toString()));
    }

    public static String getEquipRecipeName(String filePath) {
        String equipRecipeName = "";
        BufferedReader br = null;
        try {
            String cfgline = null;
            File cfgfile = new File(filePath);
            br = new BufferedReader(new InputStreamReader(new FileInputStream(cfgfile), "UTF-8"));
            while ((cfgline = br.readLine()) != null) {
                equipRecipeName = cfgline.split(",")[0];
            }
            br.close();
        } catch (Exception e) {
            return null;
        }
        return equipRecipeName;
    }
    //1)字符串转unicode 

    /**
     * 将字符串转成unicode
     *
     * @param str 待转字符串
     * @return unicode字符串
     */
    public String convert(String str) {
        str = (str == null ? "" : str);
        String tmp;
        StringBuffer sb = new StringBuffer(1000);
        char c;
        int i, j;
        sb.setLength(0);
        for (i = 0; i < str.length(); i++) {
            c = str.charAt(i);
            sb.append("\\u");
            j = (c >>> 8); //取出高8位 
            tmp = Integer.toHexString(j);
            if (tmp.length() == 1) {
                sb.append("0");
            }
            sb.append(tmp);
            j = (c & 0xFF); //取出低8位 
            tmp = Integer.toHexString(j);
            if (tmp.length() == 1) {
                sb.append("0");
            }
            sb.append(tmp);

        }
        return (new String(sb));
    }

//    2)unicode转成字符串
//    ，与上述过程反向操作即可 
    /**
     * 将unicode 字符串
     *
     * @param str 待转字符串
     * @return 普通字符串
     */
    public static String revert(String str) {
        str = (str == null ? "" : str);
        if (!str.contains("\\u"))//如果不是unicode码则原样返回 
        {
            return str;
        }

        StringBuilder sb = new StringBuilder(1000);

        for (int i = 0; i < str.length() - 6;) {
            String strTemp = str.substring(i, i + 6);
            String value = strTemp.substring(2);
            int c = 0;
            for (int j = 0; j < value.length(); j++) {
                char tempChar = value.charAt(j);
                int t = 0;
                switch (tempChar) {
                    case 'a':
                        t = 10;
                        break;
                    case 'b':
                        t = 11;
                        break;
                    case 'c':
                        t = 12;
                        break;
                    case 'd':
                        t = 13;
                        break;
                    case 'e':
                        t = 14;
                        break;
                    case 'f':
                        t = 15;
                        break;
                    default:
                        t = tempChar - 48;
                        break;
                }

                c += t * ((int) Math.pow(16, (value.length() - j - 1)));
            }
            sb.append((char) c);
            i = i + 6;
        }
        return sb.toString();
    }

//unicode转为本地
    public static String ascii2Native(String str) {
        StringBuilder sb = new StringBuilder();
        int begin = 0;
        int index = str.indexOf("\\u");
        while (index != -1) {
            sb.append(str.substring(begin, index));
            sb.append(ascii2Char(str.substring(index, index + 6)));
            begin = index + 6;
            index = str.indexOf("\\u", begin);
        }
        sb.append(str.substring(begin));
        return sb.toString();
    }

    private static char ascii2Char(String str) {
        if (str.length() != 6) {
            throw new IllegalArgumentException(
                    "Ascii string of a native character must be 6 character.");
        }
        if (!"\\u".equals(str.substring(0, 2))) {
            throw new IllegalArgumentException(
                    "Ascii string of a native character must start with \"\\u\".");
        }
        String tmp = str.substring(2, 4);
        int code = Integer.parseInt(tmp, 16) << 8;
        tmp = str.substring(4, 6);
        code += Integer.parseInt(tmp, 16);
        return (char) code;
    }
}
