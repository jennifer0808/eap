/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.secsLayer.resolver;


import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.biz.recipe.domain.RecipeTemplate;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import org.apache.ibatis.session.SqlSession;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class AWD300TXRecipeUtil {

    // 将乱码文件转成BYTE数组
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
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return buffer;
    }

    // 1byte、2byte和4byte的转换方法
    public static String tranOx(String num) {
        String[] nums = num.split(",");
        String oxStr = "";
        if (nums.length == 1) {
            String numOx1 = Integer.toHexString(Integer.parseInt(nums[0]));
            String oxStr1 = "";
            if (Integer.parseInt(nums[0]) < 0) {
                oxStr1 = numOx1.substring(6);
            } else {
                oxStr1 = numOx1;
            }
            oxStr = oxStr1;
        } else if (nums.length == 2) {
            String numOx1 = Integer.toHexString(Integer.parseInt(nums[0]));
            String numOx2 = Integer.toHexString(Integer.parseInt(nums[1]));
            String oxStr1 = "";
            if (Integer.parseInt(nums[0]) < 0) {
                oxStr1 = numOx1.substring(6);
            } else {
                oxStr1 = numOx1;
            }
            String oxStr2 = "";
            if (Integer.parseInt(nums[1]) < 0) {
                oxStr2 = numOx2.substring(6, numOx2.length());
            } else {
                oxStr2 = numOx2;
            }
            oxStr = oxStr1 + oxStr2;

        } else {
            String numOx1 = Integer.toHexString(Integer.parseInt(nums[0]));
            String numOx2 = Integer.toHexString(Integer.parseInt(nums[1]));
            String numOx3 = Integer.toHexString(Integer.parseInt(nums[2]));
            String numOx4 = Integer.toHexString(Integer.parseInt(nums[3]));

            String oxStr1 = "";
            if (Integer.parseInt(nums[0]) < 0) {
                oxStr1 = numOx1.substring(6);
            } else {
                oxStr1 = numOx1;
            }
            String oxStr2 = "";
            if (Integer.parseInt(nums[1]) < 0) {
                oxStr2 = numOx2.substring(6, numOx2.length());
            } else {
                oxStr2 = numOx2;
            }
            String oxStr3 = "";
            if (Integer.parseInt(nums[2]) < 0) {
                oxStr3 = numOx3.substring(6, numOx3.length());
            } else {
                oxStr3 = numOx3;

            }
            String oxStr4 = "";
            if (Integer.parseInt(nums[3]) < 0) {
                oxStr4 = numOx4.substring(6, numOx4.length());
            } else {
                oxStr4 = numOx4;
            }
            oxStr = oxStr1 + oxStr2 + oxStr3 + oxStr4;

        }
        return Integer.valueOf(oxStr, 16).toString();
    }

    //16进制转10进制  
    public static int HexToInt(String strHex) {
        int nResult = 0;
        if (!IsHex(strHex)) {
            return nResult;
        }
        String str = strHex.toUpperCase();
        if (str.length() > 2) {
            if (str.charAt(0) == '0' && str.charAt(1) == 'X') {
                str = str.substring(2);
            }
        }
        int nLen = str.length();
        for (int i = 0; i < nLen; ++i) {
            char ch = str.charAt(nLen - i - 1);
            try {
                nResult += (GetHex(ch) * GetPower(16, i));
            } catch (Exception e) {
                // TODO Auto-generated catch block  
                e.printStackTrace();
            }
        }
        return nResult;
    }

    //计算16进制对应的数值  
    public static int GetHex(char ch) throws Exception {
        if (ch >= '0' && ch <= '9') {
            return (int) (ch - '0');
        }
        if (ch >= 'a' && ch <= 'f') {
            return (int) (ch - 'a' + 10);
        }
        if (ch >= 'A' && ch <= 'F') {
            return (int) (ch - 'A' + 10);
        }
        throw new Exception("error param");
    }

    //计算幂  
    public static int GetPower(int nValue, int nCount) throws Exception {
        if (nCount < 0) {
            throw new Exception("nCount can't small than 1!");
        }
        if (nCount == 0) {
            return 1;
        }
        int nSum = 1;
        for (int i = 0; i < nCount; ++i) {
            nSum = nSum * nValue;
        }
        return nSum;
    }
    //判断是否是16进制数  

    public static boolean IsHex(String strHex) {
        int i = 0;
        if (strHex.length() > 2) {
            if (strHex.charAt(0) == '0' && (strHex.charAt(1) == 'X' || strHex.charAt(1) == 'x')) {
                i = 2;
            }
        }
        for (; i < strHex.length(); ++i) {
            char ch = strHex.charAt(i);
            if ((ch >= '0' && ch <= '9') || (ch >= 'A' && ch <= 'F') || (ch >= 'a' && ch <= 'f')) {
                continue;
            }
            return false;
        }
        return true;
    }

    // 将byte数组导入到excel表中
    /**
     * @param filePath
     * @param savedFilePath
     */
    @SuppressWarnings("null")
    public static List<RecipePara> TSKRecipeTran(String filePath) {
        List<RecipePara> recipeParas = new ArrayList<>();
//		WritableWorkbook wwb = null;
        BufferedReader br = null;
        try {
            // getFileName()方法为获取filePath路径下的所有文件名的字符串数组
//			String[] fileNames = getFileName(filePath);
//			for (int j1 = 0; j1 < fileNames.length; j1++) {
//				String saveExcelName = fileNames[j1];
            File cfgfile = new File(filePath);
            br = new BufferedReader(new InputStreamReader(new FileInputStream(cfgfile), "GBK"));
//				// 获取文件名中最后一个“ . ”出现的位置
//				int dot = saveExcelName.lastIndexOf(".");
//				// 判断是否有后文件后缀，如果有，就截取去除文件后缀后的文件名
//				if ((dot > -1) && (dot < saveExcelName.length())) {
//					saveExcelName = saveExcelName.substring(0, dot);
//				}
//				File sFile = new File(savedFilePath + saveExcelName + ".xls");
//				// 判断文件是否已经解析过，如果解析过，就继续下一轮循环
//				if (sFile.exists() && sFile.isFile()) {
//					continue;
//				}
//				OutputStream os = new FileOutputStream(savedFilePath + saveExcelName + ".xls");
//				wwb = Workbook.createWorkbook(os);
//				jxl.write.WritableSheet ws = wwb.createSheet("Sheet 1", 0);
//				// 在EXCEL表中的（0，0）坐标写入数据“KEY”，下面四行代码为定义两列的列名
//				Label l1 = new Label(0, 0, "id");
//				ws.addCell(l1);
//				Label l2 = new Label(1, 0, "name");
//				ws.addCell(l2);
//				Label l3 = new Label(2, 0, "unit");
//				ws.addCell(l3);
//				Label l4 = new Label(3, 0, "real");
//				ws.addCell(l4);
//				Label l5 = new Label(4, 0, "ecmin");
//				ws.addCell(l5);
//				Label l6 = new Label(5, 0, "ecmax");
//				ws.addCell(l6);
//				Label l7 = new Label(6, 0, "description");
//				ws.addCell(l7);

            String xx = cfgfile.getAbsolutePath();
            byte[] b = trans(xx);
            String[] ss = new String[b.length];
            for (int k = 0; k < b.length; k++) {
                ss[k] = Integer.toHexString(b[k]);
                if (b[k] < 0) {
                    ss[k] = ss[k].substring(6);
                }
                if (ss[k].length() == 1) {
                    ss[k] = "0" + ss[k];
                }
            }

            Map resultMap = new HashMap();
            for (int m = 0; m < 4132; m++) {
                int num = 1568 + 48 * m;
                System.out.print(m + 1);
                System.out.print(" " + num);
                String key = "0x" + ss[num + 3] + ss[num + 2] + ss[num + 1] + ss[num];
                String valueTemp = ss[num + 7] + ss[num + 6] + ss[num + 5] + ss[num + 4];
                System.out.print(" " + key);
                System.out.println(" " + valueTemp);
//                long value = Integer.parseInt(valueTemp, 16);
                int value = HexToInt(valueTemp);
                String setValue = "";
                if (key.equalsIgnoreCase("0x00010001")) {
                    switch (value) {
                        case 0:
                            setValue = "Circle";
                            break;
                        case 1:
                            setValue = "Rectangle";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x00010004")) {
                    switch (value) {
                        case 0:
                            setValue = "0 Deg.";
                            break;
                        case 1:
                            setValue = "90 Deg.";
                            break;
                        case 2:
                            setValue = "180 Deg.";
                            break;
                        case 3:
                            setValue = "270 Deg.";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x00010008")) {
                    switch (value) {
                        case 0:
                            setValue = "CH2 -> CH1";
                            break;
                        case 1:
                            setValue = "CH1 Only";
                            break;
                        case 2:
                            setValue = "CH2 Only";
                            break;
                        case 3:
                            setValue = "CH1 -> CH2";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x00010009")) {
                    switch (value) {
                        case 0:
                            setValue = "Normal";
                            break;
                        case 1:
                            setValue = "Index-N";
                            break;
                        case 2:
                            setValue = "SUB-I";
                            break;
                        case 3:
                            setValue = "WSUB-I";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x0001000A")) {
                    switch (value) {
                        case 0:
                            setValue = "Same as CH1";
                            break;
                        case 1:
                            setValue = "Normal";
                            break;
                        case 2:
                            setValue = "Index-N";
                            break;
                        case 3:
                            setValue = "SUB-I";
                            break;
                        case 4:
                            setValue = "WSUB-I";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x0001000B")) {
                    switch (value) {
                        case 0:
                            setValue = "SP1";
                            break;
                        case 1:
                            setValue = "SP2";
                            break;
                        case 2:
                            setValue = "STEP";
                            break;
                        case 3:
                            setValue = "MEETING";
                            break;
                        case 4:
                            setValue = "MEETING2";
                            break;
                        case 5:
                            setValue = "COUPLE";
                            break;
                        case 6:
                            setValue = "COUPLE2";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x0001000C")) {
                    switch (value) {
                        case 0:
                            setValue = "Same as CH1";
                            break;
                        case 1:
                            setValue = "SP1";
                            break;
                        case 2:
                            setValue = "SP2";
                            break;
                        case 3:
                            setValue = "STEP";
                            break;
                        case 4:
                            setValue = "MEETING";
                            break;
                        case 5:
                            setValue = "MEETING2";
                            break;
                        case 6:
                            setValue = "COUPLE";
                            break;
                        case 7:
                            setValue = "COUPLE2";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x0001000D")) {
                    switch (value) {
                        case 0:
                            setValue = "Front -> Rear";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x0001000E")) {
                    switch (value) {
                        case 0:
                            setValue = "Front -> Rear";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x0001001B")) {
                    switch (value) {
                        case 0:
                            setValue = "Cutting area to wafer size";
                            break;
                        case 1:
                            setValue = "Cut with line No.";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x0001001C")) {
                    switch (value) {
                        case 0:
                            setValue = "No Phase";
                            break;
                        case 1:
                            setValue = "Phase1";
                            break;
                        case 2:
                            setValue = "Phase2";
                            break;
                        case 3:
                            setValue = "Phase3";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x00010027")) {
                    switch (value) {
                        case 0:
                            setValue = "Cut with DATA";
                            break;
                        case 1:
                            setValue = "Cut with MAP";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x00010028")) {
                    switch (value) {
                        case 0:
                            setValue = "Cut with DATA";
                            break;
                        case 1:
                            setValue = "Cut with MAP";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x00010029")) {
                    switch (value) {
                        case 0:
                            setValue = "Cut round shape";
                            break;
                        case 1:
                            setValue = "Cut orifra shaped";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x0001002A")) {
                    switch (value) {
                        case 0:
                            setValue = "Single phase";
                            break;
                        case 1:
                            setValue = "Multi-phase - Selvage cut";
                            break;
                        case 2:
                            setValue = "Multi-phase - Inner cut";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x00020001")) {
                    switch (value) {
                        case 0:
                            setValue = "Disable";
                            break;
                        case 1:
                            setValue = "Others";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x00020002")) {
                    switch (value) {
                        case 0:
                            setValue = "Disable";
                            break;
                        case 1:
                            setValue = "1st wafer of lot";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x00020003")) {
                    switch (value) {
                        case 0:
                            setValue = "Disable";
                            break;
                        case 1:
                            setValue = "1st wafer of lot";
                            break;
                        case 2:
                            setValue = "1st wafer and Kerf check failed";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x00020004")) {
                    switch (value) {
                        case 0:
                            setValue = "OFF";
                            break;
                        case 1:
                            setValue = "Air Curtain";
                            break;
                        case 2:
                            setValue = "water Curtain";
                            break;
                        case 3:
                            setValue = "Air+Water";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x00020005")) {
                    switch (value) {
                        case 0:
                            setValue = "Only Water ON";
                            break;
                        case 1:
                            setValue = "Cutting motion";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x0002000C")) {
                    switch (value) {
                        case 0:
                            setValue = "Individual setting by CH";
                            break;
                        case 1:
                            setValue = "Same setting to both CH";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x0002000D")) {
                    switch (value) {
                        case 0:
                            setValue = "Standard";
                            break;
                        case 1:
                            setValue = "No frame clamping";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x0002000F")) {
                    switch (value) {
                        case 0:
                            setValue = "Enable";
                            break;
                        case 1:
                            setValue = "Disable";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x00020010")) {
                    switch (value) {
                        case 0:
                            setValue = "Cutting position";
                            break;
                        case 1:
                            setValue = "Unload position";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x00020011")) {
                    switch (value) {
                        case 0:
                            setValue = "Disable";
                            break;
                        case 1:
                            setValue = "Enable";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x00020012")) {
                    switch (value) {
                        case 0:
                            setValue = "Disable";
                            break;
                        case 1:
                            setValue = "Enable";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x00020013")) {
                    switch (value) {
                        case 0:
                            setValue = "Product Wafer";
                            break;
                        case 1:
                            setValue = "Pre Cut Wafer";
                            break;
                        case 2:
                            setValue = "Stage Dress Board";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x00020017")) {
                    switch (value) {
                        case 0:
                            setValue = "OFF";
                            break;
                        case 1:
                            setValue = "For each work";
                            break;
                        case 2:
                            setValue = "End of phase";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x00020018")) {
                    switch (value) {
                        case 0:
                            setValue = "Disable";
                            break;
                        case 1:
                            setValue = "When phase ends";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x00020019")) {
                    switch (value) {
                        case 0:
                            setValue = "Disable";
                            break;
                        case 1:
                            setValue = "Enable";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x00020020")) {
                    switch (value) {
                        case 0:
                            setValue = "Disable";
                            break;
                        case 1:
                            setValue = "Enable";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x00020021")) {
                    switch (value) {
                        case 0:
                            setValue = "frame";
                            break;
                        case 1:
                            setValue = "Every work";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x00030005")) {
                    switch (value) {
                        case 0:
                            setValue = "Disable";
                            break;
                        case 1:
                            setValue = "Slow-In";
                            break;
                        case 2:
                            setValue = "Slow-In and Out";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x0003000C")) {
                    switch (value) {
                        case 0:
                            setValue = "Disable";
                            break;
                        case 1:
                            setValue = "Every line";
                            break;
                        case 2:
                            setValue = "Paused cut line only";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x00030058")) {
                    switch (value) {
                        case 0:
                            setValue = "Disable";
                            break;
                        case 1:
                            setValue = "Enable";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x00030059")) {
                    switch (value) {
                        case 0:
                            setValue = "Disable";
                            break;
                        case 1:
                            setValue = "Enable";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x0003005E")) {
                    switch (value) {
                        case 0:
                            setValue = "OFF";
                            break;
                        case 1:
                            setValue = "Valve+Water OFF";
                            break;
                        case 2:
                            setValue = "Water OFF";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x00040001")) {
                    switch (value) {
                        case 0:
                            setValue = "Down Cut";
                            break;
                        case 1:
                            setValue = "Down-Up Cut";
                            break;
                        case 2:
                            setValue = "Up Cut";
                            break;
                        case 3:
                            setValue = "Up-Down Cut";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x00040003")) {
                    switch (value) {
                        case 0:
                            setValue = "Cut-remain";
                            break;
                        case 1:
                            setValue = "Cut-in";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x00040004")) {
                    switch (value) {
                        case 0:
                            setValue = "Cut-remain";
                            break;
                        case 1:
                            setValue = "Cut-in";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x0004000E")) {
                    switch (value) {
                        case 0:
                            setValue = "Down Cut";
                            break;
                        case 1:
                            setValue = "Down-Up Cut";
                            break;
                        case 2:
                            setValue = "Up Cut";
                            break;
                        case 3:
                            setValue = "Up-Down Cut";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x00040010")) {
                    switch (value) {
                        case 0:
                            setValue = "Cut-remain";
                            break;
                        case 1:
                            setValue = "Cut-in";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x00040011")) {
                    switch (value) {
                        case 0:
                            setValue = "Cut-remain";
                            break;
                        case 1:
                            setValue = "Cut-in";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x00040053")) {
                    switch (value) {
                        case 0:
                            setValue = "Cut with Index Data";
                            break;
                        case 1:
                            setValue = "Cut with Type1 Data";
                            break;
                        case 2:
                            setValue = "Cut with Type2 Data";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x00040054")) {
                    switch (value) {
                        case 0:
                            setValue = "Cut with Index Data";
                            break;
                        case 1:
                            setValue = "Cut with Type1 Data";
                            break;
                        case 2:
                            setValue = "Cut with Type2 Data";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x00050005")) {
                    switch (value) {
                        case 0:
                            setValue = "Standard";
                            break;
                        case 1:
                            setValue = "High mag./ Ring light";
                            break;
                        case 2:
                            setValue = "Low mag. / Coaxial light";
                            break;
                        case 3:
                            setValue = "Low mag./ Ring light";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x00050007")) {
                    switch (value) {
                        case 0:
                            setValue = "Pink";
                            break;
                        case 1:
                            setValue = "Yellow";
                            break;
                        case 2:
                            setValue = "White";
                            break;
                        case 3:
                            setValue = "Green";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x00050008")) {
                    switch (value) {
                        case 1:
                            setValue = "Alignment cross";
                            break;
                        case 2:
                            setValue = "Pixel frame Upper-Right";
                            break;
                        case 3:
                            setValue = "Pixel frame Lower-Left";
                            break;
                        case 4:
                            setValue = "Rotation center cross";
                            break;
                        case 5:
                            setValue = "IDLE cross line";
                            break;
                        case 6:
                            setValue = "Cross registration frame";
                            break;
                        case 7:
                            setValue = "Model registration frame";
                            break;
                        case 8:
                            setValue = "Kerf check marker";
                            break;
                        case 9:
                            setValue = "Env. registration frame";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x00050009")) {
                    switch (value) {
                        case 1:
                            setValue = "Alignment cross";
                            break;
                        case 2:
                            setValue = "Pixel frame Upper-Right";
                            break;
                        case 3:
                            setValue = "Pixel frame Lower-Left";
                            break;
                        case 4:
                            setValue = "Rotation center cross";
                            break;
                        case 5:
                            setValue = "IDLE cross line";
                            break;
                        case 6:
                            setValue = "Cross registration frame";
                            break;
                        case 7:
                            setValue = "Model registration frame";
                            break;
                        case 8:
                            setValue = "Kerf check marker";
                            break;
                        case 9:
                            setValue = "Env. registration frame";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x0005000D")) {
                    switch (value) {
                        case 0:
                            setValue = "Disable";
                            break;
                        case 1:
                            setValue = "Enable";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x0005000E")) {
                    switch (value) {
                        case 0:
                            setValue = "Pattern matching only";
                            break;
                        case 1:
                            setValue = "Alignment cross image";
                            break;
                        case 2:
                            setValue = "Pattern matching + street view";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x0005000F")) {
                    switch (value) {
                        case 0:
                            setValue = "OFF";
                            break;
                        case 1:
                            setValue = "ON";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x00050010")) {
                    switch (value) {
                        case 0:
                            setValue = "Cut with DATA";
                            break;
                        case 1:
                            setValue = "Cut with MAP";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x00050013")) {
                    switch (value) {
                        case 0:
                            setValue = "Block";
                            break;
                        case 1:
                            setValue = "Work";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x00050014")) {
                    switch (value) {
                        case 0:
                            setValue = "Block";
                            break;
                        case 1:
                            setValue = "Work";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x0005001B")) {
                    switch (value) {
                        case 0:
                            setValue = "Multi theta";
                            break;
                        case 1:
                            setValue = "Single theta";
                            break;
                        case 2:
                            setValue = "No line";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x0005001C")) {
                    switch (value) {
                        case 0:
                            setValue = "Multi theta";
                            break;
                        case 1:
                            setValue = "Single theta";
                            break;
                        case 2:
                            setValue = "No line";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x0005001D")) {
                    switch (value) {
                        case 0:
                            setValue = "Right";
                            break;
                        case 1:
                            setValue = "Left";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x0005001E")) {
                    switch (value) {
                        case 0:
                            setValue = "Right";
                            break;
                        case 1:
                            setValue = "Left";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x00050021")) {
                    switch (value) {
                        case 0:
                            setValue = "2 point";
                            break;
                        case 1:
                            setValue = "X Map";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x00050022")) {
                    switch (value) {
                        case 0:
                            setValue = "2 point";
                            break;
                        case 1:
                            setValue = "X Map";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x00050023")) {
                    switch (value) {
                        case 0:
                            setValue = "Do not execute";
                            break;
                        case 1:
                            setValue = "Execute";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x00050024")) {
                    switch (value) {
                        case 0:
                            setValue = "No Sub Model";
                            break;
                        case 1:
                            setValue = "Both side Sub model";
                            break;
                        case 2:
                            setValue = "One side Sub Model";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x00050025")) {
                    switch (value) {
                        case 0:
                            setValue = "No Sub Model";
                            break;
                        case 1:
                            setValue = "Both side Sub model";
                            break;
                        case 2:
                            setValue = "One side Sub Model";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x00050026")) {
                    switch (value) {
                        case 0:
                            setValue = "Disable";
                            break;
                        case 1:
                            setValue = "Enable(Update cross position)";
                            break;
                        case 2:
                            setValue = "Enable(Only interlock)";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x00050027")) {
                    switch (value) {
                        case 0:
                            setValue = "OFF";
                            break;
                        case 1:
                            setValue = "ON";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x00050028")) {
                    switch (value) {
                        case 0:
                            setValue = "OFF";
                            break;
                        case 1:
                            setValue = "ON";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x00050029")) {
                    switch (value) {
                        case 0:
                            setValue = "Disable";
                            break;
                        case 1:
                            setValue = "Enable";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x0005002A")) {
                    switch (value) {
                        case 0:
                            setValue = "Disable";
                            break;
                        case 1:
                            setValue = "Enable";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x0005002E")) {
                    switch (value) {
                        case 0:
                            setValue = "Disable";
                            break;
                        case 1:
                            setValue = "Enable";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x00060001")) {
                    switch (value) {
                        case 0:
                            setValue = "Multi Points";
                            break;
                        case 1:
                            setValue = "Alignment PASS";
                            break;
                        case 2:
                            setValue = "Board Type Alignment";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x00060002")) {
                    switch (value) {
                        case 0:
                            setValue = "High+Low";
                            break;
                        case 1:
                            setValue = "High mag. Only";
                            break;
                        case 2:
                            setValue = "Low mag. Only";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x00060003")) {
                    switch (value) {
                        case 0:
                            setValue = "CH1 -> CH2";
                            break;
                        case 1:
                            setValue = "CH1 Only";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x00060004")) {
                    switch (value) {
                        case 0:
                            setValue = "Standard";
                            break;
                        case 1:
                            setValue = "1st wafer Auto-Focus";
                            break;
                        case 2:
                            setValue = "1st wafer Auto-Focus/Bright";
                            break;
                        case 3:
                            setValue = "Every wafer Auto-Focus";
                            break;
                        case 4:
                            setValue = "Every wafer Auto-Focus/Bright";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x00060005")) {
                    switch (value) {
                        case 0:
                            setValue = "Disable";
                            break;
                        case 1:
                            setValue = "Auto-Retry";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x00060006")) {
                    switch (value) {
                        case 0:
                            setValue = "Disable";
                            break;
                        case 1:
                            setValue = "Enable";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x0006000F")) {
                    switch (value) {
                        case 0:
                            setValue = "Coaxial";
                            break;
                        case 1:
                            setValue = "Ring";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x00060010")) {
                    switch (value) {
                        case 0:
                            setValue = "OFF";
                            break;
                        case 1:
                            setValue = "Low";
                            break;
                        case 2:
                            setValue = "Middle";
                            break;
                        case 3:
                            setValue = "High";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x00060011")) {
                    switch (value) {
                        case 0:
                            setValue = "AUTO";
                            break;
                        case 1:
                            setValue = "Rough";
                            break;
                        case 2:
                            setValue = "Fine";
                            break;
                        case 3:
                            setValue = "Fine2";
                            break;
                        case 4:
                            setValue = "Fine3";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x00060012")) {
                    switch (value) {
                        case 0:
                            setValue = "OFF";
                            break;
                        case 1:
                            setValue = "ON";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x00060014")) {
                    switch (value) {
                        case 0:
                            setValue = "OFF";
                            break;
                        case 1:
                            setValue = "ON";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x00060015")) {
                    switch (value) {
                        case 0:
                            setValue = "OFF";
                            break;
                        case 1:
                            setValue = "Low";
                            break;
                        case 2:
                            setValue = "Middle";
                            break;
                        case 3:
                            setValue = "High";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x00060016")) {
                    switch (value) {
                        case 0:
                            setValue = "AUTO";
                            break;
                        case 1:
                            setValue = "Rough";
                            break;
                        case 2:
                            setValue = "Fine";
                            break;
                        case 3:
                            setValue = "Fine2";
                            break;
                        case 4:
                            setValue = "Fine3";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x00060017")) {
                    switch (value) {
                        case 0:
                            setValue = "OFF";
                            break;
                        case 1:
                            setValue = "ON";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x00060019")) {
                    switch (value) {
                        case 0:
                            setValue = "OFF";
                            break;
                        case 1:
                            setValue = "ON";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x0006001A")) {
                    switch (value) {
                        case 0:
                            setValue = "Disable";
                            break;
                        case 1:
                            setValue = "1st wafer of lot";
                            break;
                        case 2:
                            setValue = "Every Wafer";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x0006001B")) {
                    switch (value) {
                        case 0:
                            setValue = "None";
                            break;
                        case 1:
                            setValue = "1st wafer of lot";
                            break;
                        case 2:
                            setValue = "Every Wafer";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x0006001C")) {
                    switch (value) {
                        case 0:
                            setValue = "Disable";
                            break;
                        case 1:
                            setValue = "Measurement only at lot start";
                            break;
                        case 2:
                            setValue = "Auto Correction at lot start";
                            break;
                        case 3:
                            setValue = "Measurement only every wafer";
                            break;
                        case 4:
                            setValue = "Auto Correction every wafer";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x0006001D")) {
                    switch (value) {
                        case 0:
                            setValue = "Both CH";
                            break;
                        case 1:
                            setValue = "CH1";
                            break;
                        case 2:
                            setValue = "CH2";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x00060027")) {
                    switch (value) {
                        case 0:
                            setValue = "AUTO";
                            break;
                        case 1:
                            setValue = "Both model check";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x0006002E")) {
                    switch (value) {
                        case 0:
                            setValue = "TYPE1";
                            break;
                        case 1:
                            setValue = "TYPE2";
                            break;
                        case 2:
                            setValue = "TYPE3";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x00060033")) {
                    switch (value) {
                        case 0:
                            setValue = "Detected edge location";
                            break;
                        case 1:
                            setValue = "Smart edge location";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x0006003F")) {
                    switch (value) {
                        case 0:
                            setValue = "Lo mag only";
                            break;
                        case 1:
                            setValue = "Lo mag + Hi mag verification";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x00060040")) {
                    switch (value) {
                        case 0:
                            setValue = "OFF";
                            break;
                        case 1:
                            setValue = "Both channels";
                            break;
                        case 2:
                            setValue = "CH1 only";
                            break;
                        case 3:
                            setValue = "CH2 only";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x00060043")) {
                    switch (value) {
                        case 0:
                            setValue = "OFF";
                            break;
                        case 1:
                            setValue = "ON";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x00060044")) {
                    switch (value) {
                        case 0:
                            setValue = "Disable";
                            break;
                        case 1:
                            setValue = "Enable";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x00060045")) {
                    switch (value) {
                        case 0:
                            setValue = "Type 1";
                            break;
                        case 1:
                            setValue = "Type 2";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x00060047")) {
                    switch (value) {
                        case 0:
                            setValue = "Disable";
                            break;
                        case 1:
                            setValue = "Enable";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x0006004B")) {
                    switch (value) {
                        case 0:
                            setValue = "Do not execute";
                            break;
                        case 1:
                            setValue = "Only Y Correction";
                            break;
                        case 2:
                            setValue = "YT Correction";
                            break;
                        case 3:
                            setValue = "XMAP";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x0006004C")) {
                    switch (value) {
                        case 0:
                            setValue = "Execute with DATA";
                            break;
                        case 1:
                            setValue = "Execute with MAP";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x0006004D")) {
                    switch (value) {
                        case 0:
                            setValue = "Both CH";
                            break;
                        case 1:
                            setValue = "Only CH1";
                            break;
                        case 2:
                            setValue = "Only CH2";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x00060056")) {
                    switch (value) {
                        case 0:
                            setValue = "Retry";
                            break;
                        case 1:
                            setValue = "Do not retry";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x00060057")) {
                    switch (value) {
                        case 0:
                            setValue = "Do not execute";
                            break;
                        case 1:
                            setValue = "AF only for 1st point";
                            break;
                        case 2:
                            setValue = "AF every searching";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x0006005A")) {
                    switch (value) {
                        case 0:
                            setValue = "Do not execute";
                            break;
                        case 1:
                            setValue = "Only Y Correction";
                            break;
                        case 2:
                            setValue = "YT Correction";
                            break;
                        case 3:
                            setValue = "XMAP";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x00060061")) {
                    switch (value) {
                        case 0:
                            setValue = "Same time";
                            break;
                        case 1:
                            setValue = "Individual time";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x00060063")) {
                    switch (value) {
                        case 0:
                            setValue = "OFF";
                            break;
                        case 1:
                            setValue = "ON";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x00060064")) {
                    switch (value) {
                        case 0:
                            setValue = "OFF";
                            break;
                        case 1:
                            setValue = "ON";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x00060065")) {
                    switch (value) {
                        case 0:
                            setValue = "Execute at Main and Sub index";
                            break;
                        case 1:
                            setValue = "Execute at Main index";
                            break;
                        case 2:
                            setValue = "Execute at Sub index";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x00060066")) {
                    switch (value) {
                        case 0:
                            setValue = "Execute at Main and Sub index";
                            break;
                        case 1:
                            setValue = "Execute at Main index";
                            break;
                        case 2:
                            setValue = "Execute at Sub index";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x0006006B")) {
                    switch (value) {
                        case 0:
                            setValue = "OFF";
                            break;
                        case 1:
                            setValue = "ON";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x0006006C")) {
                    switch (value) {
                        case 0:
                            setValue = "OFF";
                            break;
                        case 1:
                            setValue = "ON";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x00060093")) {
                    switch (value) {
                        case 0:
                            setValue = "Standard";
                            break;
                        case 1:
                            setValue = "M1 AND M2";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x00060095")) {
                    switch (value) {
                        case 0:
                            setValue = "Standard";
                            break;
                        case 1:
                            setValue = "Low contrast";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x00060096")) {
                    switch (value) {
                        case 0:
                            setValue = "Standard";
                            break;
                        case 1:
                            setValue = "Enhanced";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x00060097")) {
                    switch (value) {
                        case 0:
                            setValue = "Standard";
                            break;
                        case 1:
                            setValue = "Low contrast";
                            break;
                        case 2:
                            setValue = "high sensitivity";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x00070002")) {
                    switch (value) {
                        case 0:
                            setValue = "Disable";
                            break;
                        case 1:
                            setValue = "Evaluation Only";
                            break;
                        case 2:
                            setValue = "Kerf center correction";
                            break;
                        case 3:
                            setValue = "Kerf center and Alignment cross";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x00070003")) {
                    switch (value) {
                        case 0:
                            setValue = "Disable";
                            break;
                        case 1:
                            setValue = "Evaluation Only";
                            break;
                        case 2:
                            setValue = "Kerf center correction";
                            break;
                        case 3:
                            setValue = "Kerf center and Alignment cross";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x00070005")) {
                    switch (value) {
                        case 0:
                            setValue = "Disable";
                            break;
                        case 1:
                            setValue = "Enable";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x00070006")) {
                    switch (value) {
                        case 0:
                            setValue = "Disable";
                            break;
                        case 1:
                            setValue = "Enable";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x00070008")) {
                    switch (value) {
                        case 0:
                            setValue = "Coaxial";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x00070009")) {
                    switch (value) {
                        case 0:
                            setValue = "High";
                            break;
                        case 1:
                            setValue = "Low";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x0007000A")) {
                    switch (value) {
                        case 0:
                            setValue = "High";
                            break;
                        case 1:
                            setValue = "Low";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x00070012")) {
                    switch (value) {
                        case 0:
                            setValue = "OFF";
                            break;
                        case 1:
                            setValue = "ON";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x00070013")) {
                    switch (value) {
                        case 0:
                            setValue = "OFF";
                            break;
                        case 1:
                            setValue = "ON";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x00070016")) {
                    switch (value) {
                        case 0:
                            setValue = "OFF";
                            break;
                        case 1:
                            setValue = "ON";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x00070017")) {
                    switch (value) {
                        case 0:
                            setValue = "OFF";
                            break;
                        case 1:
                            setValue = "ON";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x00070018")) {
                    switch (value) {
                        case 0:
                            setValue = "Disable";
                            break;
                        case 1:
                            setValue = "Enable";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x0007001A")) {
                    switch (value) {
                        case 0:
                            setValue = "Standard";
                            break;
                        case 1:
                            setValue = "Image comparison";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x0007001B")) {
                    switch (value) {
                        case 0:
                            setValue = "Same position";
                            break;
                        case 1:
                            setValue = "Camera1 master position";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x00070022")) {
                    switch (value) {
                        case 0:
                            setValue = "Standard";
                            break;
                        case 1:
                            setValue = "1st wafer Auto-Focus";
                            break;
                        case 2:
                            setValue = "1st wafer Auto-Focus/Bright";
                            break;
                        case 3:
                            setValue = "Every wafer Auto-Focus";
                            break;
                        case 4:
                            setValue = "Every wafer Auto-Focus/Bright";
                            break;
                        case 5:
                            setValue = "Alignment Result";
                            break;
                        case 6:
                            setValue = "Every line Auto-Focus";
                            break;
                        case 7:
                            setValue = "After ERROR Auto-Focus";
                            break;
                        case 8:
                            setValue = "Dress step1 Auto-Focus/Bright";
                            break;
                        case 9:
                            setValue = "Dress step1 Auto-Focus";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x00070023")) {
                    switch (value) {
                        case 0:
                            setValue = "OFF";
                            break;
                        case 1:
                            setValue = "ON";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x00070028")) {
                    switch (value) {
                        case 0:
                            setValue = "OFF";
                            break;
                        case 1:
                            setValue = "ON";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x00070029")) {
                    switch (value) {
                        case 0:
                            setValue = "Disable";
                            break;
                        case 1:
                            setValue = "Enable";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x0007002A")) {
                    switch (value) {
                        case 0:
                            setValue = "Disable";
                            break;
                        case 1:
                            setValue = "Enable";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x00070031")) {
                    switch (value) {
                        case 0:
                            setValue = "Disable";
                            break;
                        case 1:
                            setValue = "Enable";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x00070032")) {
                    switch (value) {
                        case 0:
                            setValue = "Execute with DATA";
                            break;
                        case 1:
                            setValue = "Execute with MAP";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x00070033")) {
                    switch (value) {
                        case 0:
                            setValue = "Block";
                            break;
                        case 1:
                            setValue = "Work";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x00070034")) {
                    switch (value) {
                        case 0:
                            setValue = "Block";
                            break;
                        case 1:
                            setValue = "Work";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x00070035")) {
                    switch (value) {
                        case 0:
                            setValue = "Execute only F1";
                            break;
                        case 1:
                            setValue = "Execute at Main index";
                            break;
                        case 2:
                            setValue = "Execute at next Main index";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x00070036")) {
                    switch (value) {
                        case 0:
                            setValue = "Disable";
                            break;
                        case 1:
                            setValue = "Standard";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x00070037")) {
                    switch (value) {
                        case 0:
                            setValue = "Disable";
                            break;
                        case 1:
                            setValue = "Standard";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x00070038")) {
                    switch (value) {
                        case 0:
                            setValue = "Disable";
                            break;
                        case 1:
                            setValue = "Standard";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x00070039")) {
                    switch (value) {
                        case 0:
                            setValue = "Disable";
                            break;
                        case 1:
                            setValue = "Standard";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x0007003A")) {
                    switch (value) {
                        case 0:
                            setValue = "Standard";
                            break;
                        case 1:
                            setValue = "Laser groove";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x0007003B")) {
                    switch (value) {
                        case 0:
                            setValue = "Standard";
                            break;
                        case 1:
                            setValue = "Laser groove";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x00070040")) {
                    switch (value) {
                        case 0:
                            setValue = "OFF";
                            break;
                        case 1:
                            setValue = "Evaluation Only";
                            break;
                        case 2:
                            setValue = "Evaluation and Correction";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x0008000B")) {
                    switch (value) {
                        case 0:
                            setValue = "Disable";
                            break;
                        case 1:
                            setValue = "Jump to next die in X-direction";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x0008000E")) {
                    switch (value) {
                        case 0:
                            setValue = "Disable";
                            break;
                        case 1:
                            setValue = "Enable";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x00080025")) {
                    switch (value) {
                        case 0:
                            setValue = "Left edge base";
                            break;
                        case 1:
                            setValue = "Same X";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x00080026")) {
                    switch (value) {
                        case 0:
                            setValue = "Standard";
                            break;
                        case 1:
                            setValue = "Individual";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x0009000F")) {
                    switch (value) {
                        case 0:
                            setValue = "Disable";
                            break;
                        case 1:
                            setValue = "Front+Rear";
                            break;
                        case 2:
                            setValue = "Front";
                            break;
                        case 3:
                            setValue = "Rear";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x00090010")) {
                    switch (value) {
                        case 0:
                            setValue = "Disable";
                            break;
                        case 1:
                            setValue = "Front+Rear";
                            break;
                        case 2:
                            setValue = "Front";
                            break;
                        case 3:
                            setValue = "Rear";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x000A0001")) {
                    switch (value) {
                        case 0:
                            setValue = "Synchronized to SP1";
                            break;
                        case 1:
                            setValue = "Individual setting";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x000A0002")) {
                    switch (value) {
                        case 0:
                            setValue = "Execute after half-cut";
                            break;
                        case 1:
                            setValue = "Execute after full-cut";
                            break;
                        case 2:
                            setValue = "Not execute";
                            break;
                        case 3:
                            setValue = "Half-cut & full-cut";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x000A0003")) {
                    switch (value) {
                        case 0:
                            setValue = "Disable";
                            break;
                        case 1:
                            setValue = "Enable";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x000A0009")) {
                    switch (value) {
                        case 0:
                            setValue = "Standard";
                            break;
                        case 1:
                            setValue = "Set distance";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x000A000C")) {
                    switch (value) {
                        case 0:
                            setValue = "Standard";
                            break;
                        case 1:
                            setValue = "DOWN-cut";
                            break;
                        case 2:
                            setValue = "UP-cut";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x000A000D")) {
                    switch (value) {
                        case 0:
                            setValue = "Standard";
                            break;
                        case 1:
                            setValue = "DOWN-cut";
                            break;
                        case 2:
                            setValue = "UP-cut";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x000A000F")) {
                    switch (value) {
                        case 0:
                            setValue = "Disable";
                            break;
                        case 1:
                            setValue = "Enable";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x000A0010")) {
                    switch (value) {
                        case 0:
                            setValue = "Disable";
                            break;
                        case 1:
                            setValue = "Enable";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x000A0022")) {
                    switch (value) {
                        case 0:
                            setValue = "Disable";
                            break;
                        case 1:
                            setValue = "Front+Rear";
                            break;
                        case 2:
                            setValue = "Front";
                            break;
                        case 3:
                            setValue = "Rear";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x000A0024")) {
                    switch (value) {
                        case 0:
                            setValue = "1st line of CH";
                            break;
                        case 1:
                            setValue = "1st line of work";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x000A0025")) {
                    switch (value) {
                        case 0:
                            setValue = "SP2 of STEP cutting";
                            break;
                        case 1:
                            setValue = "All Cutting methods";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x000A0026")) {
                    switch (value) {
                        case 0:
                            setValue = "Execute after half-cut";
                            break;
                        case 1:
                            setValue = "Execute after full-cut";
                            break;
                        case 2:
                            setValue = "Not execute";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x000A002B")) {
                    switch (value) {
                        case 0:
                            setValue = "Standard";
                            break;
                        case 1:
                            setValue = "Set distance";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x000B0006")) {
                    switch (value) {
                        case 0:
                            setValue = "Disable";
                            break;
                        case 1:
                            setValue = "Enable";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x000B0007")) {
                    switch (value) {
                        case 0:
                            setValue = "Disable";
                            break;
                        case 1:
                            setValue = "Enable";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x000C0001")) {
                    switch (value) {
                        case 0:
                            setValue = "Disable";
                            break;
                        case 1:
                            setValue = "Blade name";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x000C0002")) {
                    switch (value) {
                        case 0:
                            setValue = "Interrupt cutting process";
                            break;
                        case 1:
                            setValue = "Perform after wafer unloaded.";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x000C0003")) {
                    switch (value) {
                        case 0:
                            setValue = "Standard";
                            break;
                        case 1:
                            setValue = "Prediction control";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x000C0004")) {
                    switch (value) {
                        case 0:
                            setValue = "Distance";
                            break;
                        case 1:
                            setValue = "Line number";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x000C0005")) {
                    switch (value) {
                        case 0:
                            setValue = "Blade management mode";
                            break;
                        case 1:
                            setValue = "Recipe change";
                            break;
                        case 2:
                            setValue = "Every wafer";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x000C0006")) {
                    switch (value) {
                        case 0:
                            setValue = "Individual setting";
                            break;
                        case 1:
                            setValue = "Synchronized";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x000C000B")) {
                    switch (value) {
                        case 0:
                            setValue = "Standard";
                            break;
                        case 1:
                            setValue = "Individual setting by CH";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x000C001A")) {
                    switch (value) {
                        case 0:
                            setValue = "Continue";
                            break;
                        case 1:
                            setValue = "Unload";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x000C001B")) {
                    switch (value) {
                        case 0:
                            setValue = "Disable";
                            break;
                        case 1:
                            setValue = "Enable";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x000C0023")) {
                    switch (value) {
                        case 0:
                            setValue = "Synchronized";
                            break;
                        case 1:
                            setValue = "Individual setting";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x000C0028")) {
                    switch (value) {
                        case 0:
                            setValue = "OFF";
                            break;
                        case 1:
                            setValue = "CH1 only";
                            break;
                        case 2:
                            setValue = "CH2 only";
                            break;
                        case 3:
                            setValue = "Both channels";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x000C0029")) {
                    switch (value) {
                        case 0:
                            setValue = "OFF";
                            break;
                        case 1:
                            setValue = "CH1 only";
                            break;
                        case 2:
                            setValue = "CH2 only";
                            break;
                        case 3:
                            setValue = "Both channels";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x000C002A")) {
                    switch (value) {
                        case 0:
                            setValue = "Disable";
                            break;
                        case 1:
                            setValue = "After 1 line cutting";
                            break;
                        case 2:
                            setValue = "After cutting set distance";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x000C002B")) {
                    switch (value) {
                        case 0:
                            setValue = "Auto. Dress interval";
                            break;
                        case 1:
                            setValue = "Blade management";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x000C0032")) {
                    switch (value) {
                        case 0:
                            setValue = "Blade work interval";
                            break;
                        case 1:
                            setValue = "Frame interval";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x000C0034")) {
                    switch (value) {
                        case 0:
                            setValue = "Disable";
                            break;
                        case 1:
                            setValue = "Enable";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x000C0037")) {
                    switch (value) {
                        case 0:
                            setValue = "Disable";
                            break;
                        case 1:
                            setValue = "Enable";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x000C00A8")) {
                    switch (value) {
                        case 0:
                            setValue = "OFF";
                            break;
                        case 1:
                            setValue = "ON";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x000C00A9")) {
                    switch (value) {
                        case 0:
                            setValue = "Cut-remain";
                            break;
                        case 1:
                            setValue = "Cut-in";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x000C0138")) {
                    switch (value) {
                        case 0:
                            setValue = "OFF";
                            break;
                        case 1:
                            setValue = "ON";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x000C0139")) {
                    switch (value) {
                        case 0:
                            setValue = "Cut-remain";
                            break;
                        case 1:
                            setValue = "Cut-in";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x000C0158")) {
                    switch (value) {
                        case 0:
                            setValue = "Disable";
                            break;
                        case 1:
                            setValue = "Enable";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x000C015D")) {
                    switch (value) {
                        case 0:
                            setValue = "Distance";
                            break;
                        case 1:
                            setValue = "Line number";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x000D0002")) {
                    switch (value) {
                        case 0:
                            setValue = "Standard";
                            break;
                        case 1:
                            setValue = "Alignment prior mode";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x000D0003")) {
                    switch (value) {
                        case 0:
                            setValue = "Standard";
                            break;
                        case 1:
                            setValue = "CH individual cutting";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x000D0005")) {
                    switch (value) {
                        case 0:
                            setValue = "Standard";
                            break;
                        case 1:
                            setValue = "Standard";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x000D0048")) {
                    switch (value) {
                        case 0:
                            setValue = "Disable";
                            break;
                        case 1:
                            setValue = "Enable";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x000D004B")) {
                    switch (value) {
                        case 0:
                            setValue = "No(Standard)";
                            break;
                        case 1:
                            setValue = "Execute";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x000E0002")) {
                    switch (value) {
                        case 0:
                            setValue = "OFF";
                            break;
                        case 1:
                            setValue = "Air Curtain";
                            break;
                        case 2:
                            setValue = "Water Curtain";
                            break;
                        case 3:
                            setValue = "Air+Water";
                            break;
                        case 4:
                            setValue = "Air Curtain & Scope Air";
                            break;
                        case 5:
                            setValue = "Water Curtain & Scope Air";
                            break;
                        case 6:
                            setValue = "Air+Water Curtain & Scope Air";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x000E0003")) {
                    switch (value) {
                        case 0:
                            setValue = "Disable";
                            break;
                        case 1:
                            setValue = "Rotation cleaning";
                            break;
                        case 2:
                            setValue = "Static cleaning";
                            break;
                        case 3:
                            setValue = "Special cleaning";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x000E0006")) {
                    switch (value) {
                        case 0:
                            setValue = "Disable";
                            break;
                        case 1:
                            setValue = "Bubble";
                            break;
                        case 2:
                            setValue = "Low";
                            break;
                        case 3:
                            setValue = "Rinse";
                            break;
                        case 4:
                            setValue = "Bu+Lo";
                            break;
                        case 5:
                            setValue = "Bu+Rinse";
                            break;
                        case 6:
                            setValue = "Lo+Rinse";
                            break;
                        case 7:
                            setValue = "Bu+Lo+Rn";
                            break;
                        case 8:
                            setValue = "Hi";
                            break;
                        case 9:
                            setValue = "Hi+Lo";
                            break;
                        case 10:
                            setValue = "Hi+Rinse";
                            break;
                        case 11:
                            setValue = "Hi+Lo+Rn";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x000E000A")) {
                    switch (value) {
                        case 0:
                            setValue = "Disable";
                            break;
                        case 1:
                            setValue = "Bubble";
                            break;
                        case 2:
                            setValue = "Low";
                            break;
                        case 3:
                            setValue = "Rinse";
                            break;
                        case 4:
                            setValue = "Bu+Lo";
                            break;
                        case 5:
                            setValue = "Bu+Rinse";
                            break;
                        case 6:
                            setValue = "Lo+Rinse";
                            break;
                        case 7:
                            setValue = "Bu+Lo+Rn";
                            break;
                        case 8:
                            setValue = "Hi";
                            break;
                        case 9:
                            setValue = "Hi+Lo";
                            break;
                        case 10:
                            setValue = "Hi+Rinse";
                            break;
                        case 11:
                            setValue = "Hi+Lo+Rn";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x000E000E")) {
                    switch (value) {
                        case 0:
                            setValue = "Disable";
                            break;
                        case 1:
                            setValue = "Bubble";
                            break;
                        case 2:
                            setValue = "Low";
                            break;
                        case 3:
                            setValue = "Rinse";
                            break;
                        case 4:
                            setValue = "Bu+Lo";
                            break;
                        case 5:
                            setValue = "Bu+Rinse";
                            break;
                        case 6:
                            setValue = "Lo+Rinse";
                            break;
                        case 7:
                            setValue = "Bu+Lo+Rn";
                            break;
                        case 8:
                            setValue = "Hi";
                            break;
                        case 9:
                            setValue = "Hi+Lo";
                            break;
                        case 10:
                            setValue = "Hi+Rinse";
                            break;
                        case 11:
                            setValue = "Hi+Lo+Rn";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x000E0012")) {
                    switch (value) {
                        case 0:
                            setValue = "Disable";
                            break;
                        case 1:
                            setValue = "Bubble";
                            break;
                        case 2:
                            setValue = "Low";
                            break;
                        case 3:
                            setValue = "Rinse";
                            break;
                        case 4:
                            setValue = "Bu+Lo";
                            break;
                        case 5:
                            setValue = "Bu+Rinse";
                            break;
                        case 6:
                            setValue = "Lo+Rinse";
                            break;
                        case 7:
                            setValue = "Bu+Lo+Rn";
                            break;
                        case 8:
                            setValue = "Hi";
                            break;
                        case 9:
                            setValue = "Hi+Lo";
                            break;
                        case 10:
                            setValue = "Hi+Rinse";
                            break;
                        case 11:
                            setValue = "Hi+Lo+Rn";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x000E0016")) {
                    switch (value) {
                        case 0:
                            setValue = "Disable";
                            break;
                        case 1:
                            setValue = "Bubble";
                            break;
                        case 2:
                            setValue = "Low";
                            break;
                        case 3:
                            setValue = "Rinse";
                            break;
                        case 4:
                            setValue = "Bu+Lo";
                            break;
                        case 5:
                            setValue = "Bu+Rinse";
                            break;
                        case 6:
                            setValue = "Lo+Rinse";
                            break;
                        case 7:
                            setValue = "Bu+Lo+Rn";
                            break;
                        case 8:
                            setValue = "Hi";
                            break;
                        case 9:
                            setValue = "Hi+Lo";
                            break;
                        case 10:
                            setValue = "Hi+Rinse";
                            break;
                        case 11:
                            setValue = "Hi+Lo+Rn";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x000E001A")) {
                    switch (value) {
                        case 0:
                            setValue = "Disable";
                            break;
                        case 1:
                            setValue = "Bubble";
                            break;
                        case 2:
                            setValue = "Low";
                            break;
                        case 3:
                            setValue = "Rinse";
                            break;
                        case 4:
                            setValue = "Bu+Lo";
                            break;
                        case 5:
                            setValue = "Bu+Rinse";
                            break;
                        case 6:
                            setValue = "Lo+Rinse";
                            break;
                        case 7:
                            setValue = "Bu+Lo+Rn";
                            break;
                        case 8:
                            setValue = "Hi";
                            break;
                        case 9:
                            setValue = "Hi+Lo";
                            break;
                        case 10:
                            setValue = "Hi+Rinse";
                            break;
                        case 11:
                            setValue = "Hi+Lo+Rn";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x000E001E")) {
                    switch (value) {
                        case 0:
                            setValue = "Disable";
                            break;
                        case 1:
                            setValue = "Bubble";
                            break;
                        case 2:
                            setValue = "Low";
                            break;
                        case 3:
                            setValue = "Rinse";
                            break;
                        case 4:
                            setValue = "Bu+Lo";
                            break;
                        case 5:
                            setValue = "Bu+Rinse";
                            break;
                        case 6:
                            setValue = "Lo+Rinse";
                            break;
                        case 7:
                            setValue = "Bu+Lo+Rn";
                            break;
                        case 8:
                            setValue = "Hi";
                            break;
                        case 9:
                            setValue = "Hi+Lo";
                            break;
                        case 10:
                            setValue = "Hi+Rinse";
                            break;
                        case 11:
                            setValue = "Hi+Lo+Rn";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x000E0022")) {
                    switch (value) {
                        case 0:
                            setValue = "Standard";
                            break;
                        case 1:
                            setValue = "No Air blow";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x000E0026")) {
                    switch (value) {
                        case 0:
                            setValue = "Disable";
                            break;
                        case 1:
                            setValue = "Bubble";
                            break;
                        case 2:
                            setValue = "Low";
                            break;
                        case 3:
                            setValue = "Rinse";
                            break;
                        case 4:
                            setValue = "Bu+Lo";
                            break;
                        case 5:
                            setValue = "Bu+Rinse";
                            break;
                        case 6:
                            setValue = "Lo+Rinse";
                            break;
                        case 7:
                            setValue = "Bu+Lo+Rn";
                            break;
                        case 8:
                            setValue = "Hi";
                            break;
                        case 9:
                            setValue = "Hi+Lo";
                            break;
                        case 10:
                            setValue = "Hi+Rinse";
                            break;
                        case 11:
                            setValue = "Hi+Lo+Rn";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x000E002A")) {
                    switch (value) {
                        case 0:
                            setValue = "Disable";
                            break;
                        case 1:
                            setValue = "Bubble";
                            break;
                        case 2:
                            setValue = "Low";
                            break;
                        case 3:
                            setValue = "Rinse";
                            break;
                        case 4:
                            setValue = "Bu+Lo";
                            break;
                        case 5:
                            setValue = "Bu+Rinse";
                            break;
                        case 6:
                            setValue = "Lo+Rinse";
                            break;
                        case 7:
                            setValue = "Bu+Lo+Rn";
                            break;
                        case 8:
                            setValue = "Hi";
                            break;
                        case 9:
                            setValue = "Hi+Lo";
                            break;
                        case 10:
                            setValue = "Hi+Rinse";
                            break;
                        case 11:
                            setValue = "Hi+Lo+Rn";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x000E0033")) {
                    switch (value) {
                        case 0:
                            setValue = "Disable";
                            break;
                        case 1:
                            setValue = "Air Curtain";
                            break;
                        case 2:
                            setValue = "Water Curtain";
                            break;
                        case 3:
                            setValue = "Air&Water Curtain";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x000E0034")) {
                    switch (value) {
                        case 0:
                            setValue = "Disable";
                            break;
                        case 1:
                            setValue = "CH1";
                            break;
                        case 2:
                            setValue = "CH2";
                            break;
                        case 3:
                            setValue = "CH1&CH2";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x000E0039")) {
                    switch (value) {
                        case 0:
                            setValue = "OFF";
                            break;
                        case 1:
                            setValue = "ON";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x000E003A")) {
                    switch (value) {
                        case 0:
                            setValue = "Same Pre-wash mode";
                            break;
                        case 1:
                            setValue = "OFF";
                            break;
                        case 2:
                            setValue = "Air Curtain";
                            break;
                        case 3:
                            setValue = "Water Curtain";
                            break;
                        case 4:
                            setValue = "Air+Water";
                            break;
                        case 5:
                            setValue = "Air Curtain & Scope Air";
                            break;
                        case 6:
                            setValue = "Water Curtain & Scope Air";
                            break;
                        case 7:
                            setValue = "Air+Water Curtain & Scope Air";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x000E003B")) {
                    switch (value) {
                        case 0:
                            setValue = "Standard";
                            break;
                        case 1:
                            setValue = "Individual by STEP";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x000E0045")) {
                    switch (value) {
                        case 0:
                            setValue = "Front";
                            break;
                        case 1:
                            setValue = "Rear";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x000F0001")) {
                    switch (value) {
                        case 0:
                            setValue = "Disable";
                            break;
                        case 1:
                            setValue = "UV by fixed time setting";
                            break;
                        case 2:
                            setValue = "UV by adjustable time mode";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x000F0002")) {
                    switch (value) {
                        case 0:
                            setValue = "Disable";
                            break;
                        case 1:
                            setValue = "UV by fixed time setting";
                            break;
                        case 2:
                            setValue = "UV by adjustable time mode";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x000F0008")) {
                    switch (value) {
                        case 0:
                            setValue = "Disable";
                            break;
                        case 1:
                            setValue = "Enable";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x000F0009")) {
                    switch (value) {
                        case 0:
                            setValue = "Disable";
                            break;
                        case 1:
                            setValue = "Every work";
                            break;
                        case 2:
                            setValue = "Intervals";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x000F000D")) {
                    switch (value) {
                        case 0:
                            setValue = "Average";
                            break;
                        case 1:
                            setValue = "Inclination";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x000F000F")) {
                    switch (value) {
                        case 0:
                            setValue = "1 point";
                            break;
                        case 1:
                            setValue = "2 points";
                            break;
                        case 2:
                            setValue = "4 points";
                            break;
                        case 3:
                            setValue = "8 points";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else if (key.equalsIgnoreCase("0x000F0015")) {
                    switch (value) {
                        case 0:
                            setValue = "After 1 line cut";
                            break;
                        case 1:
                            setValue = "After 1 wefer cut";
                            break;
                        default:
                            setValue = String.valueOf(value);
                    }
                } else {
                    setValue = String.valueOf(value);
                }
                resultMap.put(key, setValue);
                if (key.equalsIgnoreCase("0x001300C8")) {
                    break;
                }
            }

            SqlSession sqlSession = MybatisSqlSession.getSqlSession();
            RecipeService recipeService = new RecipeService(sqlSession);
            List<RecipeTemplate> recipeTemplates = recipeService.searchRecipeTemplateByDeviceTypeCode("TSKAWD300TX", "RecipePara");
            for (RecipeTemplate recipeTemplate : recipeTemplates) {
                RecipePara recipePara = new RecipePara();
                String dataID = recipeTemplate.getParaDesc().toLowerCase();
                String paraValue = "";
                if (resultMap != null && resultMap.get(dataID) != null) {
                    paraValue = String.valueOf(resultMap.get(dataID));
                    resultMap.remove(dataID);
                }
                recipePara.setParaCode(recipeTemplate.getParaCode());
                recipePara.setParaName(recipeTemplate.getParaName());
                recipePara.setSetValue(paraValue);
                if (recipeTemplate.getParaUnit() != null && !recipeTemplate.getParaUnit().isEmpty()) {
                    recipePara.setParaMeasure(recipeTemplate.getParaUnit());
                }
                if (recipeTemplate.getParaShotName() != null && !recipeTemplate.getParaShotName().isEmpty()) {
                    recipePara.setParaShotName(recipeTemplate.getParaShotName());
                }
                recipeParas.add(recipePara);
            }
            System.out.println("模板表未配的参数数目：" + resultMap.size());
            sqlSession.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return recipeParas;
    }

    public static String[] getFileName(String path) {
        File file = new File(path);
        String[] fileName = file.list();
        return fileName;
    }

    public static void main(String[] args) {
//        List<RecipePara> l = new TPRecipeUtil().tPRecipeTran("D:\\长电城东\\Recipe\\NITTO\\8-N-260PG-02\\");
//        List<RecipePara> l = new AWD300TXRecipeUtil().TSKRecipeTran("D:\\长电城东\\黄云鹏\\DSB\\8-FM318U12-224S-320-90\\");
//        List<RecipePara> l = new AWD300TXRecipeUtil().TSKRecipeTran("D:\\长电城东\\黄云鹏\\DSB\\8-TMGE30B-F125E-200-0_V0.txt\\");
        List<RecipePara> l = new AWD300TXRecipeUtil().TSKRecipeTran("D:\\8-TMIE47E-224-250-90.txt\\");
        for (int i = 0; i < l.size(); i++) {
//            System.out.println(l.get(i).toString());
            System.out.println("paraCode:" + l.get(i).getParaCode());
            System.out.println("paraName:" + l.get(i).getParaName());
            System.out.println("paraValue:" + l.get(i).getSetValue());
            System.out.println("paraUnit:" + l.get(i).getParaMeasure());
            System.out.println("paraShotName:" + l.get(i).getParaShotName());
        }
        System.out.println("参数数目:" + l.size());
    }
}
