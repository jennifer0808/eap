/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.common.resolver;

import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.common.resolver.asm.AsmAD8312RecipeUtil;
import cn.tzauto.octopus.biz.recipe.domain.Recipe;
import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.common.util.tool.FileUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.List;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;

/**
 *
 * @author luosy
 */
public class ASMAD8312FCRecipeEdit extends RecipeTransfer {

    private static final Logger logger = Logger.getLogger(ASMAD8312FCRecipeEdit.class.getName());

    public static void main(String[] args) {

        // getUniqueRecipePara("D:\\RECIPE\\YYA13FCQ016016.txt");
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        RecipeService recipeService = new RecipeService(sqlSession);
        List<RecipePara> recipeParas = recipeService.searchRcpParaByRcpRowIdAndParaCode("1525bf38-f30d-4759-b9f7-6eee9aa73091", null);
        sqlSession.close();
        List<String> list = AsmAD8312RecipeUtil.setGoldPara(recipeParas, "D:\\RECIPE\\YYA13FCQ016016.txt", "ASMAD8312FC");
        for (int i = 0; i < list.size(); i++) {
            System.out.println(i + "||||" + list.get(i));
        }
        writeRecipeFile(list, "D:\\RECIPE\\tmp");
        copyAndEditZipFile("D:\\RECIPE\\", "YYA13FCQ016016");
        FileUtil.deleteAllFilesOfDir(new File("D:\\RECIPE\\" + "tmpzip"));
    }

    @Override
   public void editRecipeFile(Recipe recipe, String deviceType, String localRecipeFilePath) {
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        RecipeService recipeService = new RecipeService(sqlSession);
        List<RecipePara> recipeParas = recipeService.searchRcpParaByRcpRowIdAndParaCode(recipe.getId(), null);
        sqlSession.close();
        List<String> list = AsmAD8312RecipeUtil.setGoldPara(recipeParas, localRecipeFilePath, deviceType);
        String recipeParentPath = localRecipeFilePath.substring(0, localRecipeFilePath.lastIndexOf("\\"));
        writeRecipeFile(list, recipeParentPath + "\\tmp");
        copyAndEditZipFile(recipeParentPath, recipe.getRecipeName());
        FileUtil.deleteAllFilesOfDir(new File(recipeParentPath + "\\tmpzip"));
    }
    private static final byte[] BUFFER = new byte[4096];

    private static void copy(InputStream input, OutputStream output) throws IOException {
        int bytesRead;
        while ((bytesRead = input.read(BUFFER)) != -1) {
            output.write(BUFFER, 0, bytesRead);
        }
    }

    public static void copyAndEditZipFile(String srcFilePath, String fileName) {

        try {
            FileUtil.renameFile(srcFilePath + fileName + ".txt", srcFilePath + "tmpzip");
            //"D:\\RECIPE\\YYA13FCQ016016.txt", "D:\\RECIPE\\TMP.txt"
            // read war.zip and write to append.zip
            ZipFile war = new ZipFile(srcFilePath + "tmpzip");
            ZipArchiveOutputStream append = new ZipArchiveOutputStream(new FileOutputStream(srcFilePath + fileName + ".txt"));

            // first, copy contents from existing war
            Enumeration<?> entries = war.getEntries();
            while (entries.hasMoreElements()) {
                ZipArchiveEntry e = (ZipArchiveEntry) entries.nextElement();
                if (e.getName().contains("McPara_Export.txt")) {
                    continue;
                }
                System.out.println("copy: " + e.getName());
                append.putArchiveEntry(e);
                if (!e.isDirectory()) {
                    copy(war.getInputStream((ZipArchiveEntry) e), append);
                }
                append.closeArchiveEntry();
            }

            // now append some extra content
            File file = new File(srcFilePath + "tmp");
            InputStream in = new FileInputStream(file);
            ZipArchiveEntry e = new ZipArchiveEntry(file, "McPara_Export.txt");
            System.out.println("append: " + e.getName());
            append.putArchiveEntry(e);
//            IOUtils.copy(in, append);
            //append.write("42\n".getBytes());
            append.closeArchiveEntry();

            // close
            war.close();
            append.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<RecipePara> transferRecipeParaFromDB(String filePath, String deviceType) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
