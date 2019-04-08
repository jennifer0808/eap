/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.secsLayer.equipImpl.esec.fc;

import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 *
 * @author luosy nullbin 46 bin 48 bad 83
 */
public class SigmaPlusWaferTransfer {

    // static String waferMappingPath = GlobalConstants.getProperty("LOCAL_WAFER_MAPPING_PATH");
    private static final Logger logger = Logger.getLogger(SigmaPlusWaferTransfer.class.getName());

    public static Map getWaferFileInfo(String waferId, String angle) {
        Map resultMap = new HashMap();
        int mappingType = 2;
        //默认为2
//        if (waferId.contains(".tmc")) {
//            mappingType = 1;
//        } else if (waferId.contains(".txt") || waferId.contains(".TXT")) {
//            mappingType = 0;
//        } else {
//            waferId = waferId + ".txt";
//        }
        //JCET:只存在txt类型文件，id不带文件名
        waferId = waferId + ".txt";
        String waferFilePath = getPath(waferId); //"D:\\RECIPE\\T19F55.00-10.TXT";
        BufferedReader br = null;
        try {
            String cfgline = null;

            int rowCount = 0;
            String columnCount = "";
            List<String> binList = new LinkedList();
            String bin = "";
            File cfgfile = new File(waferFilePath);
            br = new BufferedReader(new InputStreamReader(new FileInputStream(cfgfile), "UTF-8"));
            // String[][]wafer=new String[53][58];
            while ((cfgline = br.readLine()) != null) {
                if (cfgline.contains("Wafer_ID") || cfgline.contains("Wafer ID")) {
//                    String[] cfglines = cfgline.split(":");
//                    if (cfglines.length > 1) {
//                        //mapping bin位为0
//                        mappingType = 0;
//                    } else {
//                        //mapping bin位为P
//                        mappingType = 1;
//                    }
                    continue;
                }
                if (cfgline.contains("Flat_Notch") || cfgline.contains("Company") || cfgline.contains("Device")
                        || cfgline.contains("Original") || cfgline.contains("Wafer Lot") || cfgline.contains("Bin")
                        || cfgline.contains("Die") || cfgline.contains("Column") || cfgline.contains("Empty")) {
                    continue;
                }
                if ("".equals(cfgline)) {
                    continue;
                }
                columnCount = String.valueOf(cfgline.length());

                if (mappingType == 0) {

                    for (int j = 0; j < cfgline.length(); j++) {
                        char c = cfgline.charAt(j);
                        if (String.valueOf(c).equals("0")) {
                            bin = "48";
                        } else if (String.valueOf(c).equalsIgnoreCase("s")) {
                            bin = "83";
                        } else if (String.valueOf(c).equalsIgnoreCase("/")) {
                            bin = "47";
                        } else if (String.valueOf(c).equalsIgnoreCase("c")) {
                            bin = "67";
                        } else if (String.valueOf(c).equalsIgnoreCase("b")) {
                            bin = "66";
                        } else {
                            bin = "46";
                        }
                        binList.add(bin);
                    }
                    rowCount++;
                } else if (mappingType == 1) {
                    //char 1 bincode 49 good
                    //char 2 bincode 50 bad
                    //char 3 bincode 51 bad
                    //char 4 bincode 52
                    for (int j = 0; j < cfgline.length(); j++) {
                        char c = cfgline.charAt(j);
                        if (String.valueOf(c).equals("P") || String.valueOf(c).equals("1")) {
                            bin = "49";
                        } else if (String.valueOf(c).equals("2")) {
                            bin = "50";
                        } else if (String.valueOf(c).equals("3")) {
                            bin = "51";
                        } else if (String.valueOf(c).equals("4")) {
                            bin = "52";
                        } else {
                            bin = "46";
                        }
                        binList.add(bin);

                    }
                    rowCount++;
                } else if (mappingType == 2) {
                    //char 1 bincode 49 good
                    //char 2 bincode 50 bad
                    //char 3 bincode 51 bad
                    //char 4 bincode 52
                    for (int j = 0; j < cfgline.length(); j++) {
                        char c = cfgline.charAt(j);
                        if (String.valueOf(c).equals("1")) {
                            bin = "48";
                        } else if (String.valueOf(c).equalsIgnoreCase("x")) {
                            bin = "50";
                        } else {
                            bin = "46";
                        }
                        binList.add(bin);

                    }
                    rowCount++;
                }
            }
            br.close();
            long[] bins = new long[binList.size()];
            for (int i = 0; i < binList.size(); i++) {
                bins[i] = Long.parseLong(binList.get(i));
            }
//            resultMap.put("BinList", bins);
            resultMap.put("BinList", transferAngle(bins, angle, rowCount, Integer.valueOf(columnCount)));

            if ("90".equals(angle) || "270".equals(angle)) {
                int tmp = rowCount;
                rowCount = Integer.valueOf(columnCount);
                columnCount = String.valueOf(tmp);
            }
            resultMap.put("RowCountInDieIncrements", rowCount);
            resultMap.put("ColumnCountInDieIncrements", columnCount);

            resultMap.put("ProcessDieCount", bins.length);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return resultMap;
    }

    public static void main(String[] args) {
        Map map = getWaferFileInfo("PKBX42-03", "180");

        //transferArgs1(transferArgs1((long[]) map.get("BinList"), 53, 58), 58, 53);
//        transferAngle((long[]) map.get("BinList"), "90");
        System.out.println("cn.tzinfo.htauto.octopus.secsLayer.equipImpl.esec.SigmaPlusWaferTransfer.main()" + getPath("T19M35.00-03.TXT"));
    }

    private static long[] transferAngle(long[] src, String angle, int row, int col) {
        if ("0".equals(angle)) {
            return src;
        }
        long[] tmp = transferArgs1(src, row, col);
        if ("90".equals(angle)) {
            return tmp;
        }
        if ("180".equals(angle)) {
            tmp = transferArgs1(tmp, col, row);
        }
        if ("270".equals(angle)) {
            tmp = transferArgs1(tmp, col, row);
            tmp = transferArgs1(tmp, row, col);
        }
        return tmp;
    }

    private static long[] transferArgs(long[] src, int row, int col) {

        int i, j;
        long[][] num;
        num = new long[row][col];
        int count = 0;
        for (i = 0; i < row; i++) {
            for (j = 0; j < col; j++) {
                if (i == 0) {
                    num[i][j] = src[j];
                } else {
                    num[i][j] = src[count];
                }
                count++;
            }
        }

        long[][] num2;
        num2 = new long[col][row];

        for (i = 0; i < num.length; i++) {
            for (j = 0; j < num[i].length; j++) {
                num2[j][i] = num[i][j];
            }
        }

//            long[][] num3 = num2;
//            for (i = 0; i < num2.length; i++) {
//                for (j = 0; j < num2[i].length; j++) {
//                    long[] tmp = num2[i];
//                    for (int m = 0; m < num3[i].length; m++) {
//                        for (int p = tmp.length - 1; p > -1; p--) {
//                            num3[i][m] = tmp[p];
//                        }
//                    }
//                }
//            }
        System.out.println("-----------分割------------");
        long[] result = new long[src.length];
        List results = new LinkedList();
        for (i = 0; i < num2.length; i++) {
            for (j = 0; j < num2[i].length; j++) {
                results.add(num2[i][j]);
                // System.out.println("num2" + "[" + i + "]" + "[" + j + "]" + "---" + num2[i][j]);
            }
        }
        for (i = 0; i < results.size(); i++) {
            result[i] = Long.parseLong(String.valueOf(results.get(i)));
            System.out.print(result[i] + " ");

        }
        return result;
    }

    private static long[] transferArgs1(long[] src, int row, int col) {
        int i, j;
        long[][] num = new long[row][col];
        int count = 0;
        for (i = 0; i < row; i++) {
            for (j = 0; j < col; j++) {
                if (i == 0) {
                    num[i][j] = src[j];
                } else {
                    num[i][j] = src[count];
                }
                count++;
            }
        }
        long[][] num2 = new long[col][row];
        i = 0;
        for (int k = num.length - 1; k > -1; k--) {
            int tmp = 0;
            for (int p = 0; p < num2.length; p++) {
                num2[tmp][k] = num[i][p];
                tmp++;
            }
            i++;
        }
        long[] result = new long[src.length];
        List results = new LinkedList();
        for (i = 0; i < num2.length; i++) {
            for (j = 0; j < num2[i].length; j++) {
                results.add(num2[i][j]);
                // System.out.println("num2" + "[" + i + "]" + "[" + j + "]" + "---" + num2[i][j]);
            }
        }
        for (i = 0; i < results.size(); i++) {
            result[i] = Long.parseLong(String.valueOf(results.get(i)));
            //System.out.print(result[i] + " ");
        }
        return result;
    }

    private static String getPath(String waferId) {
        String lot = waferId.split("-")[0];
//        String path = waferMappingPath + "\\" + lot + "\\" + waferId;
        String path = "D:\\RECIPE\\" + "\\" + lot + "\\" + waferId;
        logger.info("waferpath：" + path);
        return path;
    }
}
