/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.secsLayer.resolver.hitachi;

import cn.tzauto.octopus.biz.device.service.DeviceService;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.common.util.ftp.HtFtpUtil;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;

import java.io.*;
import java.time.Instant;
import java.util.*;

/**
 * @author luosy
 */
public class HitachiWaferUtil {


    static String waferMappingPath = "D:\\WAFERMAPPING";
    static String waferSavePath = "D:\\WAFERMAPPING_RESULT";
    private static final Logger logger = Logger.getLogger(HitachiWaferUtil.class);
    private static Properties prop = new Properties();
//    private static String maxColumnCount = "0";

    public static Map getWaferFileInfo(String waferId, String angle, String deviceCode) {
        Map resultMap = new HashMap();
        InputStream in = null;
        BufferedReader br = null;
        List<String> list = new ArrayList<>();
        SqlSession sqlSession = null;
        UUID uuid = UUID.randomUUID();
        String random = uuid.toString();
        try {
            sqlSession = MybatisSqlSession.getSqlSession();
            DeviceService deviceService = new DeviceService(sqlSession);
            String waferName = waferId.split("-")[0];
            String remotePath = deviceService.queryWaferPath(waferName);
            if (StringUtils.isEmpty(remotePath)) {
                logger.warn(waferId + "在数据库中没有对应的文件地址！！！");
                String path = getPath(waferId, deviceCode);
                File file = new File(path);
                if (file.exists()) {
                    in = new FileInputStream(file);
                    br = new BufferedReader(new InputStreamReader(in, "UTF-8"));
                    String tmpString = null;
                    br.readLine();
                    br.readLine();
                    br.readLine();
                    while ((tmpString = br.readLine()) != null) {
                        list.add(tmpString);
                    }
                } else {
                    logger.error(path + ",该路径下没有文件");
                    return null;
                }
//                remotePath = HtFtpUtil.getMapping(waferName);
            } else {

                logger.info(waferId + "-->ftp压缩文件地址为：" + remotePath);
                String localPath = HtFtpUtil.tempPath + random + remotePath.substring(remotePath.lastIndexOf("/"));
                HtFtpUtil.downloadFile(localPath, remotePath);
                boolean unrar = HtFtpUtil.unrar(localPath, HtFtpUtil.tempPath + random);
                if (unrar) {
                    File file = HtFtpUtil.getFile(new File(HtFtpUtil.tempPath + random), waferId);
                    in = new FileInputStream(file);
                    br = new BufferedReader(new InputStreamReader(in, "UTF-8"));
                    String tmpString = null;
                    br.readLine();
                    br.readLine();
                    br.readLine();
                    while ((tmpString = br.readLine()) != null) {
                        list.add(tmpString);
                    }
                }
            }

        } catch (Exception e) {
            logger.error("获取mapping文件失败", e);
            return null;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            try {
                FileUtils.deleteDirectory(new File(HtFtpUtil.tempPath + random));
            } catch (IOException e) {
            }
            sqlSession.close();
        }

        String[] results = new String[list.size()];
        list.toArray(results);

        resultMap.put("BinList", results);
        resultMap.put("RowCountInDieIncrements", results.length);
        resultMap.put("ColumnCountInDieIncrements", results[0].length());
        resultMap.put("ProcessDieCount", list.size());
        return resultMap;
//
//        loadBinConfig();
//
////        String lotID = "lotID";
////        String waferNo = "waferID";
////        if (waferId.contains("-")) {
////            lotID = waferId.split("-")[0];
////        }
//
//        String[] arr = getPath(waferId, deviceCode);
//        String waferFilePath = arr[0];
//        String savePath = arr[1];
//        BufferedWriter bw = null;
//        BufferedReader br = null;
//        try {
//            String cfgline = null;
//
//            int rowCount = 0;
//            String columnCount = "";
//
//            File cfgfile = new File(waferFilePath);
//            if (!cfgfile.exists()) {
//                logger.error("文件不存在：" + waferFilePath);
//                return null;
//            }
//            bw = new BufferedWriter(new OutputStreamWriter(FileUtils.openOutputStream(new File(savePath))));
//            br = new BufferedReader(new InputStreamReader(new FileInputStream(cfgfile), "UTF-8"));
//            List<String> list = new ArrayList<>();
//            while ((cfgline = br.readLine()) != null) {
//                list.add(cfgline);
//            }
//            if (list.size() == 0) {
//                logger.error("文件里面没有内容");
//                return null;
//            }
//            list = parseList(list);
//
//            rowCount = list.size();
//            int col = list.get(0).length();
//            columnCount = columnCount + col;
//            String[] bins = new String[list.size()];
//            for (int i = 0; i < list.size(); i++) {
//                bins[i] = list.get(i);
//            }
//            String[] results = transferAngle(bins, angle, rowCount, col);
//            resultMap.put("BinList", results);
//            bw.write("Wafer_ID : " + waferId + "\n");
//            bw.write("Flat_Notch : Bottom" + "\n");
//            bw.write("" + "\n");
//            for (int i = 0; i < results.length; i++) {
//                bw.write(results[i] + "\n");
//            }
//            bw.flush();
//            if ("90".equals(angle) || "270".equals(angle)) {
//                int tmp = rowCount;
//                rowCount = Integer.valueOf(columnCount);
//                columnCount = String.valueOf(tmp);
//            }
//            resultMap.put("RowCountInDieIncrements", rowCount);
//            resultMap.put("ColumnCountInDieIncrements", columnCount);
//            resultMap.put("ProcessDieCount", bins.length);
//
//        } catch (Exception e) {
//            logger.error("waferMap加载失败", e);
//            return null;
//        } finally {
//            if (br != null) {
//                try {
//                    br.close();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//            if (bw != null) {
//                try {
//                    bw.close();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//
//        }
//        return resultMap;
    }

    public static List<String> parseList(List<String> list) {
        List<Integer> numList = new ArrayList<>(); //记录所需行数
        List<Integer> indexList = new ArrayList<>(); //记录下标
        int temp = list.get(0).length();
        int count = 0;  //所需的行数
        //记录每行个数相等最多且连续的行
        for (int i = 0; i < list.size(); i++) {
            String s = list.get(i);
            if (temp == s.length()) {
                count++;
                if (i == list.size() - 1) {
                    numList.add(count);
                    indexList.add(i);
                    count = 0;
                }
            } else if (count > 5) {
                numList.add(count);
                indexList.add(i - 1);
                count = 1;
            } else {
                count = 1;
            }
            temp = s.length();
        }
        int index = 0;   //所需行的最后一个下标
        if (numList.size() == 1) {
            count = numList.get(0);
            index = indexList.get(0);
        } else if (numList.size() > 1) {
            List<Integer> tempList = new ArrayList<>(numList);
            Collections.sort(tempList);
            count = tempList.get(tempList.size() - 1);
            index = numList.indexOf(count);  //如果所需行数 有两个且相等，则解析有问题，正常取行数多的
            index = indexList.get(index);
        } else {
            logger.error("出错了"); //没有连续两行个数相等的
        }
        for (int i = (index - count + 1); i <= (index - count + 6); i++) {
            String s = list.get(i);
            if (s.contains("+")) {
                count = index - i;
                break;
            }
        }
        boolean flag = true;
        int start = (index - count + 1 + index) / 2;
        /**
         *    10|                 1  8  1  1  1  9  1  1  1  1  1  1  1  1  1  1  1  4  1  1  8  1  1  1  1  1  1  1  1  1  1  1  1  1  1  1  1  1  1  1  1  1  1  1  1  1  1  1  1  1  1  8  1  1  1  1
         *    11|              1  1  1  1  1  1  1  1  1  1  1  1  1  1  8  1  8  1  1  1  1  1  1  1  1  1  1  1  1  1 99  1  1  1  1  8  1  1  1  1  1  1  1  1  1  1  1  3  1  1  8  1  1  1  1  1  1  1
         *    12|           1  9  1  1  1  9  1  8  1  1  1  1  1  1  1  1  1  1  1  1  1  1  1  1  1  4  1  1  8  1  4  1  1  1  1  1  1  1  1  1  1  1  1  1  1  1  1  4  1  1  1  1  1  1  1  1  1  1  1
         *    13|           4  1  1  1  1  1  1  1  8  1  1  1  8  1  4  1  1  1  1  1  1  1  1  1  1  8  1  1  1  1  4  1  1  1  1  1  1  1  1  1  1  1  1  1  1  1  1  1  1  1  1  1  1  1  1  1  1  1  1  1
         *    14|        1  1  1  1  1  1  9  1  1  1  1  1  1  1  1  1  1  8  1  1  1  1  1  1  8  1  1  1  1  1  1  1  1  8  1  1  1  1  1  1  1  1  1  1  1  1  1  1  4  1  1  1  1  1  1  1  1  4  1  1  1
         *    15|        4  1  8  9  1  9  1  1  1  1  1  1  1  1  1  1  1  1  1  1  1  1  1  1  1  1  1  1  1  8  1  1  1  1  1  1  1  1  1  1  1  1  1  1  1  1  1  1  1  1  1  1  1  1  1  1  4  1  1  1  1  1
         *    16|        9  1  1  1  9  9  1  1  1  8  1  1  1  4  1  1  8  1  1  1  1  1  1  4  1  1  4  1  1  1  1  1  1  1  1  1  1  1  8  1  1  1  1  1  1  1  1  1  1  3  1  1  1  1  1  1  1  1  1  1  1  1
         *    17|        1  1
         *    剔除序号
         */
        temp = list.get(start).indexOf("|");
        int temp1 = list.get(start).indexOf("+");//取所需中间行的"-"和"+"位置
        if (temp <= 0 && temp1 < 0) {
            flag = false;
        } else {  //包含的话，说明格式如：   16|     11  ，剔除序号
            if (temp < 0) {
                temp = temp1;
            }
            for (int i = start; i <= (start + 5); i++) {
                String s = list.get(i);
                if (!(s.contains("|") || s.contains("+"))) {
                    flag = false;
                } else {
                    if (!(temp == s.indexOf("|") || temp == s.indexOf("+"))) {
                        flag = false;
                        logger.error("数据中有问题项");
                    }
                }
            }
        }
        if (flag) {
            for (int i = (index - count + 1); i <= index; i++) {
                list.set(i, list.get(i).substring(temp + 1));
            }
        }
        flag = true;
        int num = 0; //用以记录验证到了几个字符
        long l = Instant.now().toEpochMilli();
        indexList = new ArrayList<>();
        while (flag) {   //处理是否包含的序号，  有序号，则剔除
            num++;
            int[] ints = numHandle(list, index, count, num, indexList);
            if (ints[0] < (count / 2)) {
                flag = false;
            }
            if (num > 10) {  //有可能全是数字，没有序号
                num = 0;
                flag = false;
            }
            long l1 = Instant.now().toEpochMilli();
            if (l1 - l > 20000L) {
                logger.error("超时了");
                flag = false;
            }
        }
        /**
         *     000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000111111111111111111111111111111111111111111111111111111111111111111111111111111111
         *     000000000111111111122222222223333333333444444444455555555556666666666777777777788888888889999999999000000000011111111112222222222333333333344444444445555555555666666666677777777778
         *     123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890
         * 001                                                                                      1111141111
         * 002                                                                               111111111111111111111111
         * 003                                                                           11111111111111111111111111111111
         * 004
         * 移除上面三行
         */
        if (num > 1) {
            if (indexList.size() > 0) {
                Collections.sort(indexList);
                temp = indexList.get(0);
                temp1 = 1;
                List<Integer> integers = new ArrayList<>();
                for (int i = 1; i < indexList.size(); i++) {
                    Integer integer = indexList.get(i);  //4
                    if (integer == temp) {
                        temp1++;
                        if (i == indexList.size() - 1) {
                            integers.add(temp);
                        }
                    } else {
                        if (temp1 > 1) {
                            integers.add(temp);
                        }
                        temp1 = 1;
                    }
                    temp = integer;
                }
                count = index - integers.get(integers.size() - 1);   //移除上面不需要的几行
            }

            for (int i = (index - count + 1); i <= index; i++) {
                String s = list.get(i);
                list.set(i, s.substring(num));
            }
        }
        List<String> resultList = new ArrayList<>();
        indexList = new ArrayList<>();
        temp = 0;
        flag = false;
        /**
         * 判断xml 匹配几个字符
         */
        int charNum = 1;
        if (list.get(index - count + 1).startsWith("<Row><![CDATA[")) {
            for (int i = 0; i <= (index - count + 1); i++) {
                String s = list.get(i);
                if (s.startsWith("<Bin")) {
                    int index1 = s.indexOf("BinCode=\"");
                    String substring = s.substring(index1 + 9);
                    String substring1 = substring.substring(0, substring.indexOf("\""));
                    charNum = substring1.length();
                    if (charNum == 0) {
                        charNum = 1;
                        logger.error("xml 多字符解析错误");
                    }
                    break;
                }
            }
        }
        /**
         *       111111111111111111111111111111111111111111111111111111111100
         *       555555554444444444333333333322222222221111111111000000000099
         *       765432109876543210987654321098765432109876543210987654321098
         *       ------------------------------------------------------------
         *                       MMMMMMMMMMMMMMMMMM
         *                       移除 -------------------及以上部分
         */
        for (int i = (index - count + 1); i <= index; i++) {
            String s = list.get(i);
            if (s.contains("-") || s.contains("+")) {
                flag = true;
                temp++;
                indexList.add(i);
            }
            if (s.startsWith("<Row><![CDATA[")) {
                list.set(i, s.substring(14, s.length() - 9));
            }

            if (s.startsWith("RowData:")) {
                list.set(i, s.substring(8));
            }
        }
        if (flag && temp < (count / 4)) {
            if (indexList.size() == 1 && indexList.get(0) < (index - count + 8)) {
                count = index - indexList.get(0);
            } else if (temp > 4) {
                logger.error("数据有问题，请核实");
            } else {
                temp1 = 0;
                temp = 0;
                for (int i = 0; i < indexList.size(); i++) {
                    Integer integer = indexList.get(i);
                    if (integer < (index - count + 8)) {
                        temp1++;
                    } else if (integer > (index - 4)) {
                        temp++;
                    } else {
                        logger.error("数据有问题，请核实");
                    }
                }
                if ((temp + temp1) == indexList.size()) {
                    if (temp > 0) {
                        index = index - temp;
                    }
                    if (temp1 > 0) {
                        count = index - indexList.get(temp1 - 1);
                    }
                } else {
                    logger.error("数据有问题，请核实");
                }
            }
        }
        for (int i = (index - count + 1); i <= index; i++) {
            resultList.add(list.get(i));
        }
        handleMultiCharacter(resultList, charNum);
        handleSpaceToSpot(resultList);
        return resultList;
    }

    private static void handleSpaceToSpot(List<String> resultList) {
//        for (int i = 0; i < resultList.size(); i++) {
//            String s = resultList.get(i);
//            String temp = s.replaceAll(" ", ".");
//            resultList.set(i, temp);
//        }
        String spaceBin = ".";
        String s1 = resultList.get(0);
        String substring1 = s1.substring(0, 1);
        if (spaceBin.equals(substring1)) {
            return;
        }
        String substring2 = s1.substring(s1.length() - 1);

        String s2 = resultList.get(resultList.size() - 1);
        String substring3 = s2.substring(0, 1);
        String substring4 = s2.substring(s2.length() - 1);

        if (substring1.equals(substring2) && substring2.equals(substring3) && substring3.equals(substring4)) {
            for (int i = 0; i < resultList.size(); i++) {
                String s = resultList.get(i);
                String temp = s.replaceAll(substring1, spaceBin);
                resultList.set(i, temp);
            }
        }

    }

    private static void handleMultiCharacter(List<String> list, int charNum) {
        Map<String, String> binMap = new HashMap();
        binMap = transferKey2Map(String.valueOf(prop.get("ANY")));
        int index = list.size() / 2 - 2;
        int start = list.get(0).length() / 2 - 10;
        int num = 1; //默认为但字符的
        if (start >= 20) {
            List<Integer> indexList = new ArrayList<>(); //记录空串的坐标
            Set<Integer> set = new HashSet<>();
            for (int i = index; i < index + 4; i++) {
                String[] arr = list.get(i).substring(start, start + 20).split("");
                for (int j = 0; j < arr.length; j++) {
                    if (arr[j].equals(" ")) {
                        indexList.add(j);
                    }
                }
                if (indexList.size() == 0) {
                    continue;
                }
                int temp = indexList.get(0);
                boolean first = true;
                int firstIndex = 0;
                for (int j = 1; j < indexList.size(); j++) {
                    Integer integer = indexList.get(j);
                    if (first) {
                        if (integer - 1 > temp) {
                            first = false;
                            firstIndex = integer;
                        }
                    } else {
                        if (integer - 1 > temp) {
                            set.add(integer - firstIndex);
                            firstIndex = integer;
                        }
                    }
                    temp = integer;
                }
                indexList = new ArrayList<>();
            }
            if (set.size() == 1) {
                for (Integer integer : set) {
                    num = integer;
                }
                int temp = list.get(0).length() % num;
                if (temp != 0) {
//                int delNum = num - temp; //暂时判断删除一个空格
                    // 处理 类似于这种的多字符     "aaa aaa aaa"  ,无法按照标准切割
                    num = temp;
                    for (int i = 0; i < list.size(); i++) {
                        String s = list.get(i);
                        String str = s.substring(0, s.length() - num);
                        String end = s.substring(s.length() - num);
                        StringBuffer stringBuffer = new StringBuffer(str);
                        for (int j = num; j < stringBuffer.length(); j = j + num) {
                            stringBuffer.deleteCharAt(j);
                        }
                        stringBuffer.append(end);
                        list.set(i, stringBuffer.toString());
                    }
                }
                logger.info("处理多字符");
            } else if (set.size() > 1) {
                logger.error("处理多字符出错" + set);
            } else {
                num = charNum;//xml 处理多字符
                logger.info("单字符");
            }
        }
        for (int i = 0; i < list.size(); i++) {
            String s = list.get(i);
            int length = s.length();
            StringBuilder sb = new StringBuilder();
            for (int j = 0; j < length; j = j + num) {
                String sub = s.substring(j, j + num);
                String bin = binMap.get(sub);
                if (bin == null || "".equals(bin)) {
                    logger.error("有没有匹配的BinCode:" + sub);
                    bin = " ";
                }
                sb.append(bin);
            }
            list.set(i, sb.toString());
        }
    }

    private static int[] numHandle(List<String> list, int index, int count, int num, List<Integer> indexList) {
        int sucess = 0;
        int fail = 0;
        List<Integer> tempList = new ArrayList<>();
        for (int i = (index - count + 1); i <= index; i++) {
            String s = list.get(i);
            try {
                Integer.parseInt(s.substring(num - 1, num));
                sucess++;
            } catch (NumberFormatException e) {
                fail++;
                tempList.add(i);
            }
        }
        if (sucess > (count / 2)) {
            indexList.addAll(tempList);  //成功的过多，怎加入indezList中，用以记录处理上面几行不需要的行
        }
        return new int[]{sucess, fail};

    }

    private static String[] transferAngle(String[] src, String angle, int row, int col) {
        if ("0".equals(angle)) {
            return src;
        }
        String[] tmp = transferArgs1(src, row, col);
        if ("90".equals(angle)) {
            return tmp;
        }
        tmp = transferArgs1(tmp, col, row);
        if ("180".equals(angle)) {
            return tmp;
        }
        tmp = transferArgs1(tmp, row, col);
        if ("270".equals(angle)) {
            return tmp;
        }
        logger.error("旋转角度数值有误：" + angle);
        return null;
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
        String[][] num = new String[row][col];
        for (int i = 0; i < row; i++) {
            String[] split = src[i].split("");
            for (int j = 0; j < col; j++) {
                num[i][j] = split[j];
            }
        }
        String[][] arr = new String[col][row];
        for (int i = 0; i < row; i++) {
            for (int j = 0; j < col; j++) {
                arr[j][i] = num[i][j];
            }
        }
        String[] result = new String[col];
        for (int i = 0; i < col; i++) {
            StringBuilder sb = new StringBuilder();
            for (int j = arr[i].length - 1; j >= 0; j--) {
                sb.append(arr[i][j]);
            }
            result[i] = sb.toString();
        }
        return result;
    }

    private static String getPath(String waferId, String deviceCode) {
//        String lot = waferId.split("-")[0];
        if (!deviceCode.endsWith("-M")) {
            deviceCode = deviceCode + "-M";
        }
        String path = waferMappingPath + "\\" + deviceCode + "\\" + waferId;

//        String savePath = waferSavePath + "\\" + deviceCode + "\\" + lot + "\\" + waferId;
//        String[] arr = new String[]{path, savePath};

//        path = "C:\\Users\\86180\\Desktop\\新建文件夹 (2)\\不同格式的MAP\\tmb\\NF10A-08.tmb";
//        path = "C:\\Users\\86180\\Desktop\\新建文件夹 (2)\\不同格式的MAP\\out\\H00H37-08.out";
//        path = "C:\\Users\\86180\\Desktop\\新建文件夹 (2)\\不同格式的MAP\\asc\\HYMJC-01.asc";
//        path = "C:\\Users\\86180\\Desktop\\新建文件夹 (2)\\不同格式的MAP\\cp1\\RCSYN-18.CP1";
//        path = "C:\\Users\\86180\\Desktop\\新建文件夹 (2)\\不同格式的MAP\\dat\\SI11494-03.dat";
//        path = "C:\\Users\\86180\\Desktop\\新建文件夹 (2)\\不同格式的MAP\\EMT\\P1B122-01.EMT";
//        path = "C:\\Users\\86180\\Desktop\\新建文件夹 (2)\\不同格式的MAP\\ETC\\BPR296-18.ETC";
//        path = "C:\\Users\\86180\\Desktop\\新建文件夹 (2)\\不同格式的MAP\\smic\\SL1460-08.smic";
//        path = "C:\\Users\\86180\\Desktop\\新建文件夹 (2)\\不同格式的MAP\\tma\\28-20111-0-01.tma";
//        path = "C:\\Users\\86180\\Desktop\\新建文件夹 (2)\\不同格式的MAP\\tmc\\862368-18.tmc";
//        path = "C:\\Users\\86180\\Desktop\\新建文件夹 (2)\\不同格式的MAP\\wfp\\S13195-18.wfp";
//         path = "C:\\Users\\86180\\Desktop\\新建文件夹 (2)\\不同格式的MAP\\WIN\\A738190-18.WIN";
//        path = "C:\\Users\\86180\\Desktop\\新建文件夹 (2)\\不同格式的MAP\\XML\\CP1042857-18.XML";
//        path = "C:\\Users\\86180\\Desktop\\新建文件夹 (2)\\不同格式的MAP\\UMC\\S1LP6-08.UMC";
//        path = "C:\\Users\\86180\\Desktop\\新建文件夹 (2)\\不同格式的MAP\\UTR\\P1M742-08.UTR";
//        path = "C:\\Users\\86180\\Desktop\\新建文件夹 (2)\\不同格式的MAP\\txt\\1.txt";
//        path = "C:\\Users\\86180\\Desktop\\新建文件夹 (2)\\不同格式的MAP\\txt\\2.txt";
//        path = "C:\\Users\\86180\\Desktop\\新建文件夹 (2)\\不同格式的MAP\\txt\\3.txt";
//        path = "C:\\Users\\86180\\Desktop\\新建文件夹 (2)\\不同格式的MAP\\txt\\4.txt";
//        path = "C:\\Users\\86180\\Desktop\\新建文件夹 (2)\\不同格式的MAP\\txt\\5.txt";
//         path = "C:\\Users\\86180\\Desktop\\新建文件夹 (2)\\不同格式的MAP\\txt\\6.txt";
//        path = "C:\\Users\\86180\\Desktop\\新建文件夹 (2)\\不同格式的MAP\\txt\\7.txt";
        logger.info("waferpath：" + path);
        return path;
    }

    public static void main(String[] args) {
        long l = Instant.now().toEpochMilli();
        Map map = getWaferFileInfo("HYKP12-15.xml", "270", "DA-123-M");
        System.out.println(map);
        String[] binList = (String[]) map.get("BinList");
        String BinList = "";
        for (int i = 0; i < binList.length; i++) {
            BinList = BinList + binList[i];
        }
        long l1 = Instant.now().toEpochMilli();
        System.out.println(l1 - l);
        System.out.println(BinList);
        //transferArgs1(transferArgs1((long[]) map.get("BinList"), 53, 58), 58, 53);
//        transferAngle((long[]) map.get("BinList"), "90");
    }

    private static void loadBinConfig() {
//        SigmaPlusWaferTransfer.class.getResource("SigmaPlusWaferBinCode.properties")
        try {
            InputStream in = HitachiWaferUtil.class.getClassLoader().getResourceAsStream("HTDB800WaferBinCode.properties");
            prop.load(in);
        } catch (Exception e) {

        }
    }

    private static Map transferKey2Map(String key) {
        Map map = new HashMap();
        if (StringUtils.isEmpty(key)) {
            logger.error("没有BinCode");
            return map;
        }
        String[] keys = key.split(",");
        for (String keyTmp : keys) {
            String[] values = keyTmp.split("=");
            map.put(values[0], values[1]);
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
