/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.common.resolver.hitachi;

import cn.tzauto.octopus.biz.recipe.domain.RecipeTemplate;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
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
 */
public class DB800Util {
    
    int buffersize = 2048;
    static Map<String,String> recipeNameAndID=new HashMap<String,String>();


    public static Map<String,String> transferFromFile(String filePath) {
        Map<String,String> recipeNameAndID=new HashMap<String,String>();
        File file = new File(filePath);
        String basePath = file.getParent() + File.separator + file.getName() + "copy" + File.separator;
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

                    try {

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
                Logger.getLogger(DB800Util.class.getName()).log(Level.SEVERE, null, ex);
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
        return recipeParaList;
    }


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
//        List<RecipePara> recipeParaList = new ArrayList<>();
//         try {
//            Map<String,String> map=DB730Util.transferFromFile("C:\\recipe\\db\\DB730\\BGA@BD-ACZX1000001-AW(U3)t");
//        recipeParaList=DB730Util.transferFromDB(map, "DB730");
//        } catch (Exception ex) {
//            ex.printStackTrace();
//        }
//         for(RecipePara r:recipeParaList){
//             System.out.println(r.getParaCode()+"***"+r.getParaName());
//         }
//        System.out.println(map.get("268435456"));
//        for(Entry e:map.entrySet()){
//            System.out.println(e.getKey()+"**"+e.getValue());
//        }
          Map<String,String> map=DB800Util.transferFromFile("D:\\RECIPE\\BGA@GD06BD180747B-A-X-12-3.tgz");
          int i=0;
          for(String key:map.keySet()){
              System.out.println(i+"**"+key+"**"+map.get(key));
              i++;
          }
           List<RecipePara> recipeParaList = new ArrayList<>();
           recipeParaList=DB800Util.transferFromDB(map, "HITACHIDB810Z2");
           for(RecipePara r:recipeParaList){
             System.out.println(r.getParaCode()+"***"+r.getParaName());
         }
    }
}
