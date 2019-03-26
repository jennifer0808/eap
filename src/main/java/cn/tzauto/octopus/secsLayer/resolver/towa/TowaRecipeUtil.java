/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.secsLayer.resolver.towa;

import cn.tzauto.octopus.biz.recipe.domain.RecipeTemplate;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.secsLayer.domain.ppBodyItem.towa.TowaPPBodyItem;
import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.common.util.tool.ByteTool;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Formatter;
import java.util.List;
import java.util.Locale;
import org.apache.ibatis.session.SqlSession;

/**
 *
 * @author njtz
 */
public class TowaRecipeUtil {

    public static List<TowaPPBodyItem> Y1R_RECIPE_CONFIG = new ArrayList<>();
    public static List<TowaPPBodyItem> Y1E_RECIPE_CONFIG = new ArrayList<>();
    public static List<TowaPPBodyItem> YPM1180_RECIPE_CONFIG = new ArrayList<>();
    public static List<TowaPPBodyItem> PMC1040_RECIPE_CONFIG = new ArrayList<>();

    /**
     * 将towa的ppbody按照规则解析成为ppbodyITtem的List
     *
     * @param ppbodycfg
     * @param ppbody
     * @return
     */
    /**
     * 初始化配置
     */
    public static void init() {
        //y1e title: OFFSET, Description, SIZE, x10, x100, x1k, Units, Min, Max
        Y1E_RECIPE_CONFIG = readTowaRecipeRule(TowaConfig.Y1E_CFG);
        //y1r title:BYTE NAME(E) x100 x10 BCD UNIT
        Y1R_RECIPE_CONFIG = readTowaRecipeRule(TowaConfig.Y1R_CFG);
        // ypm1180 title:BYTE	DESCRIPTION LEN	x10	x100	x1000	BCD	ASC	FLO	UNIT
        YPM1180_RECIPE_CONFIG = readTowaRecipeRule(TowaConfig.YPM1180_CFG);
        //pmc title:BYTE DESCRIPTION LEN x10 x100 x1000	BCD ASC	FLO UNIT
        PMC1040_RECIPE_CONFIG = readTowaRecipeRule(TowaConfig.PMC_CFG);

    }

    public static List transferTowaRcp(List<TowaPPBodyItem> ppbodycfg, byte[] ppbody) {
        init();
        String result = null;
        String[] ppbodys = new String[ppbody.length];
        for (int m = 0; m < ppbody.length; m++) {
            ppbodys[m] = String.valueOf(ppbody[m]);
        }
        for (int k = 0; k < ppbodys.length; k++) {
            if (Integer.parseInt(ppbodys[k]) < 0) {
                ppbodys[k] = String.valueOf(Integer.parseInt(ppbodys[k]) + 256);
            }
        }
        for (int i = 0; i < ppbodycfg.size(); i++) {
            // 去空 
            if (ppbodycfg.get(i).getDescrtption().isEmpty() || " ".equals(ppbodycfg.get(i).getDescrtption())) {
                continue;
            }
            ArrayList tmplist = new ArrayList();
            int end = 0;
            int begin = Integer.parseInt(ppbodycfg.get(i).getByteIndex().replaceAll(" ", ""));
            int length = Integer.parseInt(ppbodycfg.get(i).getLen().replaceAll(" ", ""));
            if (i < 6) {
                //如果是第一个参数，就是解析时间，那么需要转换为double数值
                if (i == 0) {
                    byte[] bcStr = new byte[8];
                    for (int j = 0; j < 8; j++) {
                        bcStr[j] = (byte) Integer.parseInt(ppbodys[j]);
                    }
                    double dd = ByteTool.arr2double(bcStr, 0);
                    Calendar cd = Calendar.getInstance();
                    cd.set(1899, 12, 28, 00, 00, 00);
                    cd.add(Calendar.DAY_OF_MONTH, (int) dd);
                    ppbodycfg.get(i).setRealValue(dd + "");
                } else {
                    String tmpStr = "";
                    for (int j = begin; j < begin + length; j++) {
                        char tempChar = (char) (Integer.parseInt(ppbodys[j]));
                        tmpStr += tempChar;
                    }
                    ppbodycfg.get(i).setRealValue(tmpStr);
                }
            } else {
                String hx2dc = "";
                for (int j = begin; j < begin + length; j++) {
                    String tmpValue = String.valueOf(Integer.parseInt(ppbodys[j]));
                    tmplist.add(tmpValue);
                    // tmpStr += tmpValue;
                    String tmp = Integer.toHexString(Integer.parseInt(ppbodys[j]));
                    if (tmp.length() == 1) {
                        hx2dc = "0" + tmp + hx2dc;
                    } else {
                        hx2dc = tmp + hx2dc;
                    }
                }
                hx2dc = complement(hx2dc);
                if (tmplist == null) {
                    result = "NULL";
                } else {
                    if (!" ".equals(ppbodycfg.get(i).getFlo()) && ppbodycfg.get(i).getFlo() != null) {
                        result = "test";//asc2string(tmpppbodylist);
                        continue;
                    }
                    if (!" ".equals(ppbodycfg.get(i).getAsc()) && ppbodycfg.get(i).getAsc() != null) {
                        result = ByteTool.asc2string(tmplist);
                        continue;
                    }
                    if (!" ".equals(ppbodycfg.get(i).getBcd()) && ppbodycfg.get(i).getBcd() != null) {
                        result = transferBCD2DC(tmplist);// asc2string(tmpppbodylist);
                    }
                    result = hx2dc;
                    //以下三个判断都要考虑数据为何种编码，是应该转为ascii码，还是直接保留为10进制数，还是为bcd码？
                    if (!" ".equals(ppbodycfg.get(i).getX10()) && ppbodycfg.get(i).getX10() != null) {
                        result = String.valueOf(Double.parseDouble(result) / 10);
//                        result = Integer.parseInt((String) result) / 10;
                    }
                    if (!" ".equals(ppbodycfg.get(i).getX100()) && ppbodycfg.get(i).getX100() != null) {
                        result = String.valueOf(Double.parseDouble(result) / 100);
//                        result = Integer.parseInt((String) result) / 100;
                    }
                    if (!" ".equals(ppbodycfg.get(i).getX1000()) && ppbodycfg.get(i).getX1000() != null) {
                        result = String.valueOf(Double.parseDouble(result) / 1000);
//                        result = Integer.parseInt((String) result) / 1000;
                    }
                }
                if (result.equals("NULL") || result.isEmpty()) {
                    ppbodycfg.get(i).setRealValue(result);
                } else {
                    ppbodycfg.get(i).setRealValue(String.valueOf(Double.parseDouble(result)));
                }
            }
//TODO 实现存储
        }
        return transfer2Normal(ppbodycfg);
    }

    public static List transferTowaRcpFromDB(String towaType, byte[] ppbody) {

        String result = null;
        String[] ppbodys = new String[ppbody.length];
        for (int m = 0; m < ppbody.length; m++) {
            ppbodys[m] = String.valueOf(ppbody[m]);
        }
        for (int k = 0; k < ppbodys.length; k++) {
            if (Integer.parseInt(ppbodys[k]) < 0) {
                ppbodys[k] = String.valueOf(Integer.parseInt(ppbodys[k]) + 256);
            }
        }

        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        RecipeService recipeService = new RecipeService(sqlSession);
        List<RecipeTemplate> rcpTemplates = recipeService.searchRecipeTemplateByDeviceTypeCode(towaType, "RecipePara");
        sqlSession.close();
        for (int i = 0; i < rcpTemplates.size(); i++) {
            // 去空 
            if (rcpTemplates.get(i).getParaName().isEmpty() || "NULL".equals(rcpTemplates.get(i).getParaName())) {
//            if (rcpTemplates.get(i).getParaName().isEmpty() || " ".equals(rcpTemplates.get(i).getParaName()) || "".equals(rcpTemplates.get(i).getParaName())||rcpTemplates.get(i).getCountRate()==null) {
                continue;
            }
            ArrayList tmplist = new ArrayList();
            int begin = rcpTemplates.get(i).getDecodeStart();
            int length = rcpTemplates.get(i).getDecodeLength();
            if (i < 6) {
                //如果是第一个参数，就是解析时间，那么需要转换为double数值
                if (i == 0) {
                    byte[] bcStr = new byte[8];
                    for (int j = 0; j < 8; j++) {
                        bcStr[j] = (byte) Integer.parseInt(ppbodys[j]);
                    }
                    double dd = ByteTool.arr2double(bcStr, 0);
                    Calendar cd = Calendar.getInstance();
                    cd.set(1899, 12, 28, 00, 00, 00);
                    cd.add(Calendar.DAY_OF_MONTH, (int) dd);
                    rcpTemplates.get(i).setSetValue(dd + "");
                } else {
                    String tmpStr = "";
                    for (int j = begin; j < begin + length; j++) {
                        char tempChar = (char) (Integer.parseInt(ppbodys[j]));
                        tmpStr += tempChar;
                    }
                    rcpTemplates.get(i).setSetValue(tmpStr);
                }
            } else {
                String hx2dc = "";
                for (int j = begin; j < begin + length; j++) {
                    String tmpValue = String.valueOf(Integer.parseInt(ppbodys[j]));
                    tmplist.add(tmpValue);
                    // tmpStr += tmpValue;
                    String tmp = Integer.toHexString(Integer.parseInt(ppbodys[j]));
                    if (tmp.length() == 1) {
                        hx2dc = "0" + tmp + hx2dc;
                    } else {
                        hx2dc = tmp + hx2dc;
                    }
                }
                hx2dc = complement(hx2dc);
                if (tmplist == null) {
                    result = "NULL";
                } else {
                    String countRate = rcpTemplates.get(i).getCountRate();
                    if (countRate.contains("FLO")) {
                        result = "test";//asc2string(tmpppbodylist);
                        continue;
                    }
                    if (rcpTemplates.get(i).getCountRate().contains("ASC")) {
                        result = ByteTool.asc2string(tmplist);
                        continue;
                    }
                    if (rcpTemplates.get(i).getCountRate().contains("BCD")) {
                        result = transferBCD2DC(tmplist);// asc2string(tmpppbodylist);
                    }
                    result = hx2dc;
                    //以下三个判断都要考虑数据为何种编码，是应该转为ascii码，还是直接保留为10进制数，还是为bcd码？
                    int defaultCountRate = 1;
                    //取countRate的数字部分              
                    if (countRate.contains("BCD")) {
                        countRate = countRate.substring(countRate.lastIndexOf("D") + 1, countRate.length());
                        if (!"".equals(countRate) && !" ".equals(countRate)) {
                            defaultCountRate = Integer.parseInt(countRate);
                        }
                    } else {
                        if (!" ".equals(countRate) && !"".equals(countRate)) {
                            defaultCountRate = Integer.parseInt(countRate);
                        }
                    }
                    if (defaultCountRate != 1) {
                        result = String.valueOf(Double.parseDouble(result) / defaultCountRate);
                    }
                }
                if (result.equals("NULL") || result.isEmpty()) {
                    rcpTemplates.get(i).setSetValue(result);
                } else {
                    rcpTemplates.get(i).setSetValue(String.valueOf(Double.parseDouble(result)));
                }
            }
//TODO 实现存储
        }
        return transfer2Normal2(rcpTemplates);
    }

    public static List readTowaRecipeRule(String path) {
        File cfgfile = new File(path);
        List<TowaPPBodyItem> towaPPBodylist = new ArrayList<>();
        try {
            FileReader fr = new FileReader(cfgfile);
            BufferedReader br = new BufferedReader(fr);
            String cfgline = null;
            while ((cfgline = br.readLine()) != null) {
                String[] cfg = cfgline.split(",");
                TowaPPBodyItem towaPPBody = new TowaPPBodyItem();
                if (cfg.length == 7) {//FOR Y1R                  
                    towaPPBody.setByteIndex(cfg[0]);
                    towaPPBody.setDescrtption(cfg[1]);
                    towaPPBody.setLen(cfg[2]);
                    towaPPBody.setX100(cfg[3]);
                    towaPPBody.setX10(cfg[4]);
                    towaPPBody.setBcd(cfg[5]);
                    towaPPBody.setUnit(cfg[6]);
                }
                if (cfg.length == 9) {//for Y1E
                    towaPPBody.setByteIndex(cfg[0]);
                    towaPPBody.setDescrtption(cfg[1]);
                    towaPPBody.setLen(cfg[2]);
                    towaPPBody.setX10(cfg[3]);
                    towaPPBody.setX100(cfg[4]);
                    towaPPBody.setX1000(cfg[5]);
                    towaPPBody.setUnit(cfg[6]);
                    towaPPBody.setMinValue(cfg[7]);
                    towaPPBody.setMaxValue(cfg[8]);
                }
                if (cfg.length == 10) {//FOR PMC &YPM1180
                    towaPPBody.setByteIndex(cfg[0]);
                    towaPPBody.setDescrtption(cfg[1]);
                    towaPPBody.setLen(cfg[2]);
                    towaPPBody.setX10(cfg[3]);
                    towaPPBody.setX100(cfg[4]);
                    towaPPBody.setX1000(cfg[5]);
                    towaPPBody.setBcd(cfg[6]);
                    towaPPBody.setAsc(cfg[7]);
                    towaPPBody.setFlo(cfg[8]);
                    towaPPBody.setUnit(cfg[9]);
                }
                towaPPBodylist.add(towaPPBody);
            }
            br.close();
            fr.close();
        } catch (Exception e) {
            System.out.print(e);
        }
        return towaPPBodylist;
    }

    public static String transferBCD2DC(ArrayList list) {
        int result = 0;
        for (int i = 0; i < list.size(); i++) {
            int tmp = Integer.parseInt((String) list.get(i));
            if (tmp < 16) {
                result = result + tmp;
            } else {
                String hxString = Integer.toHexString(tmp);
                String firstsString = hxString.substring(0, 1);
                String secondsString = hxString.substring(1, 2);
                int firstI = Integer.parseInt(firstsString, 16) * 2;
                int secondI = Integer.parseInt(secondsString, 16);
                int value = firstI + secondI;
                result = result + value;
            }
        }
        return String.valueOf(result);
    }

//取补码 参数为16进制数
    public static String complement(String hxstr) {
//        int =Integer.parseInt(str, 10);
        if (!"f".equals(hxstr.substring(0, 1))) {
            hxstr = String.valueOf(Long.parseLong(hxstr, 16));
            return hxstr;
        } else {
            long tmplong = Long.parseLong(hxstr, 16);
            String tmpString = Long.toBinaryString(tmplong);
            String tmp = new String();
            for (int i = 0; i < tmpString.length(); i++) {
                if (i == 0) {
                    continue;
                } else {
                    if ("0".equals(String.valueOf(tmpString.charAt(i)))) {
                        tmp = tmp + 1;
                    } else {
                        tmp = tmp + 0;
                    }
                }
            }
            tmplong = Long.parseLong(String.valueOf(tmp), 2);
            tmplong = tmplong + 1;
            return String.valueOf(-tmplong);
        }
    }

    private static String getDateStr(long millis) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(millis);
        Formatter ft = new Formatter(Locale.CHINA);
        return ft.format("%1$tY年%1$tm月%1$td日%1$tA，%1$tT %1$tp", cal).toString();
    }

    private static List transfer2Normal(List<TowaPPBodyItem> ppbodycfgList) {
        List<RecipePara> recipeParaList = new ArrayList<>();

        for (int i = 0; i < ppbodycfgList.size(); i++) {
            RecipePara recipePara = new RecipePara();
            recipePara.setParaCode(String.valueOf(i + 1));
            recipePara.setParaName(ppbodycfgList.get(i).getDescrtption());
            recipePara.setSetValue(ppbodycfgList.get(i).getRealValue());
            recipePara.setMinValue(ppbodycfgList.get(i).getMinValue());
            recipePara.setMaxValue(ppbodycfgList.get(i).getMaxValue());
            recipePara.setParaMeasure(ppbodycfgList.get(i).getUnit());
            recipeParaList.add(recipePara);
        }
        return recipeParaList;
    }

    private static List transfer2Normal2(List<RecipeTemplate> rcpTemplates) {
        List<RecipePara> recipeParaList = new ArrayList<>();
        for (int i = 0; i < rcpTemplates.size(); i++) {
            RecipePara recipePara = new RecipePara();
            recipePara.setParaCode(String.valueOf(i + 1));
            recipePara.setParaName(rcpTemplates.get(i).getParaName());
            recipePara.setParaShotName(rcpTemplates.get(i).getParaShotName());
            recipePara.setSetValue(rcpTemplates.get(i).getSetValue());
            recipePara.setParaMeasure(rcpTemplates.get(i).getParaUnit());
            recipeParaList.add(recipePara);
        }
        return recipeParaList;
    }
    public static void main(String args[]) {
    }
}
