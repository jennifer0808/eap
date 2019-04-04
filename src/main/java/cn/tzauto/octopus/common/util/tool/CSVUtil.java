/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.common.util.tool;


import cn.tzauto.octopus.common.globalConfig.GlobalConstants;
import cn.tzauto.octopus.common.util.ftp.FtpUtil;
import org.apache.commons.beanutils.BeanUtils;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.*;


/**
 *
 * @author luosy
 */
public class CSVUtil {

    /**
     * 生成为CSV文件
     *
     * @param exportData 源数据List
     * @param map csv文件的列表头map
     * @param outPutPath 文件路径
     * @param fileName 文件名称
     * @return
     */
    public static String createCSVFile(List exportData, LinkedHashMap map, String outPutPath, String fileName, String deviceCode, String recipeName) {
        File csvFile = null;
        BufferedWriter csvFileOutputStream = null;
        try {
            File file = new File(outPutPath);
            if (!file.exists()) {
                file.mkdir();
            }
            // 定义文件名格式并创建
            csvFile = File.createTempFile(fileName, ".csv", new File(outPutPath));
            // UTF-8使正确读取分隔符","
            csvFileOutputStream = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(csvFile), "UTF-8"), 1024);
            // 写入文件头部

            csvFileOutputStream.write("Chamber Name.:" + deviceCode + "  Pattern NO.:" + recipeName + "  UserId.: ");
//            csvFileOutputStream.write("recipeName=123456");
            csvFileOutputStream.newLine();
            for (Iterator propertyIterator = map.entrySet().iterator(); propertyIterator.hasNext();) {
                Map.Entry propertyEntry = (Map.Entry) propertyIterator.next();
                csvFileOutputStream.write("" + (String) propertyEntry.getValue() != null ? (String) propertyEntry.getValue() : "" + "");
                if (propertyIterator.hasNext()) {
                    csvFileOutputStream.write(",");
                }
            }
            csvFileOutputStream.newLine();
            // 写入文件内容
            for (Iterator iterator = exportData.iterator(); iterator.hasNext();) {
                Object row = (Object) iterator.next();
                for (Iterator propertyIterator = map.entrySet().iterator(); propertyIterator.hasNext();) {
                    Map.Entry propertyEntry = (Map.Entry) propertyIterator.next();
                    csvFileOutputStream.write((String) BeanUtils.getProperty(row, (String) propertyEntry.getKey()));
                    if (propertyIterator.hasNext()) {
                        csvFileOutputStream.write(",");
                    }
                }
                if (iterator.hasNext()) {
                    csvFileOutputStream.newLine();
                }
            }
            csvFileOutputStream.flush();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                csvFileOutputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        String remotePath = "/RECIPE/cure/";
        FtpUtil.uploadFile(outPutPath+fileName+".csv", remotePath, fileName+".csv", GlobalConstants.ftpIP, GlobalConstants.ftpPort, GlobalConstants.ftpUser, GlobalConstants.ftpPwd);
        return csvFile.getName();
    }

    /**
     * 下载文件
     *
     * @param response
     * @param csvFilePath 文件路径
     * @param fileName 文件名称
     * @throws IOException
     */
    public static void exportFile(HttpServletResponse response, String csvFilePath, String fileName) throws IOException {
        response.setContentType("application/csv;charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=" + URLEncoder.encode(fileName, "UTF-8"));
        InputStream in = null;
        try {
            in = new FileInputStream(csvFilePath);
            int len = 0;
            byte[] buffer = new byte[1024];
            response.setCharacterEncoding("UTF-8");
            OutputStream out = response.getOutputStream();
            while ((len = in.read(buffer)) > 0) {
                out.write(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});
                out.write(buffer, 0, len);
            }
        } catch (FileNotFoundException e) {
            System.out.println(e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    /**
     * 删除该目录filePath下的所有文件
     *
     * @param filePath 文件目录路径
     */
    public static void deleteFiles(String filePath) {
        File file = new File(filePath);
        if (file.exists()) {
            File[] files = file.listFiles();
            for (int i = 0; i < files.length; i++) {
                if (files[i].isFile()) {
                    files[i].delete();
                }
            }
        }
    }

    /**
     * 删除单个文件
     *
     * @param filePath 文件目录路径
     * @param fileName 文件名称
     */
    public static void deleteFile(String filePath, String fileName) {
        File file = new File(filePath);
        if (file.exists()) {
            File[] files = file.listFiles();
            for (int i = 0; i < files.length; i++) {
                if (files[i].isFile()) {
                    if (files[i].getName().equals(fileName)) {
                        files[i].delete();
                        return;
                    }
                }
            }
        }
    }

    /**
     * 测试数据
     *

     */
//    @SuppressWarnings({"rawtypes", "unchecked"})
    public static String setCSVFile(List list, String deviceCode, String recipeName) {
        if (list == null || list.isEmpty()) {
            return "";
        }
        List<Map> exportData = new ArrayList();
        for (int i = 0; i <= list.size() - 10;) {
            Map<String, String> rowMap = new LinkedHashMap<>();
            rowMap.put("1", String.valueOf(i));
            rowMap.put("2", String.valueOf(list.get(i)));
            rowMap.put("3", String.valueOf(list.get(i + 9)));
            rowMap.put("4", String.valueOf(list.get(i + 2)));
            rowMap.put("5", String.valueOf(list.get(i + 3)));
            rowMap.put("6", String.valueOf(list.get(i + 4)));
            rowMap.put("7", String.valueOf(list.get(i + 5)));
            rowMap.put("8", String.valueOf(list.get(i + 6)));
            rowMap.put("9", String.valueOf(list.get(i + 7)));
            rowMap.put("10", String.valueOf(list.get(i + 8)));
            rowMap.put("11", String.valueOf(list.get(i + 1)));

            i = i + 10;
            exportData.add(rowMap);
        }
        LinkedHashMap map = new LinkedHashMap();
        map.put("1", "ProcessTime");
        map.put("2", "Time");
        map.put("3", "O2");
        map.put("4", "CH2");
        map.put("5", "CH3");
        map.put("6", "CH4");
        map.put("7", "CH5");
        map.put("8", "CH6");
        map.put("9", "CH7");
        map.put("10", "CH8");
        map.put("11", "Pressure");
        String path = "D:/CURE/";

        Date date = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        String createdate = sdf.format(date);

        String fileName = deviceCode + "-" + createdate.replace("-", "").replace(":", "");
        fileName = fileName.replace(" ", "");
        fileName = createCSVFile(exportData, map, path, fileName, deviceCode, recipeName);
        return fileName;
    }
    
    public static void main(String[] args) {
         String remotePath = "/RECIPE/cure/";
        FtpUtil.uploadFile("D:\\CURE\\T6000-20180508 1104052598662326829875674.csv", remotePath, "T6000-20180508 1104052598662326829875674.csv","172.17.173.9", "21", "rms", "xccdkj@123");
    }
}
