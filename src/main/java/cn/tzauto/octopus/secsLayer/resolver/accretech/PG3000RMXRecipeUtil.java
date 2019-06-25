/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.secsLayer.resolver.accretech;

import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.biz.recipe.domain.RecipeTemplate;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;

import java.io.InputStreamReader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.session.SqlSession;

import org.apache.log4j.Logger;

/**
 * @author njtz
 */
public class PG3000RMXRecipeUtil {

    private static Logger logger = Logger.getLogger(PG3000RMXRecipeUtil.class);

    public static byte[] trans(String recipepath) {
        byte[] buffer = null;
        try {
            File file = new File(recipepath);
            FileInputStream fis = new FileInputStream(file);
            ByteArrayOutputStream bos = new ByteArrayOutputStream(1000);
            byte[] b = new byte[1000];
            int n;
            while ((n = fis.read(b)) != -1) {
                bos.write(b, 0, n);
            }
            fis.close();
            bos.close();
            buffer = bos.toByteArray();
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        return buffer;
    }
    //将4个值拼成一个新的8位16进制数

    public static String transferOx(String num) {
        String[] nums = num.split(",");
        String oxStr = "";
        String numOx1 = Integer.toHexString(Integer.parseInt(nums[0]));
        String numOx2 = Integer.toHexString(Integer.parseInt(nums[1]));
        String numOx3 = Integer.toHexString(Integer.parseInt(nums[2]));
        String numOx4 = Integer.toHexString(Integer.parseInt(nums[3]));
        String oxStr1 = "";

        if (Integer.parseInt(nums[0]) < 0) {
            oxStr1 = numOx1.substring(6);
        } else if (Integer.parseInt(nums[0]) >= 0 && Integer.parseInt(nums[0]) < 16) {
            oxStr1 = "0" + numOx1;
        } else {
            oxStr1 = numOx1;
        }

        String oxStr2 = "";
        if (Integer.parseInt(nums[1]) < 0) {
            oxStr2 = numOx2.substring(6, numOx2.length());
        } else if (Integer.parseInt(nums[1]) >= 0 && Integer.parseInt(nums[1]) < 16) {
            oxStr2 = "0" + numOx2;
        } else {
            oxStr2 = numOx2;
        }

        String oxStr3 = "";
        if (Integer.parseInt(nums[2]) < 0) {
            oxStr3 = numOx3.substring(6, numOx3.length());
        } else if (Integer.parseInt(nums[2]) > 0 && Integer.parseInt(nums[2]) < 16) {
            oxStr3 = "0" + numOx3;
        } else {
            oxStr3 = numOx3;
        }

        String oxStr4 = "";
        if (Integer.parseInt(nums[3]) < 0) {
            oxStr4 = numOx4.substring(6, numOx4.length());
        } else if (Integer.parseInt(nums[3]) >= 0 && Integer.parseInt(nums[3]) < 16) {
            oxStr4 = "0" + numOx4;
        } else {
            oxStr4 = numOx4;
        }
        oxStr = oxStr1 + oxStr2 + oxStr3 + oxStr4;
        String s = getDecimalPoint(oxStr);
        return s;
    }
    //将16进制数转换成10进制数

    public static String getDecimalPoint(String oxStr) {
        String numString = "";
        String oxString = oxStr.substring(0, 1);// 1
        String oxStringBack = oxStr.substring(1, oxStr.length());// 0010400

        String oxStrGetChange = getBinary(oxString); // 0000
        String oxStrBinaryFirst = oxStrGetChange.substring(0, 1);// 0
        String oxStrBinaryBack = oxStrGetChange.substring(1, oxStrGetChange.length());// 001
        int i = Integer.parseInt(Integer.valueOf(oxStrBinaryBack, 2).toString());
        Double oxString2 = Double.valueOf(oxStringBack) / Math.pow(10, i);
        String oxString3 = Double.toString(oxString2);

        if ("0".equals(oxStrBinaryFirst)) {
            if (oxString2 > 1) {
                numString = oxString3.replaceFirst("0*", "");
            } else {
                numString = oxString2 + "";
            }
        } else {
            if (oxString2 < -1) {
                numString = "-" + oxString3.replaceFirst("0*", "");
            } else {
                numString = "-" + oxString2 + "";
            }
        }
        return numString;
    }

    public static String getBinary(String oxString) {
        String num = "";
        if ("0".equals(oxString)) {
            num = "0000";
        } else if ("1".equals(oxString)) {
            num = "0001";
        } else if ("2".equals(oxString)) {
            num = "0010";
        } else if ("3".equals(oxString)) {
            num = "0011";
        } else if ("4".equals(oxString)) {
            num = "0101";
        } else if ("6".equals(oxString)) {
            num = "0110";
        } else if ("7".equals(oxString)) {
            num = "0111";
        } else if ("8".equals(oxString)) {
            num = "1000";
        } else if ("9".equals(oxString)) {
            num = "1001";
        } else if ("a".equals(oxString)) {
            num = "1010";
        } else if ("b".equals(oxString)) {
            num = "1011";
        } else if ("c".equals(oxString)) {
            num = "1100";
        } else if ("d".equals(oxString)) {
            num = "1101";
        } else if ("e".equals(oxString)) {
            num = "1110";
        } else if ("f".equals(oxString)) {
            num = "1111";
        }
        return num;
    }
    //double型转成string型

    public static String mathChange(String math) {
        Double d = Double.valueOf(math).doubleValue();
        int nums = (new Double(d)).intValue();
        String numss = nums + "";
        return numss;
    }

    // filePath 是PPBODY原文件的存储路径,savedFilePath为解析后的存储路径
    public static List<RecipePara> pg300RecipeTran(String filePath) {
        List<RecipePara> list = new ArrayList<>();
//        WritableWorkbook wwb = null;
        BufferedReader br = null;
        try {
            File cfgfile = new File(filePath);
            br = new BufferedReader(new InputStreamReader(new FileInputStream(cfgfile), "GBK"));
            // 获取Btye数组
            String xx = cfgfile.getAbsolutePath();
            byte[] b = trans(xx);
            for (int i = 0; i < b.length; i += 4) {
                RecipePara rp = new RecipePara();
                String s = null;
                if (i == 0) {
                    rp.setParaCode("1");
                    rp.setParaName("GR1 WHEEL SPEED");
                    rp.setSetValue(mathChange(transferOx(b[i] + "," + b[i + 1] + "," + b[i + 2] + "," + b[i + 3])));
                    rp.setParaMeasure("rpm");
                    rp.setMinValue("0");
                    rp.setMaxValue("3000");

                } else if (i == 4) {
                    rp.setParaCode("2");
                    rp.setParaName("GR1 INITIAL THICKNESS");
                    rp.setSetValue(transferOx(b[i] + "," + b[i + 1] + "," + b[i + 2] + "," + b[i + 3]));
                    rp.setParaMeasure("μm");
                    rp.setMinValue("0.0");
                    rp.setMaxValue("1999.9");

                } else if (i == 8) {
                    rp.setParaCode("3");
                    rp.setParaName("GR1 TARGET THICKNESS");
                    rp.setSetValue(transferOx(b[i] + "," + b[i + 1] + "," + b[i + 2] + "," + b[i + 3]));
                    rp.setParaMeasure("μm");
                    rp.setParaMeasure("μm");
                    rp.setMinValue("0.0");
                    rp.setMaxValue("1999.9");

                } else if (i == 12) {
                    rp.setParaCode("4");
                    rp.setParaName("GR1 AIR CUT");
                    rp.setSetValue(transferOx(b[i] + "," + b[i + 1] + "," + b[i + 2] + "," + b[i + 3]));
                    rp.setParaMeasure("μm");
                    rp.setParaMeasure("μm");
                    rp.setMinValue("0.0");
                    rp.setMaxValue("9999.9");

                } else if (i == 16) {
                    rp.setParaCode("5");
                    rp.setParaName("GR1 WHEEL FEED2 THICKNESS");
                    rp.setSetValue(transferOx(b[i] + "," + b[i + 1] + "," + b[i + 2] + "," + b[i + 3]));
                    rp.setParaMeasure("μm");
                    rp.setParaMeasure("μm");
                    rp.setMinValue("0.0");
                    rp.setMaxValue("1999.9");

                } else if (i == 20) {
                    rp.setParaCode("6");
                    rp.setParaName("GR1 WHEEL FEED3 THICKNESS");
                    rp.setSetValue(transferOx(b[i] + "," + b[i + 1] + "," + b[i + 2] + "," + b[i + 3]));
                    rp.setParaMeasure("μm");
                    rp.setParaMeasure("μm");
                    rp.setMinValue("0.0");
                    rp.setMaxValue("1999.9");

                } else if (i == 24) {
                    rp.setParaCode("7");
                    rp.setParaName("GR1 WHEEL FEED1");
                    rp.setSetValue(transferOx(b[i] + "," + b[i + 1] + "," + b[i + 2] + "," + b[i + 3]));
                    rp.setParaMeasure("μm");
                    rp.setParaMeasure("μm/sec");
                    rp.setMinValue("0.00");
                    rp.setMaxValue("99.99");

                } else if (i == 28) {
                    rp.setParaCode("8");
                    rp.setParaName("GR1 WHEEL FEED2");
                    rp.setSetValue(transferOx(b[i] + "," + b[i + 1] + "," + b[i + 2] + "," + b[i + 3]));
                    rp.setParaMeasure("μm");
                    rp.setParaMeasure("μm/sec");
                    rp.setMinValue("0.00");
                    rp.setMaxValue("99.99");

                } else if (i == 32) {
                    rp.setParaCode("9");
                    rp.setParaName("GR1 WHEEL FEED3");
                    rp.setSetValue(transferOx(b[i] + "," + b[i + 1] + "," + b[i + 2] + "," + b[i + 3]));
                    rp.setParaMeasure("μm");
                    rp.setParaMeasure("μm/sec");
                    rp.setMinValue("0.00");
                    rp.setMaxValue("99.99");

                } else if (i == 36) {
                    rp.setParaCode("10");
                    rp.setParaName("GR1 CHUCK SPEED1");
                    rp.setSetValue(mathChange(transferOx(b[i] + "," + b[i + 1] + "," + b[i + 2] + "," + b[i + 3])));
                    rp.setParaMeasure("μm");
                    rp.setParaMeasure("Rpm");
                    rp.setMinValue("0");
                    rp.setMaxValue("400");

                } else if (i == 40) {
                    rp.setParaCode("11");
                    rp.setParaName("GR1 CHUCK SPEED2");
                    rp.setSetValue(mathChange(transferOx(b[i] + "," + b[i + 1] + "," + b[i + 2] + "," + b[i + 3])));
                    rp.setParaMeasure("μm");
                    rp.setParaMeasure("Rpm");
                    rp.setMinValue("0");
                    rp.setMaxValue("400");

                } else if (i == 44) {
                    rp.setParaCode("12");
                    rp.setParaName("GR1 CHUCK SPEED3");
                    rp.setSetValue(mathChange(transferOx(b[i] + "," + b[i + 1] + "," + b[i + 2] + "," + b[i + 3])));
                    rp.setParaMeasure("μm");
                    rp.setParaMeasure("Rpm");
                    rp.setMinValue("0");
                    rp.setMaxValue("400");

                } else if (i == 48) {
                    rp.setParaCode("13");
                    rp.setParaName("GR1 CHUCK SPEED4");
                    rp.setSetValue(mathChange(transferOx(b[i] + "," + b[i + 1] + "," + b[i + 2] + "," + b[i + 3])));
                    rp.setParaMeasure("μm");
                    rp.setParaMeasure("Rpm");
                    rp.setMinValue("0");
                    rp.setMaxValue("400");

                } else if (i == 52) {
                    rp.setParaCode("14");
                    rp.setParaName("GR1 SPARK OUT TIME");
                    rp.setSetValue(mathChange(transferOx(b[i] + "," + b[i + 1] + "," + b[i + 2] + "," + b[i + 3])));
                    rp.setParaMeasure("μm");
                    rp.setParaMeasure("Sec");
                    rp.setMinValue("0");
                    rp.setMaxValue("99");

                } else if (i == 56) {
                    rp.setParaCode("15");
                    rp.setParaName("GR1 ESCAPE FEED");
                    rp.setSetValue(transferOx(b[i] + "," + b[i + 1] + "," + b[i + 2] + "," + b[i + 3]));
                    rp.setParaMeasure("μm");
                    rp.setParaMeasure("μm/sec");
                    rp.setMinValue("0.00");
                    rp.setMaxValue("99.99");

                } else if (i == 60) {
                    rp.setParaCode("16");
                    rp.setParaName("GR1 ESCAPE OUT TIME");
                    rp.setSetValue(mathChange(transferOx(b[i] + "," + b[i + 1] + "," + b[i + 2] + "," + b[i + 3])));
                    rp.setParaMeasure("μm");
                    rp.setParaMeasure("Sec");
                    rp.setMinValue("0");
                    rp.setMaxValue("99");

                } else if (i == 64) {
                    rp.setParaCode("17");
                    rp.setParaName("GR2 WHEEL SPEED");
                    rp.setSetValue(mathChange(transferOx(b[i] + "," + b[i + 1] + "," + b[i + 2] + "," + b[i + 3])));
                    rp.setParaMeasure("μm");
                    rp.setParaMeasure("Rpm");
                    rp.setMinValue("0");
                    rp.setMaxValue("3000");

                } else if (i == 68) {
                    rp.setParaCode("18");
                    rp.setParaName("GR2 INITIAL THICKNESS");
                    rp.setSetValue(transferOx(b[i] + "," + b[i + 1] + "," + b[i + 2] + "," + b[i + 3]));
                    rp.setParaMeasure("μm");
                    rp.setParaMeasure("μm");
                    rp.setMinValue("0.0");
                    rp.setMaxValue("1999.9");

                } else if (i == 72) {
                    rp.setParaCode("19");
                    rp.setParaName("GR2 TARGET THICKNESS");
                    rp.setSetValue(transferOx(b[i] + "," + b[i + 1] + "," + b[i + 2] + "," + b[i + 3]));
                    rp.setParaMeasure("μm");
                    rp.setParaMeasure("μm");
                    rp.setMinValue("0.0");
                    rp.setMaxValue("1999.9");

                } else if (i == 76) {
                    rp.setParaCode("20");
                    rp.setParaName("GR2 AIR CUT");
                    rp.setSetValue(transferOx(b[i] + "," + b[i + 1] + "," + b[i + 2] + "," + b[i + 3]));
                    rp.setParaMeasure("μm");
                    rp.setParaMeasure("μm");
                    rp.setMinValue("0.0");
                    rp.setMaxValue("9999.9");

                } else if (i == 80) {
                    rp.setParaCode("21");
                    rp.setParaName("GR2 WHEEL FEED2 THICKNESS");
                    rp.setSetValue(transferOx(b[i] + "," + b[i + 1] + "," + b[i + 2] + "," + b[i + 3]));
                    rp.setParaMeasure("μm");
                    rp.setParaMeasure("μm");
                    rp.setMinValue("0.0");
                    rp.setMaxValue("1999.9");

                } else if (i == 84) {
                    rp.setParaCode("22");
                    rp.setParaName("GR2 WHEEL FEED3 THICKNESS");
                    rp.setSetValue(transferOx(b[i] + "," + b[i + 1] + "," + b[i + 2] + "," + b[i + 3]));
                    rp.setParaMeasure("μm");
                    rp.setParaMeasure("μm");
                    rp.setMinValue("0.0");
                    rp.setMaxValue("1999.9");

                } else if (i == 88) {
                    rp.setParaCode("23");
                    rp.setParaName("GR2 WHEEL FEED1");
                    rp.setSetValue(transferOx(b[i] + "," + b[i + 1] + "," + b[i + 2] + "," + b[i + 3]));
                    rp.setParaMeasure("μm");
                    rp.setParaMeasure("μm/sec");
                    rp.setMinValue("0.00");
                    rp.setMaxValue("99.99");

                } else if (i == 92) {
                    rp.setParaCode("24");
                    rp.setParaName("GR2 WHEEL FEED2");
                    rp.setSetValue(transferOx(b[i] + "," + b[i + 1] + "," + b[i + 2] + "," + b[i + 3]));
                    rp.setParaMeasure("μm");
                    rp.setParaMeasure("μm/sec");
                    rp.setMinValue("0.00");
                    rp.setMaxValue("99.99");

                } else if (i == 96) {
                    rp.setParaCode("25");
                    rp.setParaName("GR2 WHEEL FEED3");
                    rp.setSetValue(transferOx(b[i] + "," + b[i + 1] + "," + b[i + 2] + "," + b[i + 3]));
                    rp.setParaMeasure("μm");
                    rp.setParaMeasure("μm/sec");
                    rp.setMinValue("0.00");
                    rp.setMaxValue("99.99");

                } else if (i == 100) {
                    rp.setParaCode("26");
                    rp.setParaName("GR2 CHUCK SPEED1");
                    rp.setSetValue(mathChange(transferOx(b[i] + "," + b[i + 1] + "," + b[i + 2] + "," + b[i + 3])));
                    rp.setParaMeasure("μm");
                    rp.setParaMeasure("Rpm");
                    rp.setMinValue("0");
                    rp.setMaxValue("400");

                } else if (i == 104) {
                    rp.setParaCode("27");
                    rp.setParaName("GR2 CHUCK SPEED2");
                    rp.setSetValue(mathChange(transferOx(b[i] + "," + b[i + 1] + "," + b[i + 2] + "," + b[i + 3])));
                    rp.setParaMeasure("μm");
                    rp.setParaMeasure("Rpm");
                    rp.setMinValue("0");
                    rp.setMaxValue("400");

                } else if (i == 108) {
                    rp.setParaCode("28");
                    rp.setParaName("GR2 CHUCK SPEED3");
                    rp.setSetValue(mathChange(transferOx(b[i] + "," + b[i + 1] + "," + b[i + 2] + "," + b[i + 3])));
                    rp.setParaMeasure("μm");
                    rp.setParaMeasure("Rpm");
                    rp.setMinValue("0");
                    rp.setMaxValue("400");

                } else if (i == 112) {
                    rp.setParaCode("29");
                    rp.setParaName("GR2 CHUCK SPEED4");
                    rp.setSetValue(mathChange(transferOx(b[i] + "," + b[i + 1] + "," + b[i + 2] + "," + b[i + 3])));
                    rp.setParaMeasure("μm");
                    rp.setParaMeasure("Rpm");
                    rp.setMinValue("0");
                    rp.setMaxValue("400");

                } else if (i == 116) {
                    rp.setParaCode("30");
                    rp.setParaName("GR2 SPARK OUT TIME");
                    rp.setSetValue(mathChange(transferOx(b[i] + "," + b[i + 1] + "," + b[i + 2] + "," + b[i + 3])));
                    rp.setParaMeasure("μm");
                    rp.setParaMeasure("Sec");
                    rp.setMinValue("0");
                    rp.setMaxValue("99");

                } else if (i == 120) {
                    rp.setParaCode("31");
                    rp.setParaName("GR2 ESCAPE FEED");
                    rp.setSetValue(transferOx(b[i] + "," + b[i + 1] + "," + b[i + 2] + "," + b[i + 3]));
                    rp.setParaMeasure("μm");
                    rp.setParaMeasure("μm/sec");
                    rp.setMinValue("0.00");
                    rp.setMaxValue("99.99");

                } else if (i == 124) {
                    rp.setParaCode("32");
                    rp.setParaName("GR2 ESCAPE TIME");
                    rp.setSetValue(mathChange(transferOx(b[i] + "," + b[i + 1] + "," + b[i + 2] + "," + b[i + 3])));
                    rp.setParaMeasure("μm");
                    rp.setParaMeasure("Sec");
                    rp.setMinValue("0");
                    rp.setMaxValue("99");

                } else if (i == 128) {
                    rp.setParaCode("33");
                    rp.setParaName("WAFER SIZE");
                    rp.setSetValue(mathChange(transferOx(b[i] + "," + b[i + 1] + "," + b[i + 2] + "," + b[i + 3])));
                    rp.setParaMeasure("μm");
                    rp.setParaMeasure("Inch");
                    rp.setMinValue("8");
                    rp.setMaxValue("12");

                } else if (i == 132) {
                    rp.setParaCode("34");
                    rp.setParaName("NOT USED");
                    rp.setSetValue("");
                    rp.setParaMeasure("");
                    rp.setParaMeasure("");
                    rp.setMinValue("");
                    rp.setMaxValue("");

                } else if (i == 136) {
                    rp.setParaCode("35");
                    rp.setParaName("NOT USED");
                    rp.setSetValue("");
                    rp.setParaMeasure("");
                    rp.setParaMeasure("");
                    rp.setMinValue("");
                    rp.setMaxValue("");

                } else if (i == 140) {
                    rp.setParaCode("36");
                    rp.setParaName("P1 POLISHING TIME");
                    rp.setSetValue(mathChange(transferOx(b[i] + "," + b[i + 1] + "," + b[i + 2] + "," + b[i + 3])));
                    rp.setParaMeasure("μm");
                    rp.setParaMeasure("sec");
                    rp.setMinValue("0");
                    rp.setMaxValue("999");

                } else if (i == 144) {
                    rp.setParaCode("37");
                    rp.setParaName("P1 POLISH HEAD SPEED");
                    rp.setSetValue(mathChange(transferOx(b[i] + "," + b[i + 1] + "," + b[i + 2] + "," + b[i + 3])));
                    rp.setParaMeasure("μm");
                    rp.setParaMeasure("rpm");
                    rp.setMinValue("0");
                    rp.setMaxValue("280");

                } else if (i == 148) {
                    rp.setParaCode("38");
                    rp.setParaName("P1 CHUCK SPEED");
                    rp.setSetValue(mathChange(transferOx(b[i] + "," + b[i + 1] + "," + b[i + 2] + "," + b[i + 3])));
                    rp.setParaMeasure("μm");
                    rp.setParaMeasure("rpm");
                    rp.setMinValue("0");
                    rp.setMaxValue("400");

                } else if (i == 152) {
                    rp.setParaCode("39");
                    rp.setParaName("P1 AIR PRESSURE");
                    rp.setSetValue(transferOx(b[i] + "," + b[i + 1] + "," + b[i + 2] + "," + b[i + 3]));
                    rp.setParaMeasure("μm");
                    rp.setParaMeasure("V");
                    rp.setMinValue("0.00");
                    rp.setMaxValue("10.00");

                } else if (i == 156) {
                    rp.setParaCode("40");
                    rp.setParaName("P1 SLURRY");
                    rp.setSetValue(transferOx(b[i] + "," + b[i + 1] + "," + b[i + 2] + "," + b[i + 3]));
                    rp.setParaMeasure("μm");
                    rp.setParaMeasure("ml/min");
                    rp.setMinValue("0.0");
                    rp.setMaxValue("500.0");

                } else if (i == 160) {
                    s = mathChange(transferOx(b[i] + "," + b[i + 1] + "," + b[i + 2] + "," + b[i + 3]));
                    rp.setParaCode("41");
                    rp.setParaName("P1 RINSE");
                    if ("0".equals(s)) {
                        rp.setSetValue("OFF");
                    } else if ("1".equals(s)) {
                        rp.setSetValue("ON");
                    } else {
                        System.out.println("无此数据");
                    }
                    rp.setParaMeasure("Digit");
                    rp.setMinValue("0");
                    rp.setMaxValue("1");

                } else if (i == 164) {
                    rp.setParaCode("42");
                    rp.setParaName("");
                    rp.setSetValue("");
                    rp.setParaMeasure("");
                    rp.setMinValue("");
                    rp.setMaxValue("");

                } else if (i == 168) {
                    rp.setParaCode("43");
                    rp.setParaName("P2 POLISHING TIME");
                    rp.setSetValue(mathChange(transferOx(b[i] + "," + b[i + 1] + "," + b[i + 2] + "," + b[i + 3])));
                    rp.setParaMeasure("sec");
                    rp.setMinValue("0");
                    rp.setMaxValue("999");

                } else if (i == 172) {
                    rp.setParaCode("44");
                    rp.setParaName("P2 POLISH HEAD SPEED");
                    rp.setSetValue(mathChange(transferOx(b[i] + "," + b[i + 1] + "," + b[i + 2] + "," + b[i + 3])));
                    rp.setParaMeasure("rpm");
                    rp.setMinValue("0");
                    rp.setMaxValue("280");

                } else if (i == 176) {
                    rp.setParaCode("45");
                    rp.setParaName("P2 CHUCK SPEED");
                    rp.setSetValue(mathChange(transferOx(b[i] + "," + b[i + 1] + "," + b[i + 2] + "," + b[i + 3])));
                    rp.setParaMeasure("rpm");
                    rp.setMinValue("0");
                    rp.setMaxValue("400");

                } else if (i == 180) {
                    rp.setParaCode("46");
                    rp.setParaName("P2 AIR PRESSURE");
                    rp.setSetValue(transferOx(b[i] + "," + b[i + 1] + "," + b[i + 2] + "," + b[i + 3]));
                    rp.setParaMeasure("V");
                    rp.setMinValue("0.00");
                    rp.setMaxValue("10.00");

                } else if (i == 184) {
                    rp.setParaCode("47");
                    rp.setParaName("P2 SLURRY");
                    rp.setSetValue(transferOx(b[i] + "," + b[i + 1] + "," + b[i + 2] + "," + b[i + 3]));
                    rp.setParaMeasure("ml/min");
                    rp.setMinValue("0.0");
                    rp.setMaxValue("500.0");

                } else if (i == 188) {
                    s = mathChange(transferOx(b[i] + "," + b[i + 1] + "," + b[i + 2] + "," + b[i + 3]));
                    rp.setParaCode("48");
                    rp.setParaName("P2 RINSE");
                    if ("0".equals(s)) {
                        rp.setSetValue("OFF");
                    } else if ("1".equals(s)) {
                        rp.setSetValue("ON");
                    } else {
                        System.out.println("无此数据");
                    }
                    rp.setParaMeasure("Digit");
                    rp.setMinValue("0");
                    rp.setMaxValue("1");

                } else if (i == 192) {
                    rp.setParaCode("49");
                    rp.setParaName("NOT USED");
                    rp.setSetValue("");
                    rp.setParaMeasure("");
                    rp.setMinValue("");
                    rp.setMaxValue("");

                } else if (i == 196) {
                    rp.setParaCode("50");
                    rp.setParaName("P3 POLISHING TIME");
                    rp.setSetValue(mathChange(transferOx(b[i] + "," + b[i + 1] + "," + b[i + 2] + "," + b[i + 3])));
                    rp.setParaMeasure("sec");
                    rp.setMinValue("0");
                    rp.setMaxValue("999");

                } else if (i == 200) {
                    rp.setParaCode("51");
                    rp.setParaName("P3 POLISH HEAD SPEED");
                    rp.setSetValue(mathChange(transferOx(b[i] + "," + b[i + 1] + "," + b[i + 2] + "," + b[i + 3])));
                    rp.setParaMeasure("rpm");
                    rp.setMinValue("0");
                    rp.setMaxValue("280");

                } else if (i == 204) {
                    rp.setParaCode("52");
                    rp.setParaName("P3 CHUCK SPEED");
                    rp.setSetValue(mathChange(transferOx(b[i] + "," + b[i + 1] + "," + b[i + 2] + "," + b[i + 3])));
                    rp.setParaMeasure("rpm");
                    rp.setMinValue("0");
                    rp.setMaxValue("400");

                } else if (i == 208) {
                    rp.setParaCode("53");
                    rp.setParaName("P3 AIR PRESSURE");
                    rp.setSetValue(transferOx(b[i] + "," + b[i + 1] + "," + b[i + 2] + "," + b[i + 3]));
                    rp.setParaMeasure("V");
                    rp.setMinValue("0.00");
                    rp.setMaxValue("10.00");

                } else if (i == 212) {
                    rp.setParaCode("54");
                    rp.setParaName("P3 SLURRY");
                    rp.setSetValue(transferOx(b[i] + "," + b[i + 1] + "," + b[i + 2] + "," + b[i + 3]));
                    rp.setParaMeasure("ml/min");
                    rp.setMinValue("0.0");
                    rp.setMaxValue("500.0");

                } else if (i == 216) {
                    s = mathChange(transferOx(b[i] + "," + b[i + 1] + "," + b[i + 2] + "," + b[i + 3]));
                    rp.setParaCode("55");
                    rp.setParaName("P3 RINSE");
                    if ("0".equals(s)) {
                        rp.setSetValue("OFF");
                    } else if ("1".equals(s)) {
                        rp.setSetValue("ON");
                    } else {
                        System.out.println("无此数据");
                    }
                    rp.setParaMeasure("Digit");
                    rp.setMinValue("0");
                    rp.setMaxValue("1");

                } else if (i == 220) {
                    rp.setParaCode("56");
                    rp.setParaName("NOT USED");
                    rp.setSetValue("");
                    rp.setParaMeasure("");
                    rp.setMinValue("");
                    rp.setMaxValue("");

                } else if (i == 224) {
                    rp.setParaCode("57");
                    rp.setParaName("P4 POLISHING TIME");
                    rp.setSetValue(mathChange(transferOx(b[i] + "," + b[i + 1] + "," + b[i + 2] + "," + b[i + 3])));
                    rp.setParaMeasure("sec");
                    rp.setMinValue("0");
                    rp.setMaxValue("999");

                } else if (i == 228) {
                    rp.setParaCode("58");
                    rp.setParaName("P4 POLISH HEAD SPEED");
                    rp.setSetValue(mathChange(transferOx(b[i] + "," + b[i + 1] + "," + b[i + 2] + "," + b[i + 3])));
                    rp.setParaMeasure("rpm");
                    rp.setMinValue("0");
                    rp.setMaxValue("280");

                } else if (i == 232) {
                    rp.setParaCode("59");
                    rp.setParaName("P4 CHUCK SPEED");
                    rp.setSetValue(mathChange(transferOx(b[i] + "," + b[i + 1] + "," + b[i + 2] + "," + b[i + 3])));
                    rp.setParaMeasure("rpm");
                    rp.setMinValue("0");
                    rp.setMaxValue("400");

                } else if (i == 236) {
                    rp.setParaCode("60");
                    rp.setParaName("P4 AIR PRESSURE");
                    rp.setSetValue(transferOx(b[i] + "," + b[i + 1] + "," + b[i + 2] + "," + b[i + 3]));
                    rp.setParaMeasure("V");
                    rp.setMinValue("0.00");
                    rp.setMaxValue("10.00");

                } else if (i == 240) {
                    rp.setParaCode("61");
                    rp.setParaName("P4 SLURRY");
                    rp.setSetValue(transferOx(b[i] + "," + b[i + 1] + "," + b[i + 2] + "," + b[i + 3]));
                    rp.setParaMeasure("ml/min");
                    rp.setMinValue("0.0");
                    rp.setMaxValue("500.0");

                } else if (i == 244) {
                    s = mathChange(transferOx(b[i] + "," + b[i + 1] + "," + b[i + 2] + "," + b[i + 3]));
                    rp.setParaCode("62");
                    rp.setParaName("P4 RINSE");
                    if ("0".equals(s)) {
                        rp.setSetValue("OFF");
                    } else if ("1".equals(s)) {
                        rp.setSetValue("ON");
                    } else {
                        System.out.println("无此数据");
                    }
                    rp.setParaMeasure("Digit");
                    rp.setMinValue("0");
                    rp.setMaxValue("1");

                } else if (i == 248) {
                    rp.setParaCode("63");
                    rp.setParaName("NOT USED");
                    rp.setSetValue("");
                    rp.setParaMeasure("");
                    rp.setMinValue("");
                    rp.setMaxValue("");

                } else if (i == 252) {
                    rp.setParaCode("64");
                    rp.setParaName("P5 POLISHING TIME");
                    rp.setSetValue(mathChange(transferOx(b[i] + "," + b[i + 1] + "," + b[i + 2] + "," + b[i + 3])));
                    rp.setParaMeasure("sec");
                    rp.setMinValue("0");
                    rp.setMaxValue("999");

                } else if (i == 256) {
                    rp.setParaCode("65");
                    rp.setParaName("P5 POLISH HEAD SPEED");
                    rp.setSetValue(mathChange(transferOx(b[i] + "," + b[i + 1] + "," + b[i + 2] + "," + b[i + 3])));
                    rp.setParaMeasure("rpm");
                    rp.setMinValue("0");
                    rp.setMaxValue("280");

                } else if (i == 260) {
                    rp.setParaCode("66");
                    rp.setParaName("P5 CHUCK SPEED");
                    rp.setSetValue(mathChange(transferOx(b[i] + "," + b[i + 1] + "," + b[i + 2] + "," + b[i + 3])));
                    rp.setParaMeasure("rpm");
                    rp.setMinValue("0");
                    rp.setMaxValue("400");

                } else if (i == 264) {
                    rp.setParaCode("67");
                    rp.setParaName("P5 AIR PRESSURE");
                    rp.setSetValue(transferOx(b[i] + "," + b[i + 1] + "," + b[i + 2] + "," + b[i + 3]));
                    rp.setParaMeasure("V");
                    rp.setMinValue("0.00");
                    rp.setMaxValue("10.00");

                } else if (i == 268) {
                    rp.setParaCode("68");
                    rp.setParaName("P5 SLURRY");
                    rp.setSetValue(transferOx(b[i] + "," + b[i + 1] + "," + b[i + 2] + "," + b[i + 3]));
                    rp.setParaMeasure("ml/min");
                    rp.setMinValue("0.0");
                    rp.setMaxValue("500.0");

                } else if (i == 272) {
                    s = mathChange(transferOx(b[i] + "," + b[i + 1] + "," + b[i + 2] + "," + b[i + 3]));
                    rp.setParaCode("69");
                    rp.setParaName("P5 RINSE");
                    if ("0".equals(s)) {
                        rp.setSetValue("OFF");
                    } else if ("1".equals(s)) {
                        rp.setSetValue("ON");
                    } else {
                        System.out.println("无此数据");
                    }
                    rp.setParaMeasure("Digit");
                    rp.setMinValue("0");
                    rp.setMaxValue("1");

                } else if (i == 276) {
                    rp.setParaCode("70");
                    rp.setParaName("NOT USED");
                    rp.setSetValue("");
                    rp.setParaMeasure("");
                    rp.setMinValue("");
                    rp.setMaxValue("");

                } else if (i == 280) {
                    rp.setParaCode("71");
                    rp.setParaName("P6 POLISHING TIME");
                    rp.setSetValue(mathChange(transferOx(b[i] + "," + b[i + 1] + "," + b[i + 2] + "," + b[i + 3])));
                    rp.setParaMeasure("sec");
                    rp.setMinValue("0");
                    rp.setMaxValue("999");

                } else if (i == 284) {
                    rp.setParaCode("72");
                    rp.setParaName("P6 POLISH HEAD SPEED");
                    rp.setSetValue(mathChange(transferOx(b[i] + "," + b[i + 1] + "," + b[i + 2] + "," + b[i + 3])));
                    rp.setParaMeasure("rpm");
                    rp.setMinValue("0");
                    rp.setMaxValue("280");

                } else if (i == 288) {
                    rp.setParaCode("73");
                    rp.setParaName("P6 CHUCK SPEED");
                    rp.setSetValue(mathChange(transferOx(b[i] + "," + b[i + 1] + "," + b[i + 2] + "," + b[i + 3])));
                    rp.setParaMeasure("rpm");
                    rp.setMinValue("0");
                    rp.setMaxValue("400");

                } else if (i == 292) {
                    rp.setParaCode("74");
                    rp.setParaName("P6 AIR PRESSURE");
                    rp.setSetValue(transferOx(b[i] + "," + b[i + 1] + "," + b[i + 2] + "," + b[i + 3]));
                    rp.setParaMeasure("V");
                    rp.setMinValue("0.00");
                    rp.setMaxValue("10.00");

                } else if (i == 296) {
                    rp.setParaCode("75");
                    rp.setParaName("P6 SLURRY");
                    rp.setSetValue(transferOx(b[i] + "," + b[i + 1] + "," + b[i + 2] + "," + b[i + 3]));
                    rp.setParaMeasure("ml/min");
                    rp.setMinValue("0.0");
                    rp.setMaxValue("500.0");

                } else if (i == 300) {
                    s = mathChange(transferOx(b[i] + "," + b[i + 1] + "," + b[i + 2] + "," + b[i + 3]));
                    rp.setParaCode("76");
                    rp.setParaName("P6 RINSE");
                    if ("0".equals(s)) {
                        rp.setSetValue("OFF");
                    } else if ("1".equals(s)) {
                        rp.setSetValue("ON");
                    } else {
                        System.out.println("无此数据");
                    }
                    rp.setParaMeasure("Digit");
                    rp.setMinValue("0");
                    rp.setMaxValue("1");

                } else if (i == 304) {
                    rp.setParaCode("77");
                    rp.setParaName("NOT USED");
                    rp.setSetValue("");
                    rp.setParaMeasure("");
                    rp.setMinValue("");
                    rp.setMaxValue("");

                } else if (i == 308) {
                    rp.setParaCode("78");
                    rp.setParaName("NOT USED");
                    rp.setSetValue("");
                    rp.setParaMeasure("");
                    rp.setMinValue("");
                    rp.setMaxValue("");

                } else if (i == 312) {
                    rp.setParaCode("79");
                    rp.setParaName("NOT USED");
                    rp.setSetValue("");
                    rp.setParaMeasure("");
                    rp.setMinValue("");
                    rp.setMaxValue("");

                } else if (i == 316) {
                    rp.setParaCode("80");
                    rp.setParaName("NOT USED");
                    rp.setSetValue("");
                    rp.setParaMeasure("");
                    rp.setMinValue("");
                    rp.setMaxValue("");

                } else if (i == 320) {
                    rp.setParaCode("81");
                    rp.setParaName("PROCESS MODE");
                    rp.setSetValue(mathChange(transferOx(b[i] + "," + b[i + 1] + "," + b[i + 2] + "," + b[i + 3])));
                    rp.setParaMeasure("Digit");
                    rp.setMinValue("0");
                    rp.setMaxValue("");

                } else if (i == 324) {
                    rp.setParaCode("82");
                    rp.setParaName("NOT USED");
                    rp.setSetValue("");
                    rp.setParaMeasure("");
                    rp.setMinValue("");
                    rp.setMaxValue("");

                } else if (i == 328) {
                    rp.setParaCode("83");
                    rp.setParaName("RESERVED");
                    rp.setSetValue("");
                    rp.setParaMeasure("");
                    rp.setMinValue("");
                    rp.setMaxValue("");

                } else if (i == 332) {
                    rp.setParaCode("84");
                    rp.setParaName("NOTCH ANGLE");
                    rp.setSetValue(mathChange(transferOx(b[i] + "," + b[i + 1] + "," + b[i + 2] + "," + b[i + 3])));
                    rp.setParaMeasure("deg");
                    rp.setMinValue("0");
                    rp.setMaxValue("270");

                } else if (i == 336) {
                    s = mathChange(transferOx(b[i] + "," + b[i + 1] + "," + b[i + 2] + "," + b[i + 3]));
                    rp.setParaCode("85");
                    rp.setParaName("Positioning St OCR PASS");
                    if ("0".equals(s)) {
                        rp.setSetValue("OCR PASS");
                    } else if ("1".equals(s)) {
                        rp.setSetValue("OCR USE");
                    } else {
                        System.out.println("无此数据");
                    }
                    rp.setParaMeasure("0/1");
                    rp.setMinValue("0");
                    rp.setMaxValue("270");

                } else if (i == 340) {
                    rp.setParaCode("86");
                    rp.setParaName("RESERVED");
                    rp.setSetValue("");
                    rp.setParaMeasure("");
                    rp.setMinValue("");
                    rp.setMaxValue("");

                } else if (i == 344) {
                    rp.setParaCode("87");
                    rp.setParaName("RESERVED");
                    rp.setSetValue("");
                    rp.setParaMeasure("");
                    rp.setMinValue("");
                    rp.setMaxValue("");

                } else if (i == 348) {
                    rp.setParaCode("88");
                    rp.setParaName("RESERVED");
                    rp.setSetValue("");
                    rp.setParaMeasure("");
                    rp.setMinValue("");
                    rp.setMaxValue("");

                } else if (i == 352) {
                    rp.setParaCode(":");
                    rp.setParaName("");
                    rp.setSetValue("");
                    rp.setParaMeasure("");
                    rp.setMinValue("");
                    rp.setMaxValue("");

                } else if (i == 356) {
                    rp.setParaCode(":");
                    rp.setParaName("");
                    rp.setSetValue("");
                    rp.setParaMeasure("");
                    rp.setMinValue("");
                    rp.setMaxValue("");

                } else if (i == 400) {
                    rp.setParaCode("101");
                    rp.setParaName("Wafer Size 0/1");
                    rp.setSetValue(mathChange(transferOx(b[i] + "," + b[i + 1] + "," + b[i + 2] + "," + b[i + 3])));
                    rp.setParaMeasure("");
                    rp.setMinValue("0");
                    rp.setMaxValue("1");

                } else if (i == 404) {
                    rp.setParaCode("102");
                    rp.setParaName("Frame Size 0/1");
                    rp.setSetValue(mathChange(transferOx(b[i] + "," + b[i + 1] + "," + b[i + 2] + "," + b[i + 3])));
                    rp.setParaMeasure("");
                    rp.setMinValue("0");
                    rp.setMaxValue("1");

                } else if (i == 408) {
                    rp.setParaCode("103");
                    rp.setParaName("Wafer Thickness");
                    rp.setSetValue(mathChange(transferOx(b[i] + "," + b[i + 1] + "," + b[i + 2] + "," + b[i + 3])));
                    rp.setParaMeasure("um");
                    rp.setMinValue("10");
                    rp.setMaxValue("1500");

                } else if (i == 412) {
                    rp.setParaCode("104");
                    rp.setParaName("Bg Peel Retry");
                    rp.setSetValue(mathChange(transferOx(b[i] + "," + b[i + 1] + "," + b[i + 2] + "," + b[i + 3])));
                    rp.setParaMeasure("time");
                    rp.setMinValue("0");
                    rp.setMaxValue("9");

                } else if (i == 416) {
                    rp.setParaCode("105");
                    rp.setParaName("Apply Pressure1");
                    rp.setSetValue(mathChange(transferOx(b[i] + "," + b[i + 1] + "," + b[i + 2] + "," + b[i + 3])));
                    rp.setParaMeasure("％");
                    rp.setMinValue("0");
                    rp.setMaxValue("100");

                } else if (i == 420) {
                    rp.setParaCode("106");
                    rp.setParaName("Apply Pressure2");
                    rp.setSetValue(mathChange(transferOx(b[i] + "," + b[i + 1] + "," + b[i + 2] + "," + b[i + 3])));
                    rp.setParaMeasure("％");
                    rp.setMinValue("0");
                    rp.setMaxValue("100");

                } else if (i == 424) {
                    rp.setParaCode("107");
                    rp.setParaName("Apply Pressure3");
                    rp.setSetValue(mathChange(transferOx(b[i] + "," + b[i + 1] + "," + b[i + 2] + "," + b[i + 3])));
                    rp.setParaMeasure("％");
                    rp.setMinValue("0");
                    rp.setMaxValue("100");

                } else if (i == 428) {
                    rp.setParaCode("108");
                    rp.setParaName("Apply Pressure4");
                    rp.setSetValue(mathChange(transferOx(b[i] + "," + b[i + 1] + "," + b[i + 2] + "," + b[i + 3])));
                    rp.setParaMeasure("％");
                    rp.setMinValue("0");
                    rp.setMaxValue("100");

                } else if (i == 432) {
                    rp.setParaCode("109");
                    rp.setParaName("Apply Pressure5");
                    rp.setSetValue(mathChange(transferOx(b[i] + "," + b[i + 1] + "," + b[i + 2] + "," + b[i + 3])));
                    rp.setParaMeasure("％");
                    rp.setMinValue("0");
                    rp.setMaxValue("100");
                } else if (i == 436) {
                    rp.setParaCode("110");
                    rp.setParaName("Dc Apply Rate");
                    rp.setSetValue(mathChange(transferOx(b[i] + "," + b[i + 1] + "," + b[i + 2] + "," + b[i + 3])));
                    rp.setParaMeasure("mm/s*s");
                    rp.setMinValue("5");
                    rp.setMaxValue("700");
                } else if (i == 440) {
                    rp.setParaCode("111");
                    rp.setParaName("Dc Apply Hspd");
                    rp.setSetValue(mathChange(transferOx(b[i] + "," + b[i + 1] + "," + b[i + 2] + "," + b[i + 3])));
                    rp.setParaMeasure("mm/s");
                    rp.setMinValue("1");
                    rp.setMaxValue("160");
                } else if (i == 444) {
                    rp.setParaCode("112");
                    rp.setParaName("Dc Apply Lspd");
                    rp.setSetValue(mathChange(transferOx(b[i] + "," + b[i + 1] + "," + b[i + 2] + "," + b[i + 3])));
                    rp.setParaMeasure("mm/s");
                    rp.setMinValue("1");
                    rp.setMaxValue("160");
                } else if (i == 448) {
                    rp.setParaCode("113");
                    rp.setParaName("Bg Peel Preheat Time");
                    rp.setSetValue(transferOx(b[i] + "," + b[i + 1] + "," + b[i + 2] + "," + b[i + 3]));
                    rp.setParaMeasure("sec");
                    rp.setMinValue("0");
                    rp.setMaxValue("99.9");
                } else if (i == 452) {
                    rp.setParaCode("114");
                    rp.setParaName("Liner Select 0/1");
                    rp.setSetValue(mathChange(transferOx(b[i] + "," + b[i + 1] + "," + b[i + 2] + "," + b[i + 3])));
                    rp.setParaMeasure("");
                    rp.setMinValue("0");
                    rp.setMaxValue("1");
                } else if (i == 456) {
                    rp.setParaCode("115");
                    rp.setParaName("Dc Peel Select 0/1");
                    rp.setSetValue(mathChange(transferOx(b[i] + "," + b[i + 1] + "," + b[i + 2] + "," + b[i + 3])));
                    rp.setParaMeasure("");
                    rp.setMinValue("0");
                    rp.setMaxValue("1");

                } else if (i == 460) {
                    rp.setParaCode("116");
                    rp.setParaName("Bg Tape Thickness");
                    rp.setSetValue(mathChange(transferOx(b[i] + "," + b[i + 1] + "," + b[i + 2] + "," + b[i + 3])));
                    rp.setParaMeasure("um ");
                    rp.setMinValue("10");
                    rp.setMaxValue("500");

                } else if (i == 464) {
                    rp.setParaCode("117");
                    rp.setParaName("Dc Tape Thickness");
                    rp.setSetValue(mathChange(transferOx(b[i] + "," + b[i + 1] + "," + b[i + 2] + "," + b[i + 3])));
                    rp.setParaMeasure("um ");
                    rp.setMinValue("10");
                    rp.setMaxValue("500");

                } else if (i == 468) {
                    rp.setParaCode("118");
                    rp.setParaName("Peel Tape Thickness");
                    rp.setSetValue(mathChange(transferOx(b[i] + "," + b[i + 1] + "," + b[i + 2] + "," + b[i + 3])));
                    rp.setParaMeasure("um ");
                    rp.setMinValue("10");
                    rp.setMaxValue("500");

                } else if (i == 472) {
                    rp.setParaCode("119");
                    rp.setParaName("Bg Peel Table Up Pos");
                    rp.setSetValue(transferOx(b[i] + "," + b[i + 1] + "," + b[i + 2] + "," + b[i + 3]));
                    rp.setParaMeasure("mm ");
                    rp.setMinValue("-50.0");
                    rp.setMaxValue("400.0 ");

                } else if (i == 476) {
                    rp.setParaCode("120");
                    rp.setParaName("Bg Peel Table Up Speed");
                    rp.setSetValue(mathChange(transferOx(b[i] + "," + b[i + 1] + "," + b[i + 2] + "," + b[i + 3])));
                    rp.setParaMeasure("mm/s");
                    rp.setMinValue("1");
                    rp.setMaxValue("300");

                } else if (i == 480) {
                    rp.setParaCode("121");
                    rp.setParaName("Bg Peel Table Up Bar Height");
                    rp.setSetValue(mathChange(transferOx(b[i] + "," + b[i + 1] + "," + b[i + 2] + "," + b[i + 3])));
                    rp.setParaMeasure("um ");
                    rp.setMinValue("-999");
                    rp.setMaxValue("999");

                } else if (i == 484) {
                    rp.setParaCode("122");
                    rp.setParaName("Bg Peel Start Pos");
                    rp.setSetValue(transferOx(b[i] + "," + b[i + 1] + "," + b[i + 2] + "," + b[i + 3]));
                    rp.setParaMeasure("mm ");
                    rp.setMinValue("-50.0");
                    rp.setMaxValue("400.0");

                } else if (i == 488) {
                    rp.setParaCode("123");
                    rp.setParaName("Bg Peel Start Speed");
                    rp.setSetValue(mathChange(transferOx(b[i] + "," + b[i + 1] + "," + b[i + 2] + "," + b[i + 3])));
                    rp.setParaMeasure("mm/s");
                    rp.setMinValue("1");
                    rp.setMaxValue("300");

                } else if (i == 492) {
                    rp.setParaCode("124");
                    rp.setParaName("Bg Peel Start Bar Height");
                    rp.setSetValue(mathChange(transferOx(b[i] + "," + b[i + 1] + "," + b[i + 2] + "," + b[i + 3])));
                    rp.setParaMeasure("um ");
                    rp.setMinValue("-999");
                    rp.setMaxValue("999");

                } else if (i == 496) {
                    rp.setParaCode("125");
                    rp.setParaName("Bg Peel Change Pos");
                    rp.setSetValue(transferOx(b[i] + "," + b[i + 1] + "," + b[i + 2] + "," + b[i + 3]));
                    rp.setParaMeasure("mm ");
                    rp.setMinValue("-50.0");
                    rp.setMaxValue("400.0");

                } else if (i == 500) {
                    rp.setParaCode("126");
                    rp.setParaName("Bg Peel Change Speed");
                    rp.setSetValue(mathChange(transferOx(b[i] + "," + b[i + 1] + "," + b[i + 2] + "," + b[i + 3])));
                    rp.setParaMeasure("mm/s");
                    rp.setMinValue("1");
                    rp.setMaxValue("300");

                } else if (i == 504) {
                    rp.setParaCode("127");
                    rp.setParaName("Bg Peel Change Bar Height");
                    rp.setSetValue(mathChange(transferOx(b[i] + "," + b[i + 1] + "," + b[i + 2] + "," + b[i + 3])));
                    rp.setParaMeasure("um ");
                    rp.setMinValue("-999");
                    rp.setMaxValue("999");

                } else if (i == 512) {
                    rp.setParaCode("129");
                    rp.setParaName("Bg Peel End Speed");
                    rp.setSetValue(mathChange(transferOx(b[i] + "," + b[i + 1] + "," + b[i + 2] + "," + b[i + 3])));
                    rp.setParaMeasure("mm/s");
                    rp.setMinValue("1");
                    rp.setMaxValue("300");

                } else if (i == 516) {
                    rp.setParaCode("130");
                    rp.setParaName("Bg Peel End Bar Height");
                    rp.setSetValue(mathChange(transferOx(b[i] + "," + b[i + 1] + "," + b[i + 2] + "," + b[i + 3])));
                    rp.setParaMeasure("um ");
                    rp.setMinValue("-999");
                    rp.setMaxValue("999");

                } else if (i == 520) {
                    rp.setParaCode("131");
                    rp.setParaName("FrameCassetteSelect");
                    rp.setSetValue(mathChange(transferOx(b[i] + "," + b[i + 1] + "," + b[i + 2] + "," + b[i + 3])));
                    rp.setParaMeasure("");
                    rp.setMinValue("0");
                    rp.setMaxValue("1");

                } else if (i == 528) {
                    rp.setParaCode("133");
                    rp.setParaName("BgPeelStopPosition");
                    rp.setSetValue(transferOx(b[i] + "," + b[i + 1] + "," + b[i + 2] + "," + b[i + 3]));
                    rp.setParaMeasure("mm");
                    rp.setMinValue("-50.0");
                    rp.setMaxValue("400.0");

                } else if (i == 532) {
                    rp.setParaCode("134");
                    rp.setParaName("BgPeelStopSpeed");
                    rp.setSetValue(mathChange(transferOx(b[i] + "," + b[i + 1] + "," + b[i + 2] + "," + b[i + 3])));
                    rp.setParaMeasure("mm/s");
                    rp.setMinValue("1");
                    rp.setMaxValue("300");

                } else if (i == 536) {
                    rp.setParaCode("135");
                    rp.setParaName("RESERVED");
                    rp.setSetValue(transferOx(b[i] + "," + b[i + 1] + "," + b[i + 2] + "," + b[i + 3]));
                    rp.setParaMeasure("");
                    rp.setMinValue("");
                    rp.setMaxValue("");

                } else if (i == 540) {
                    rp.setParaCode("136");
                    rp.setParaName("BCP Select");
                    rp.setSetValue(mathChange(transferOx(b[i] + "," + b[i + 1] + "," + b[i + 2] + "," + b[i + 3])));
                    rp.setParaMeasure("");
                    rp.setMinValue("0");
                    rp.setMaxValue("1");

                } else if (i == 544) {
                    rp.setParaCode("137");
                    rp.setParaName("OCR Select");
                    rp.setSetValue(mathChange(transferOx(b[i] + "," + b[i + 1] + "," + b[i + 2] + "," + b[i + 3])));
                    rp.setParaMeasure("");
                    rp.setMinValue("0");
                    rp.setMaxValue("1");

                } else if (i == 560) {
                    rp.setParaCode("141");
                    rp.setParaName("NoWaferMode");
                    rp.setSetValue(mathChange(transferOx(b[i] + "," + b[i + 1] + "," + b[i + 2] + "," + b[i + 3])));
                    rp.setParaMeasure("");
                    rp.setMinValue("0");
                    rp.setMaxValue("1");

                } else if (i == 564) {
                    rp.setParaCode("142");
                    rp.setParaName("PreCut Select");
                    rp.setSetValue(mathChange(transferOx(b[i] + "," + b[i + 1] + "," + b[i + 2] + "," + b[i + 3])));
                    rp.setParaMeasure("");
                    rp.setMinValue("0");
                    rp.setMaxValue("2");

                } else if (i == 568) {
                    rp.setParaCode("143");
                    rp.setParaName("DcApplyRate");
                    rp.setSetValue(mathChange(transferOx(b[i] + "," + b[i + 1] + "," + b[i + 2] + "," + b[i + 3])));
                    rp.setParaMeasure("mm/s*s");
                    rp.setMinValue("5");
                    rp.setMaxValue("700");

                } else if (i == 572) {
                    rp.setParaCode("144");
                    rp.setParaName("DcApplyHspd");
                    rp.setSetValue(mathChange(transferOx(b[i] + "," + b[i + 1] + "," + b[i + 2] + "," + b[i + 3])));
                    rp.setParaMeasure("mm/s");
                    rp.setMinValue("1");
                    rp.setMaxValue("160");

                } else if (i == 576) {
                    rp.setParaCode("145");
                    rp.setParaName("DcApplyLspd");
                    rp.setSetValue(mathChange(transferOx(b[i] + "," + b[i + 1] + "," + b[i + 2] + "," + b[i + 3])));
                    rp.setParaMeasure("mm/s");
                    rp.setMinValue("1");
                    rp.setMaxValue("160");

                } else if (i == 580) {
                    rp.setParaCode("146");
                    rp.setParaName("ApplySpeedChangePoint");
                    rp.setSetValue(transferOx(b[i] + "," + b[i + 1] + "," + b[i + 2] + "," + b[i + 3]));
                    rp.setParaMeasure("mm");
                    rp.setMinValue("-250.0");
                    rp.setMaxValue("200.0");

                } else if (i == 584) {
                    rp.setParaCode("147");
                    rp.setParaName("DisplacementSensorSelect");
                    rp.setSetValue(mathChange(transferOx(b[i] + "," + b[i + 1] + "," + b[i + 2] + "," + b[i + 3])));
                    rp.setParaMeasure("0~1");
                    rp.setMinValue("0");
                    rp.setMaxValue("1");

                } else if (i == 588) {
                    rp.setParaCode("148");
                    rp.setParaName("RESERVE");
                    rp.setSetValue(transferOx(b[i] + "," + b[i + 1] + "," + b[i + 2] + "," + b[i + 3]));
                    rp.setParaMeasure("");
                    rp.setMinValue("");
                    rp.setMaxValue("");

                } else if (i == 592) {
                    rp.setParaCode("149");
                    rp.setParaName("RESERVE");
                    rp.setSetValue(transferOx(b[i] + "," + b[i + 1] + "," + b[i + 2] + "," + b[i + 3]));
                    rp.setParaMeasure("");
                    rp.setMinValue("");
                    rp.setMaxValue("");

                } else if (i == 596) {
                    rp.setParaCode("150");
                    rp.setParaName("RESERVE");
                    rp.setSetValue(transferOx(b[i] + "," + b[i + 1] + "," + b[i + 2] + "," + b[i + 3]));
                    rp.setParaMeasure("");
                    rp.setMinValue("");
                    rp.setMaxValue("");

                } else if (i == 600) {
                    rp.setParaCode("151");
                    rp.setParaName("RESERVE");
                    rp.setSetValue(transferOx(b[i] + "," + b[i + 1] + "," + b[i + 2] + "," + b[i + 3]));
                    rp.setParaMeasure("");
                    rp.setMinValue("");
                    rp.setMaxValue("");

                } else if (i == 604) {
                    rp.setParaCode("152");
                    rp.setParaName("RESERVE");
                    rp.setSetValue(mathChange(transferOx(b[i] + "," + b[i + 1] + "," + b[i + 2] + "," + b[i + 3])));
                    rp.setParaMeasure("");
                    rp.setMinValue("0");
                    rp.setMaxValue("");

                } else if (i == 608) {
                    rp.setParaCode("153");
                    rp.setParaName("WaferPreheatTime");
                    rp.setSetValue(mathChange(transferOx(b[i] + "," + b[i + 1] + "," + b[i + 2] + "," + b[i + 3])));
                    rp.setParaMeasure("0.1s");
                    rp.setMinValue("0");
                    rp.setMaxValue("999");

                } else if (i == 612) {
                    rp.setParaCode("154");
                    rp.setParaName("WaferBlowTime");
                    rp.setSetValue(mathChange(transferOx(b[i] + "," + b[i + 1] + "," + b[i + 2] + "," + b[i + 3])));
                    rp.setParaMeasure("0.1s");
                    rp.setMinValue("0");
                    rp.setMaxValue("999");

                } else if (i == 616) {
                    rp.setParaCode("155");
                    rp.setParaName("RESERVE");
                    rp.setSetValue(transferOx(b[i] + "," + b[i + 1] + "," + b[i + 2] + "," + b[i + 3]));
                    rp.setParaMeasure("");
                    rp.setMinValue("");
                    rp.setMaxValue("");

                } else if (i == 620) {
                    rp.setParaCode("156");
                    rp.setParaName("PeelTapeFeedLength");
                    rp.setSetValue(mathChange(transferOx(b[i] + "," + b[i + 1] + "," + b[i + 2] + "," + b[i + 3])));
                    rp.setParaMeasure("0.1mm");
                    rp.setMinValue("0");
                    rp.setMaxValue("999");

                } else if (i == 624) {
                    rp.setParaCode("157");
                    rp.setParaName("PeelTapeWindLength(Peeling Start)");
                    rp.setSetValue(mathChange(transferOx(b[i] + "," + b[i + 1] + "," + b[i + 2] + "," + b[i + 3])));
                    rp.setParaMeasure("0.1mm");
                    rp.setMinValue("0");
                    rp.setMaxValue("999");

                } else if (i == 628) {
                    rp.setParaCode("158");
                    rp.setParaName("PeelTapeWindLength(Peeling Middle)");
                    rp.setSetValue(mathChange(transferOx(b[i] + "," + b[i + 1] + "," + b[i + 2] + "," + b[i + 3])));
                    rp.setParaMeasure("0.1mm");
                    rp.setMinValue("0");
                    rp.setMaxValue("9999");

                } else if (i == 632) {
                    rp.setParaCode("159");
                    rp.setParaName("PeelTapeWindLength(Peeling End)");
                    rp.setSetValue(mathChange(transferOx(b[i] + "," + b[i + 1] + "," + b[i + 2] + "," + b[i + 3])));
                    rp.setParaMeasure("0.1mm");
                    rp.setMinValue("0");
                    rp.setMaxValue("999");

                } else if (i == 636) {
                    rp.setParaCode("160");
                    rp.setParaName("PeelTapeWindSpeed");
                    rp.setSetValue(mathChange(transferOx(b[i] + "," + b[i + 1] + "," + b[i + 2] + "," + b[i + 3])));
                    rp.setParaMeasure("1mm");
                    rp.setMinValue("0");
                    rp.setMaxValue("999");

                } else if (i == 644) {
                    rp.setParaCode("162");
                    rp.setParaName("BgPeelBarPreheatTime");
                    rp.setSetValue(mathChange(transferOx(b[i] + "," + b[i + 1] + "," + b[i + 2] + "," + b[i + 3])));
                    rp.setParaMeasure("0.1s");
                    rp.setMinValue("0");
                    rp.setMaxValue("999");

                } else if (i == 1024) {
                    rp.setParaCode("257");
                    rp.setParaName("Recipe Name");
                    String aa = "";
                    for (int n = 1024; n < 1063; n++) {
                        String ss = b[n] + " ";
                        String[] chars = ss.split(" ");
                        for (int y = 0; y < chars.length; y++) {
                            char ch = (char) Integer.parseInt(chars[y]);
                            String strStringType = String.valueOf(ch);
                            aa = aa + strStringType;
                        }
                    }
                    rp.setSetValue(aa);
                    rp.setParaMeasure("");
                    rp.setMinValue("");
                    rp.setMaxValue("");

                } else if (i == 1584) {
                    s = mathChange(transferOx(b[i] + "," + b[i + 1] + "," + b[i + 2] + "," + b[i + 3]));
                    rp.setParaCode("397");
                    rp.setParaName("Port type");
                    if ("0".equals(s)) {
                        rp.setSetValue("FOUP(MAPING)");
                    } else if ("1".equals(s)) {
                        rp.setSetValue("FOUP(NO MAPPING)");
                    } else if ("2".equals(s)) {
                        rp.setSetValue("OPEN(MAPPING)/");
                    } else if ("3".equals(s)) {
                        rp.setSetValue("OPEN(NO MAPPING)");
                    } else if ("4".equals(s)) {
                        rp.setSetValue("FOSB(MAPPING) ONLY12'");
                    } else if ("5".equals(s)) {
                        rp.setSetValue("FOSB(NO MAPPING) ONLY12'");
                    } else if ("6".equals(s)) {
                        rp.setSetValue("DSC(NO MAPPING)ONLY8'");
                    } else {
                        rp.setSetValue("数据有误");
                    }
                    rp.setParaMeasure("");
                    rp.setMinValue("");
                    rp.setMaxValue("");

                } else if (i == 1588) {
                    s = mathChange(transferOx(b[i] + "," + b[i + 1] + "," + b[i + 2] + "," + b[i + 3]));
                    rp.setParaCode("398");
                    rp.setParaName("Cassette size");
                    if ("0".equals(s)) {
                        rp.setSetValue("8 inch");
                    } else if ("1".equals(s)) {
                        rp.setSetValue("12 inch");
                    } else {
                        logger.info("无此数据");
                    }
                    rp.setParaMeasure("0/1");
                    rp.setMinValue("");
                    rp.setMaxValue("");

                }
                if (rp.getParaCode() != null && !"NOT USED".equals(rp.getParaName()) && !"".equals(rp.getParaName())
                        && !":".equals(rp.getParaName()) && !"RESERVED".equals(rp.getParaName())
                        && !"RESERVE".equals(rp.getParaName())) {
                    list.add(rp);
                }
            }
//                // 此循环是在表中插入从原PPBODY文件获取的LIST数据
//                for (int j = 0; j < list.size(); j++) {
//                    Label label1 = new Label(0, j + 1, list.get(j).getId());
//                    ws.addCell(label1);
//                    Label label2 = new Label(1, j + 1, list.get(j).getName());
//                    ws.addCell(label2);
//                    Label label3 = new Label(2, j + 1, list.get(j).getReal());
//                    ws.addCell(label3);
//                    Label label4 = new Label(3, j + 1, list.get(j).getUnit());
//                    ws.addCell(label4);
//                    Label label5 = new Label(4, j + 1, list.get(j).getEcmin());
//                    ws.addCell(label5);
//                    Label label6 = new Label(5, j + 1, list.get(j).getEcmax());
//                    ws.addCell(label6);
//                }
            // 插入rp文件完成后清空LIST，继续下次循环
//                list.clear();
            br.close();
//                wwb.write();
//                wwb.close();

        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        return list;
    }

    public static String[] getFileName(String path) {
        File file = new File(path);
        String[] fileName = file.list();
        return fileName;
    }

    public static void main(String[] args) {

        List map = getAllValue("C:\\Users\\luosy\\Desktop\\pg3000 recipe\\pg3000 recipe\\12INCH-250UM-8310-NP-8K-D175-FC");
        Map map1 = transferFromList(map);
        List<RecipePara> recipeParas = transferFromDB(map1);
        for (RecipePara value : recipeParas) {
            System.out.println(value.getParaCode() + "---" + value.getSetValue());
        }
    }

    private static List getAllValue(String filePath) {
        List<String> list = new LinkedList();
        BufferedReader br = null;
        try {
            String cfgline = null;
            File cfgfile = new File(filePath);
            br = new BufferedReader(new InputStreamReader(new FileInputStream(cfgfile), "GBK"));
            while ((cfgline = br.readLine()) != null) {
                String value = "";
                if (cfgline.contains("L1") || cfgline.contains("L2") || cfgline.contains("[") || cfgline.contains("]")) {
                    continue;
                }
                String[] cfg = cfgline.split(":");
                if (cfg.length > 1) {
                    value = cfg[1];
                }
                value = value.replaceAll(" ", "");
                list.add(value);
            }
            br.close();
            return list;
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return null;
        }
    }

    private static Map transferFromList(List valueList) {
        Map paraMap = new HashMap();
        if (valueList != null && !valueList.isEmpty()) {
            for (int i = 1; i < valueList.size(); i++) {
                String key = String.valueOf(valueList.get(i));
                String value = String.valueOf(valueList.get(i + 1));
                if (value.contains(",")) {
                    String[] values = value.split(",");
                    for (int j = 0; j < values.length; j++) {
                        paraMap.put(key + j, values[j]);
                    }
                } else {
                    paraMap.put(key, value);
                }
                i = i + 1;
            }
        }
        return paraMap;
    }

    private static List<RecipePara> transferFromDB(Map paraMap) {
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        RecipeService recipeService = new RecipeService(sqlSession);
        List<RecipeTemplate> recipeTemplates = recipeService.searchRecipeTemplateByDeviceTypeCode("ACCRETECHPG3000RMX", "RecipePara");
        sqlSession.close();
        List<RecipePara> recipeParaList = new ArrayList<>();
        if (recipeTemplates != null && !recipeTemplates.isEmpty()) {
            for (RecipeTemplate recipeTemplate : recipeTemplates) {
                RecipePara recipePara = new RecipePara();
                recipePara.setParaCode(recipeTemplate.getParaCode());
                recipePara.setParaName(recipeTemplate.getParaName());
                recipePara.setParaShotName(recipeTemplate.getParaShotName());
                recipePara.setSetValue(paraMap.get(recipeTemplate.getParaCode()).toString());
                recipePara.setParaMeasure(recipeTemplate.getParaUnit());
                recipePara.setParaShotName(recipeTemplate.getParaShotName());
                recipeParaList.add(recipePara);
            }
        }
        return recipeParaList;
    }

    public static List<RecipePara> transferPG3000RCP(String recipePath) {
        List<RecipePara> recipeParas = new ArrayList<>();
        List valueList = getAllValue(recipePath);
        if (valueList != null && !valueList.isEmpty()) {
            Map map = transferFromList(valueList);
            recipeParas = transferFromDB(map);
        }
        return recipeParas;
    }
}
