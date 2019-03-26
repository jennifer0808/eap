/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.secsLayer.resolver.hitachi;

import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.biz.recipe.domain.RecipeTemplate;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.ibatis.session.SqlSession;

/**
 *
 * @author Wang Danfeng
 */
public class DB730Util {

    int buffersize = 2048;
    static Map<String,String> recipeNameAndID=new HashMap<String,String>();
    public static void setRecipeNameAndID(){
        
    }

    //解压zip格式的压缩文件
//    public static void forDB730(String filePath) {
//        ZipFile zipFile = null;
//        ZipInputStream zis = null;
//        InputStream is = null;
//        //BufferedReader br=null;
//        FileOutputStream fos = null;
//        try {
//            zipFile = new ZipFile(filePath);
//            zis = new ZipInputStream(new FileInputStream(filePath));
//            ZipEntry ze = null;
//            //br=new BufferedReader(new InputStreamReader(new FileInputStream(filePath)));
//            String newFilePath = filePath + "copy";
//            File file = new File(newFilePath);
//            if (file.exists()) {
//                deleteFile(file);
//                System.out.println("11111111111111");
//                file.mkdir();
//            } else {
//                file.mkdir();
//            }
//            while ((ze = zis.getNextEntry()) != null && !ze.isDirectory()) {
//                System.out.println(ze.getName());
//                File zeFile = new File(ze.getName());
//                String[] names = ze.getName().split("/");
//                fos = new FileOutputStream(newFilePath + File.separator + names[0]);
//                is = zipFile.getInputStream(ze);
//                //is=new FileInputStream(zeFile);
//                byte[] buff = new byte[1024];
//                int len = 0;
//                while ((len = is.read(buff)) != -1) {
//                    fos.write(buff, 0, len);
//                }
//                //fos.flush();
//
//            }
//
//        } catch (IOException ex) {
//            Logger.getLogger(DB730Util.class.getName()).log(Level.SEVERE, null, ex);
//        } finally {
//            try {
//                zis.close();
//                //is.close();
//                if (fos != null) {
//                    fos.close();
//                }
//                //fos.close();
//            } catch (IOException ex) {
//                Logger.getLogger(DB730Util.class.getName()).log(Level.SEVERE, null, ex);
//            }
//        }
//    }
//
//    //解压tgz格式的文件
//    public static void ForDB7302(String file) {
//
//        DB730Util o = new DB730Util();
//
//        File f = o.deCompressTGZFile(file);
//
//        o.deCompressTARFile(f);
//    }
//
//    //file:源文件路径
//    public File deCompressTGZFile(String file) {
//        return deCompressGZFile(new File(file));
//    }
//
//    private File deCompressGZFile(File file) {
//        FileOutputStream out = null;
//        GzipCompressorInputStream gzIn = null;
//        try {
//            FileInputStream fin = new FileInputStream(file);
//            BufferedInputStream in = new BufferedInputStream(fin);
//            File outFile = new File(file.getParent() + File.separator
//                    + file.getName() + ".tar");
//            out = new FileOutputStream(outFile);
//            gzIn = new GzipCompressorInputStream(in);
//            final byte[] buffer = new byte[buffersize];
//            int n = 0;
//            while (-1 != (n = gzIn.read(buffer))) {
//                out.write(buffer, 0, n);
//            }
//            return outFile;
//        } catch (Exception e) {
//            e.printStackTrace();
//            return null;
//        } finally {
//            try {
//                out.close();
//                gzIn.close();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//    }
//
//    private void deCompressTARFile(File file) {
//        String basePath = file.getParent() + File.separator + file.getName() + "copy" + File.separator;
//        System.out.println(basePath);
//        File newfile = new File(basePath);
//        if (!newfile.exists()) {
//            newfile.mkdir();
//        }
//        TarArchiveInputStream is = null;
//        try {
//            is = new TarArchiveInputStream(new FileInputStream(file));
//            while (true) {
//                TarArchiveEntry entry = is.getNextTarEntry();
//                if (entry == null) {
//                    break;
//                }
//                if (entry.isDirectory()) {// 这里貌似不会运行到，跟ZipEntry有点不一样  
//                    new File(basePath + entry.getName()).mkdirs();
//                } else {
//                    FileOutputStream os = null;
//                    try {
//                        File f = new File(basePath + entry.getName());
//                        if (!f.getParentFile().exists()) {
//                            f.getParentFile().mkdirs();
//                        }
//                        if (!f.exists()) {
//                            f.createNewFile();
//                        }
//                        os = new FileOutputStream(f);
//                        byte[] bs = new byte[buffersize];
//                        int len = -1;
//                        while ((len = is.read(bs)) != -1) {
//                            os.write(bs, 0, len);
//                        }
//                        os.flush();
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    } finally {
//                        os.close();
//                    }
//                }
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        } finally {
//            try {
//                is.close();
//                file.delete();
//            } catch (IOException ex) {
//                Logger.getLogger(DB730Util.class.getName()).log(Level.SEVERE, null, ex);
//            }
//        }
//    }

    public static Map<String,String> transferFromFile(String filePath) {
        Map<String,String> recipeNameAndID=new HashMap<String,String>();
        File file = new File(filePath);
        String basePath = file.getParent() + File.separator + file.getName() + "copy" + File.separator;
//        //String basePath1 = file.getParent() + File.separator + file.getName() + File.separator;
//        System.out.println(basePath);
//        File newfile = new File(basePath);
//        if (!newfile.exists()) {
//            newfile.mkdir();
//        }
        TarArchiveInputStream is = null;
        try {
            is = new TarArchiveInputStream(new GzipCompressorInputStream(new FileInputStream(file)));
            while (true) {
                TarArchiveEntry entry = is.getNextTarEntry();
                if (entry == null) {
                    break;
                }
                if (!entry.getName().endsWith(".csv")) {
                    break;
                } else {
                    System.out.println(entry.getName());
                }
                if (entry.isDirectory()) {// 这里貌似不会运行到，跟ZipEntry有点不一样  
                    new File(basePath + entry.getName()).mkdirs();
                } else {

                    //FileOutputStream os = null;
                    try {
                          //解压文件的方式解读recipe.csv ,完成后删除文件
//                        File f = new File(basePath + entry.getName());
//                        System.out.println(f.getAbsolutePath());
//                        if (!f.getParentFile().exists()) {
//                            f.getParentFile().mkdirs();
//                        }
//                        if (!f.exists()) {
//                            f.createNewFile();
//                        }
//                        os = new FileOutputStream(f);
//                        byte[] bs = new byte[2048];
//                        int len = -1;
//                        while ((len = is.read(bs)) != -1) {
//                            os.write(bs, 0, len);
//                        }
//                          os.flush();
//                        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(f)));
//                        String lineStr = "";
//                        while ((lineStr = br.readLine()) != null) {
//                            System.out.println(lineStr);
//                        }
//                        br.close();

                        //直接读取流方法
                        byte[] bs = new byte[2048];
                        int len = -1;
                        ByteArrayOutputStream baos=new ByteArrayOutputStream();
                        InputStream input=null;
                        while ((len = is.read(bs)) != -1) {
                           baos.write(bs, 0, len);
                        }
                        byte[] bytes=baos.toByteArray();
                        input=new ByteArrayInputStream(bytes);
                        BufferedReader br=new BufferedReader(new InputStreamReader(input));
                        String linestr="";
                        int i=0;
                        while((linestr=br.readLine())!=null){
                            String[] linstrs=linestr.split(",");
                            if(linstrs.length>=2){
                                //System.out.println("**"+i+"**"+"["+linstrs[0]+","+linstrs[1]+"]");
                                recipeNameAndID.put(linstrs[0], linstrs[1]);
                            }
                            i++;
                        }
                        input.close();
                        br.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                        return null;
                    } 
                }
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            try {
                is.close();
                //file.delete();
                //删除解压后的文件
                //deleteFile(newfile);
                
            } catch (IOException ex) {
                Logger.getLogger(DB730Util.class.getName()).log(Level.SEVERE, null, ex);
                return null;
            }
        }
        return recipeNameAndID;

    }
    public static List transferFromDB(Map<String,String> paraMap, String deviceType) {
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        RecipeService recipeService = new RecipeService(sqlSession);
        List<RecipeTemplate> recipeTemplates = recipeService.searchRecipeTemplateByDeviceTypeCode(deviceType, "RecipePara");
        sqlSession.close();
//        List<String> paraNameList = new ArrayList<>();
//        for (int i = 0; i < recipeTemplates.size(); i++) {
//            paraNameList.add(recipeTemplates.get(i).getParaName());
//        }
        List<RecipePara> recipeParaList = new ArrayList<>(); 
        for(RecipeTemplate recipeTemplate :recipeTemplates){
            String key=recipeTemplate.getParaCode();
            if(key==null||"".equals(key)){
                continue;
            }
            RecipePara recipePara = new RecipePara();
                recipePara.setParaCode(recipeTemplate.getParaCode());
                recipePara.setParaName(recipeTemplate.getParaName());
                recipePara.setParaShotName(recipeTemplate.getParaShotName());
                recipePara.setSetValue(paraMap.get(key));
                recipePara.setMinValue(recipeTemplate.getMinValue());
                recipePara.setMaxValue(recipeTemplate.getMaxValue());
                recipePara.setParaMeasure(recipeTemplate.getParaUnit());
                recipeParaList.add(recipePara); 
        }
//        Set<Map.Entry<String, String>> entry = paraMap.entrySet();
//        for (Map.Entry<String, String> e : entry) {
//            if (paraNameList.contains(e.getKey())) {
//                RecipePara recipePara = new RecipePara();
//                recipePara.setParaCode(recipeTemplates.get(paraNameList.indexOf(e.getKey())).getParaCode());
//                recipePara.setParaName(recipeTemplates.get(paraNameList.indexOf(e.getKey())).getParaName());
//                recipePara.setParaShotName(recipeTemplates.get(paraNameList.indexOf(e.getKey())).getParaShotName());
//                recipePara.setSetValue(e.getValue());
//                recipePara.setMinValue(recipeTemplates.get(paraNameList.indexOf(e.getKey())).getMinValue());
//                recipePara.setMaxValue(recipeTemplates.get(paraNameList.indexOf(e.getKey())).getMaxValue());
//                recipePara.setParaMeasure(recipeTemplates.get(paraNameList.indexOf(e.getKey())).getParaUnit());
//                recipePara.setParaShotName(recipeTemplates.get(paraNameList.indexOf(e.getKey())).getParaShotName().toString());
//                recipeParaList.add(recipePara);       
//            }
//        }
        return recipeParaList;
    }

//    public static void deleteFile(File file) {
//        //File file=new File("C:\\recipe\\laser\\8-120824CB-D175-150-90-ST9.rcp1");
//        //String fileName=file.getName();
//        if (file.exists()) {
//            if (file.isDirectory()) {
//                File[] files = file.listFiles();
//                for (File f : files) {
//                    deleteFile(f);
//                }
//                System.out.println("删除" + file.getAbsolutePath() + file.delete());
//            } else {
//                System.out.println("删除" + file.getAbsolutePath() + file.delete());
//            }
//
//        }
//    }
//
//    public static List<String> getFileLists(String path) {
//        List<String> list = new ArrayList<String>();
//
//        File file = new File(path);
//        System.out.println(file.getPath());
//        String[] strName = file.list();
//        System.out.println(strName);
//
//        String name = "";
//        for (int i = 0; i < strName.length; i++) {
//            String filePath = path + File.separator + strName[i];
//            System.out.println(filePath);
//            list.add(filePath);
//        }
//
//        return list;
//    }

    public static void main(String[] args) throws FileNotFoundException {
        //DB730Util db=new DB730Util();
//        String path="‪C:\\recipe\\db\\DB730\\";
//        List<String> list=new ArrayList<String>();
//        list=DB730Util.getFileLists(path) ;
//        for(String str:list){
//            
//            DB730Util.forDB730(str);
//        }
        //filePath="C:\\recipe\\db\\ASMAD8312\\BD-DAOWLSH0677-2Wt.rcp";
        //DB730Util.forDB730("C:\\recipe\\db\\DB730\\BGA@BD-ACZX1000001-AW(U1)");
        //db.deCompressTGZFile(filePath);
        //FileInputStream fis=new FileInputStream(filePath);
        //DB730Util.ForDB7302("C:\\recipe\\db\\DB730\\BGA@BD-ACZX1000001-AW(U3)t");
        //测试readCsv
        List<RecipePara> recipeParaList = new ArrayList<>();
         try {
            Map<String,String> map=DB730Util.transferFromFile("C:\\recipe\\db\\DB730\\BGA@BD-ACZX1000001-AW(U3)t");
        recipeParaList=DB730Util.transferFromDB(map, "DB730");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
         for(RecipePara r:recipeParaList){
             System.out.println(r.getParaCode()+"***"+r.getParaName());
         }
//        System.out.println(map.get("268435456"));
//        for(Entry e:map.entrySet()){
//            System.out.println(e.getKey()+"**"+e.getValue());
//        }
    }
}
