package cn.tzauto.octopus.secsLayer.resolver.ht;


import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.common.resolver.TransferUtil;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 *
 * @author xuc
 */
public class HongTengRecipeUtil {

    private static final Logger logger = Logger.getLogger(HongTengRecipeUtil.class.getName());

    public static List transferRcpFromDB(String recipePath) {
        String ppbody = TransferUtil.getPPBody(0, recipePath).get(0).toString();
        List<RecipePara> recipeParas = new ArrayList<>();
        if (!ppbody.isEmpty() && ppbody != "") {
            String[] ppbodys = ppbody.split("\\;");
            System.out.println(ppbodys.length);
            for (int i = 0; i < ppbodys.length; i++) {
                String[] values = ppbodys[i].split("\\:");
                values[0] = ppbodys[i].split("\\:")[0];//recipeParaName 
                String recipeParaName = values[0];
                System.out.println(recipeParaName);
                values[1] = ppbodys[i].split("\\:")[1];//recipeParaValues
                String[] paraValues = values[1].split("\\,");
                for (int j = 0; j < paraValues.length; j++) {
                    RecipePara recipePara = new RecipePara();
                    recipePara.setParaName(values[0]);
                    recipePara.setSetValue(paraValues[j]);
                    recipeParas.add(recipePara);
                }
            }
        } else {
            logger.info("ppbody为空！");
        }
        return recipeParas;
    }

    public static List transfer7200RcpFromDB(String recipePath) {
        File file = new File(recipePath);
        List<RecipePara> recipeParas = new ArrayList<>();
        if (file.exists()) {
            try {
                FileReader reader = new FileReader(file);
                BufferedReader br = new BufferedReader(reader);
                String line = "";
                String paraName = "";
                String paraValue = "";
                try {
                    while ((line = br.readLine())!= null) {
                        RecipePara recipePara = new RecipePara();
                        System.out.println(line);
                        if (line.contains(":") && !line.contains("||") && !line.contains("=")) {
                            String [] values = line.split("\\:");
                            paraName = values[0];
                            paraValue = values[1];
                            recipePara.setParaName(paraName);
                            recipePara.setSetValue(paraValue);
                            recipeParas.add(recipePara);
                        }
                    }
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            } catch (FileNotFoundException ex) {
                ex.printStackTrace();
            }
        }
        return recipeParas;
    }

    public static void main(String args[]) {
        String recipePath = "D:\\JCET-A-TQFP-14X14-100L-1.4.txt";
        List<RecipePara> recipeParas = transfer7200RcpFromDB(recipePath);
        System.out.println(recipeParas.size());
        for (int i = 0; i < recipeParas.size(); i++) {
            System.out.println(recipeParas.get(i).getParaName().toString() + "======" + recipeParas.get(i).getSetValue().toString());
        }
        
    }
}
