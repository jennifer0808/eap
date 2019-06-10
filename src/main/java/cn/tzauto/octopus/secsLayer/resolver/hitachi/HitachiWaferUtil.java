/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.secsLayer.resolver.hitachi;

import org.apache.log4j.Logger;

import java.io.*;
import java.util.*;

/**
 *
 * @author luosy
 */
public class HitachiWaferUtil {

    static String waferMappingPath = "D:\\WAFERMAPPING";
    private static final Logger logger = Logger.getLogger(HitachiWaferUtil.class);
    private static Properties prop = new Properties();
    private static String maxColumnCount = "0";

    public static Map getWaferFileInfo(String waferId, String angle, String deviceCode) {
        loadBinConfig();
        Map<String, String> binMap = new HashMap();
        binMap = transferKey2Map(String.valueOf(prop.get("ANY")));
        Map resultMap = new HashMap();
        String lotID = "lotID";
        String waferNo = "waferID";
        if (waferId.contains("-")) {
            lotID = waferId.split("-")[0];
        }

        String waferFilePath = getPath(waferId, deviceCode);
        BufferedReader br = null;
        try {
            String cfgline = null;

            int rowCount = 0;
            String columnCount = "";
            List<String> binList = new LinkedList();
            String bin = "";
            File cfgfile = new File(waferFilePath);
            br = new BufferedReader(new InputStreamReader(new FileInputStream(cfgfile), "UTF-8"));
            while ((cfgline = br.readLine()) != null) {
                if (cfgline.contains(waferId) || cfgline.contains(lotID) || cfgline.contains("Bin") || cfgline.equals(waferNo)) {
                    continue;
                }
                if (cfgline.contains("Wafer_ID") || cfgline.contains("Flat_Notch:")) {
                    continue;
                }
                //asc
                if (cfgline.contains("DEVICE") || cfgline.contains("X:") || cfgline.contains("Y:") || cfgline.contains("REFDIE:")
                        || cfgline.contains("WAFERID") || cfgline.contains("FLAT ZONE :")) {
                    continue;
                }
                //
                if (cfgline.contains("Spreadtrum") || cfgline.contains("NOTCH") || cfgline.contains(waferId.split("-")[0])) {
                    continue;
                }
                //dat
                if (cfgline.contains("PASS") || cfgline.contains("=") || cfgline.contains("FAIL")
                        || cfgline.contains("5    10   15   20   25   30")
                        || cfgline.contains("111111111122222222223333333333")) {
                    continue;
                }
                //cp1
                if (cfgline.contains(":") && !waferId.contains("txt")) {
                    continue;
                }
                if (cfgline.contains("BIN") || cfgline.contains("BOF") || cfgline.contains("EXTENSION") || cfgline.contains("EOF")
                        || cfgline.contains("0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000001111111111111111111111111111111")
                        || cfgline.contains("0000000001111111111222222222233333333334444444444555555555566666666667777777777888888888899999999990000000000111111111122222222223")
                        || cfgline.contains("1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890")) {
                    continue;
                }
                if ("".equals(cfgline.trim())) {
                    continue;
                }

                //out
                if (waferId.contains(".out") || waferId.contains(".CP1")) {
                    cfgline = trimOutType(cfgline);
                }
//                cfgline = trimCP1Type(cfgline);
                //dat
                if (cfgline.contains("PM")) {
                    continue;
                }

                //EMT
                if (waferId.contains(".EMT") && cfgline.contains("EMT")) {
                    continue;
                }
                //etc
                if (waferId.contains(".EMT")) {
                    if (cfgline.contains("ETC")) {
                        continue;
                    }
                    if (cfgline.substring(0, 1).equals("#")) {
                        continue;
                    }
                }
                //smic
                if (cfgline.contains("die")) {
                    continue;
                }
                if (waferId.contains(".smic")) {
                    cfgline = transferSMIC(cfgline);
                }

                //tma
                if (cfgline.contains("++-++-++") || cfgline.contains("01 02 03 04 05 06 07 08 09 10 11 12 13 14 15 16 17 18 19 20")) {
                    continue;
                }
                if (waferId.contains(".tma")) {
                    cfgline = trimTmaType(cfgline);
                }

                //tmb
                if (cfgline.contains("| Bin") || cfgline.contains("| 00")) {
                    continue;
                }
                if (waferId.contains(".tmb") || waferId.contains(".tmc")) {
                    if (!cfgline.contains("|")) {
                        continue;
                    } else {
                        cfgline = trimTmbType(cfgline);
                    }
                }

                if (waferId.contains(".dat")) {
                    if (cfgline.contains("]")) {
                        continue;
                    }
                    cfgline = trimdatType(cfgline);
                }
                if (waferId.contains(".UMC")) {
                    if (cfgline.length() < 15 || cfgline.contains("UMC")) {
                        continue;
                    }
                }
                if (waferId.contains(".UTR")) {
                    if (cfgline.contains(".UTR") || cfgline.equals("PMC") || cfgline.length() < 15) {
                        continue;
                    }
                }
                //wfp
                if (waferId.contains(".wfp")) {
                    if (!cfgline.contains("|")) {
                        continue;
                    } else {
                        cfgline = trimWfpType(cfgline);
                    }
                }

                if (waferId.contains(".WIN")) {
                    if (cfgline.contains(".WIN") || cfgline.equals("Nuvoton") || cfgline.contains("-")) {
                        continue;
                    }
                }
                if (waferId.contains(".XML")) {
                    if (!cfgline.contains("CDATA")) {
                        continue;
                    } else {
                        cfgline = trimXmlType(cfgline);
                    }
                }
                if (waferId.contains(".txt") || waferId.contains(".TXT")) {
                    cfgline = trimTxtType(cfgline);
                }

                if ("".equals(cfgline.trim())) {
                    continue;
                }
                columnCount = String.valueOf(cfgline.trim().length());
                if (Integer.valueOf(maxColumnCount) < Integer.valueOf(columnCount)) {
                    maxColumnCount = columnCount;
                }
                System.out.println(cfgline);
                for (int j = 0; j < cfgline.length(); j++) {
                    char c = cfgline.charAt(j);
                    bin = binMap.get(String.valueOf(c));
                    if (bin == null || "".equals(bin)) {
                        bin = " ";
                    }
                    binList.add(bin);
                }
                rowCount++;
            }
            br.close();
            String[] bins = new String[binList.size()];
            for (int i = 0; i < binList.size(); i++) {
                bins[i] = binList.get(i);
            }
            resultMap.put("BinList", transferAngle(bins, angle, rowCount, Integer.valueOf(maxColumnCount)));
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

    private static String[] transferAngle(String[] src, String angle, int row, int col) {
        if ("0".equals(angle)) {
            return src;
        }
        String[] tmp = transferArgs1(src, row, col);
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
            System.out.println(result[i] + " ");

        }
        return result;
    }

    private static String[] transferArgs1(String[] src, int row, int col) {
        int i, j;
        String[][] num = new String[row][col];
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
        String[][] num2 = new String[col][row];
        i = 0;
        for (int k = num.length - 1; k > -1; k--) {
            int tmp = 0;
            for (int p = 0; p < num2.length; p++) {
                num2[tmp][k] = num[i][p];
                tmp++;
            }
            i++;
        }
        String[] result = new String[src.length];
        List results = new LinkedList();
        for (i = 0; i < num2.length; i++) {
            for (j = 0; j < num2[i].length; j++) {
                results.add(num2[i][j]);
//                 System.out.println("num2" + "[" + i + "]" + "[" + j + "]" + "---" + num2[i][j]);
            }
        }
        for (i = 0; i < results.size(); i++) {
            result[i] = String.valueOf(results.get(i));
            System.out.print(result[i] + "");
        }
        return result;
    }

    private static String getPath(String waferId, String deviceCode) {
        String lot = waferId.split("-")[0];
        String path = waferMappingPath + "\\" + deviceCode + "\\" + lot + "\\" + waferId;
//        path = "C:\\Users\\luosy\\Desktop\\ht\\不同格式的MAP\\不同格式的MAP\\tmb\\NF10A-08.tmb";
//        path = "C:\\Users\\luosy\\Desktop\\ht\\不同格式的MAP\\不同格式的MAP\\out\\H00H37-08.out";
//        path = "C:\\Users\\luosy\\Desktop\\ht\\不同格式的MAP\\不同格式的MAP\\asc\\HYMJC-01.asc";
//        path = "C:\\Users\\luosy\\Desktop\\ht\\不同格式的MAP\\不同格式的MAP\\cp1\\RCSYN-18.CP1";
//        path = "C:\\Users\\luosy\\Desktop\\ht\\不同格式的MAP\\不同格式的MAP\\dat\\SI11494-03.dat";
//        path = "C:\\Users\\luosy\\Desktop\\ht\\不同格式的MAP\\不同格式的MAP\\EMT\\P1B122-01.EMT";
//        path = "C:\\Users\\luosy\\Desktop\\ht\\不同格式的MAP\\不同格式的MAP\\ETC\\BPR296-18.ETC";
//        path = "C:\\Users\\luosy\\Desktop\\ht\\不同格式的MAP\\不同格式的MAP\\smic\\SL1460-08.smic";
//        path = "C:\\Users\\luosy\\Desktop\\ht\\不同格式的MAP\\不同格式的MAP\\tma\\28-20111-0-01.tma";
//        path = "C:\\Users\\luosy\\Desktop\\ht\\不同格式的MAP\\不同格式的MAP\\tmc\\862368-18.tmc";
        path = "C:\\Users\\luosy\\Desktop\\ht\\不同格式的MAP\\不同格式的MAP\\wfp\\S13195-18.wfp";
//         path = "C:\\Users\\luosy\\Desktop\\ht\\不同格式的MAP\\不同格式的MAP\\WIN\\A738190-18.WIN";
//        path = "C:\\Users\\luosy\\Desktop\\ht\\不同格式的MAP\\不同格式的MAP\\XML\\CP1042857-18.XML";
//        path = "C:\\Users\\luosy\\Desktop\\ht\\不同格式的MAP\\不同格式的MAP\\UMC\\S1LP6-08.UMC";
//        path = "C:\\Users\\luosy\\Desktop\\ht\\不同格式的MAP\\不同格式的MAP\\UTR\\P1M742-08.UTR";
//        path = "C:\\Users\\luosy\\Desktop\\ht\\不同格式的MAP\\不同格式的MAP\\UTR\\P1M742-08.UTR";
//        path = "C:\\Users\\luosy\\Desktop\\ht\\不同格式的MAP\\不同格式的MAP\\txt\\1.txt";
//        path = "C:\\Users\\luosy\\Desktop\\ht\\不同格式的MAP\\不同格式的MAP\\txt\\2.txt";
//        path = "C:\\Users\\luosy\\Desktop\\ht\\不同格式的MAP\\不同格式的MAP\\txt\\3.txt";
//        path = "C:\\Users\\luosy\\Desktop\\ht\\不同格式的MAP\\不同格式的MAP\\txt\\4.txt";
//        path = "C:\\Users\\luosy\\Desktop\\ht\\不同格式的MAP\\不同格式的MAP\\txt\\5.txt";
//         path = "C:\\Users\\luosy\\Desktop\\ht\\不同格式的MAP\\不同格式的MAP\\txt\\6.txt";
//        path = "C:\\Users\\luosy\\Desktop\\ht\\不同格式的MAP\\不同格式的MAP\\txt\\7.txt";
        logger.info("waferpath：" + path);
        return path;
    }

    public static void main(String[] args) {

        Map map = getWaferFileInfo("BPR296-18.wfp", "180", "DA-123-M");

        //transferArgs1(transferArgs1((long[]) map.get("BinList"), 53, 58), 58, 53);
//        transferAngle((long[]) map.get("BinList"), "90");
    }

    private static void loadBinConfig() {
//        SigmaPlusWaferTransfer.class.getResource("SigmaPlusWaferBinCode.properties")
        try {
            InputStream in = HitachiWaferUtil.class.getResourceAsStream("HTDB800WaferBinCode.properties");
            prop.load(in);
        } catch (Exception e) {

        }
    }

    private static Map transferKey2Map(String key) {
        Map map = new HashMap();
        if (key != null && !"".equals(key)) {
            String[] keys = key.split(",");
            for (String keyTmp : keys) {
                String[] values = keyTmp.split("=");
                map.put(values[0], values[1]);
            }
        }
        return map;
    }

    private static String trimCP1Type(String fileLine) {
        String head = fileLine.substring(0, 3);
        try {
            int headI = Integer.parseInt(head);
            if (headI < 1000 && headI > -1) {
                return fileLine.substring(3);
            }
        } catch (Exception e) {
            return fileLine;
        }
        return fileLine;
    }

    private static String trimdatType(String fileLine) {
        String head = fileLine.substring(0, 2);
        try {
            int headI = Integer.parseInt(head);
            if (headI < 100 && headI > -1) {
                fileLine = fileLine.substring(2);
//                fileLine = fileLine.replaceAll("/", "1").replaceAll("F", "X");
                return fileLine;
            }
        } catch (Exception e) {
            return fileLine;
        }

        return fileLine;
    }

    private static String transferSMIC(String fileLine) {
//        fileLine = fileLine.replaceAll("A", "1");
        return fileLine;
    }

    private static String trimTmaType(String fileLine) {
        String head = fileLine.substring(0, 3);
        try {
            int headI = Integer.parseInt(head);
            if (headI < 999 && headI > -1) {
                fileLine = fileLine.substring(4);
//                fileLine = fileLine.replaceAll("P", "1").replaceAll("F", "X");
                fileLine = fileLine.replaceAll(" ", "").replaceAll("\\.", " ");
                return fileLine;
            }
        } catch (Exception e) {
            return fileLine;
        }
        return fileLine;
    }

    private static String trimTmbType(String fileLine) {
        String head = fileLine.substring(0, 3);
        try {
            int headI = Integer.parseInt(head);
            if (headI < 999 && headI > -1) {
                fileLine = fileLine.substring(4);
//                fileLine = fileLine.replaceAll("P", "1").replaceAll("F", "X");
                fileLine = fileLine.replaceAll("10", "}").replaceAll("11", "]").replaceAll("12", ")").replaceAll("13", "(");
                fileLine = fileLine.replaceAll("   ", ".").replaceAll(" ", "").replaceAll("\\.", " ");
//                System.out.println(fileLine);
                fileLine = fileLine.replaceAll("01", "1").replaceAll("02", "2").replaceAll("03", "3").replaceAll("04", "4");
                fileLine = fileLine.replaceAll("05", "5").replaceAll("06", "6").replaceAll("07", "7").replaceAll("08", "8").replaceAll("09", "9");

                System.out.println(fileLine);
                return fileLine;
            }
        } catch (Exception e) {
            return fileLine;
        }

        return fileLine;
    }

    private static String trimOutType(String fileLine) {
        String head = fileLine.substring(0, 3);
        try {
            int headI = Integer.parseInt(head);
            if (headI < 999 && headI > -1) {
                fileLine = fileLine.substring(3);
//                fileLine = fileLine.replaceAll("P", "1").replaceAll("F", "X");
                return fileLine;
            }
        } catch (Exception e) {
            return fileLine;
        }
        return fileLine;
    }

    private static String trimWfpType(String fileLine) {
        String head = fileLine.substring(0, 3);
        try {
            int headI = Integer.parseInt(head);
            if (headI < 999 && headI > -1) {
                fileLine = fileLine.substring(7);
//                fileLine = fileLine.replaceAll("P", "1").replaceAll("F", "X");
//                fileLine = fileLine.replaceAll(" ", "").replaceAll("\\.", " ");
                return fileLine;
            }
        } catch (Exception e) {
            return fileLine;
        }
        return fileLine;
    }

    private static String trimXmlType(String fileLine) {
        fileLine = fileLine.replaceAll("]]></Row>", "").replaceAll("<Row><!\\[CDATA\\[", "");

        return fileLine;
    }
    static boolean nextRowIgnore = false;

    private static String trimTxtType(String fileLine) {
        if (!fileLine.contains("RowData") && fileLine.contains(":")) {
            return "";
        }

        if (fileLine.contains("!") || fileLine.contains("-+----+----+")) {
            return "";
        }
        if (fileLine.contains("→") || fileLine.contains("↓")) {
            return "";
        }
        if (fileLine.contains("Sample")) {
            nextRowIgnore = true;
            return "";
        }
        if (nextRowIgnore) {
            nextRowIgnore = false;
            return "";
        }
        if (fileLine.length() < 15) {
            return "";
        }
        if (fileLine.contains("RowData")) {
            return fileLine.replaceAll("RowData:", "").replaceAll("___", ".").replaceAll("000", "0").replaceAll("036", "*").replaceAll(" ", "");
        }

        String head = fileLine.substring(0, 3);
        try {
            int headI = Integer.parseInt(head);
            if (headI < 999 && headI > -1) {
                fileLine = fileLine.substring(3);
//                fileLine = fileLine.replaceAll("P", "1").replaceAll("F", "X");
//                fileLine = fileLine.replaceAll(" ", "").replaceAll("\\.", " ");
                return fileLine;
            }
        } catch (Exception e) {
            head = fileLine.substring(0, 8);
            if (head.contains("|") || head.contains("+")) {
                fileLine = fileLine.substring(8).replaceAll("99", "c").replaceAll("   ", ".").replaceAll("  ", "").replaceAll(" ", "").replaceAll("\\.", " ");
            }
        }
        return fileLine;
    }
}
