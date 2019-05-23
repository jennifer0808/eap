///*
// * To change this template, choose Tools | Templates
// * and open the template in the editor.
// */
//package cn.tzauto.octopus.secsLayer.resolver.yamada;
//
//import cn.tzauto.octopus.biz.recipe.domain.RecipeTemplate;
//import cn.tzauto.octopus.biz.recipe.service.RecipeService;
//import cn.tzauto.octopus.common.resolver.TransferUtil;
//import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
//import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
//
//import java.io.BufferedReader;
//import java.io.File;
//import java.io.FileInputStream;
//import java.io.InputStreamReader;
//import java.util.ArrayList;
//import java.util.Date;
//import java.util.LinkedList;
//import java.util.List;
//import org.apache.ibatis.session.SqlSession;
//
///**
// *
// * @author njtz
// *
// */
//public class YamadRecipeUtil {
//
//    public static List<RecipeTemplate> yamadaRcpCfg() {
//        File cfgfile = new File("D:\\SVN\\documents\\10.参考资料\\Equipment Secs Docs\\MOLD\\YAMADA\\YAMADAcfg.csv");
//        List recipeParaList = new LinkedList();
//        List<RecipeTemplate> recipeTemplates = new ArrayList<>();
//        try {
//            BufferedReader br = new BufferedReader(new InputStreamReader(
//                    new FileInputStream(cfgfile), "GBK"));
//            String cfgline = null;
//            while ((cfgline = br.readLine()) != null) {
//                String[] cfg = cfgline.split(",");
//                RecipePara recipePara = new RecipePara();
//                RecipeTemplate recipeTemplate = new RecipeTemplate();
//                recipeTemplate.setParaCode(cfg[0]);
//                recipeTemplate.setParaName(cfg[1]);
//                recipeTemplate.setParaUnit(cfg[2]);
//                recipePara.setParaCode(cfg[0]);
//                recipePara.setParaName(cfg[1]);
//                recipePara.setParaMeasure(cfg[2]);
//                recipePara.setSetValue("");
//                cfg[4] = cfg[4].replaceAll(" ", "");
//                String tmp[] = cfg[4].split("~");
//                recipePara.setMinValue(tmp[0]);
//                recipePara.setMaxValue(tmp[1]);
//                recipeParaList.add(recipePara);
//                recipeTemplate.setMinValue(tmp[0]);
//                recipeTemplate.setMaxValue(tmp[1]);
//                recipeTemplate.setDeviceVariableType("RecipePara");
//                recipeTemplate.setCreateBy("lsy");
//                recipeTemplate.setCreateDate(new Date());
//                recipeTemplate.setUpdateBy("lsy");
//                recipeTemplate.setUpdateDate(new Date());
//                recipeTemplate.setDelFlag("0");
//                recipeTemplate.setDeviceTypeCode("YAMADA170T");
//                recipeTemplate.setDeviceTypeId("d1e93ca70c17471f923bf4665ae974a3");
//                recipeTemplate.setDeviceTypeName("YAMADA170T");
//                recipeTemplates.add(recipeTemplate);
//            }
//            br.close();
//            // fr.close();
//        } catch (Exception e) {
//            System.out.print(e);
//        }
//        return recipeTemplates;
//    }
//
//    public static List transferYamadaRcp(String recipePath) {
//        String ppbody = TransferUtil.getPPBody(0, recipePath).get(0).toString();
//        String[] ppbodys = ppbody.split(",");
//        System.out.println("ppbodys size:" + ppbodys.length);
//        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
//        RecipeService recipeService = new RecipeService(sqlSession);
//        List<RecipeTemplate> recipeTemplates = recipeService.searchRecipeTemplateByDeviceTypeCode("YAMADA170T", "RecipePara");
//        sqlSession.close();
//        List<RecipePara> recipeParas = new ArrayList<>();
//        //防止ppbodys的大小超过模板里配置的参数数量
//        int size = recipeTemplates.size() > ppbodys.length ? ppbodys.length : recipeTemplates.size();
//        for (int i = 0; i < size; i++) {
//            RecipePara recipePara = new RecipePara();
//            recipePara.setParaCode(recipeTemplates.get(i).getParaCode());
//            recipePara.setParaDesc(recipeTemplates.get(i).getParaDesc());
//            recipePara.setParaMeasure(recipeTemplates.get(i).getParaUnit());
//            recipePara.setParaName(recipeTemplates.get(i).getParaName());
//            recipePara.setParaShotName(recipeTemplates.get(i).getParaShotName());
//            recipePara.setSetValue(ppbodys[i]);
//            recipeParas.add(recipePara);
//        }
//
//        return recipeParas;
//    }
//
//    public static void main(String aar[]) {
//        String recipePath = "C:\\Users\\wjy\\Desktop\\D3500-6035\\QFN0.75-0.85\\QFN0.75-0.85_V0.txt";//QFN-250x70-0.55-700_V0.txt    QFN-25070-0.55-G770_V0.txt
//        List<RecipePara> recipeParas = transferYamadaRcp(recipePath);
//        System.out.println(recipeParas.size());
//    }
//}
