package cn.tzauto.octopus.common.log;

import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;
import org.apache.commons.io.FileUtils;

import java.io.*;

/**
 * Created by tzauto on 2019/6/8.
 */
public class LogMake {
    public static void main(String[] args) {
        String contains="?果?：";
        File file = new File("E://temp.log");
        FileOutputStream fos =null;
        BufferedWriter bw  =null;

        try {
             fos =  FileUtils.openOutputStream(file);

             bw = new BufferedWriter(new OutputStreamWriter(fos));

            File dir = new File("E:\\log");
            if(dir.isDirectory()){
                File[] list =  dir.listFiles();
                for (File file1 : list) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file1), "UTF-8"));
                    String tmpString = "";
                    while ((tmpString = br.readLine()) != null) {
                        if (tmpString.contains(contains)) {
                            bw.write(tmpString);
                        }
                    }
                }
                bw.flush();

            }
        } catch (IOException e) {
            e.printStackTrace();
        }finally {

        }
    }
}
