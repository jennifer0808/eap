/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.common.resolver.hitachi;

import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

/**
 *
 * @author 
 */
public class DB810Util {

    private static final Logger logger = Logger.getLogger(DB810Util.class.getName());

    public static List<RecipePara> transferFromDB(String filePath, String recipeName) {
        ZipInputStream zis = null;
        List<RecipePara> recipeParas = new ArrayList<>();
        try {
            ZipFile zipFile = new ZipFile(filePath);
            zis = new ZipInputStream(new FileInputStream(filePath));
            ZipEntry ze = null;
            while ((ze = zis.getNextEntry()) != null) {
                if (ze.getName().contains(recipeName)) {
                    InputStream is = zipFile.getInputStream(ze);
                    BufferedReader br = new BufferedReader(new InputStreamReader(is));
                    String line = "";
                    String paraName = "";
                    String paraValue = "";
                    String maxValue = "";
                    String minValue = "";
                    int count = 1;
                    while ((line = br.readLine()) != null) {
                        RecipePara recipePara = new RecipePara();
                        if (count == 1) {
                            recipePara.setParaName("");
                            recipePara.setSetValue("");
                            recipePara.setMaxValue("");
                            recipePara.setMinValue("");
                            recipeParas.add(recipePara);
                            count++;
                            continue;
                        }
                        if (line.contains(",")) {
                            String[] paras = line.split("\\,");
                            paraName = paras[0];
                            paraValue = paras[1];
                            maxValue = paras[2];
                            minValue = paras[3];
                            recipePara.setParaName(paraName);
                            recipePara.setSetValue(paraValue);
                            recipePara.setMaxValue(maxValue);
                            recipePara.setMinValue(minValue);
                            recipeParas.add(recipePara);
                            count++;
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return recipeParas;
    }
     public static void main(String args[]) {
        List<RecipePara> recipeParas = transferFromDB("D:\\RECIPE\\BGA@GD06BD180747B-A-X-12-3.tgz", "BGA@GD06BD180747B-A-X-12-3.tgz");
        for (int i = 0; i < recipeParas.size(); i++) {
            System.out.println(recipeParas.get(i).getParaName().toString() + "=" + recipeParas.get(i).getSetValue().toString());
        }
        System.out.println(recipeParas.size());
    }
}