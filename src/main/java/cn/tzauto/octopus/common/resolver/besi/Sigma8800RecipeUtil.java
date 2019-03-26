/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.common.resolver.besi;

import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.biz.recipe.domain.RecipeTemplate;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.session.SqlSession;

/**
 *
 * @author 陈佳能
 */
public class Sigma8800RecipeUtil {

    public static List transferFromDB(Map map, String deviceType) {
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        RecipeService recipeService = new RecipeService(sqlSession);
        List<RecipeTemplate> recipeTemplates = recipeService.searchRecipeTemplateByDeviceTypeCode(deviceType, "RecipePara");
        sqlSession.close();
        List<RecipePara> recipeParaList = new ArrayList<>();
        String XMLData = (String) map.get("Value");
        String[] resultTemp = XMLData.split("</value>");
        String[] result = new String[resultTemp.length - 1];
        for (int i = 0; i < result.length; i++) {
            result[i] = resultTemp[i];
            System.out.println(result[i]);
        }
        String value = "";
        List<String> resultList = new ArrayList();
        for (String valueTemp : result) {
            String[] values = valueTemp.split(">");
            value = values[values.length - 1];
            resultList.add(value);
        }

        for (int j = 0; j < recipeTemplates.size(); j++) {
            RecipePara recipePara = new RecipePara();
            recipePara.setParaCode(recipeTemplates.get(j).getParaCode());
            recipePara.setParaMeasure(recipeTemplates.get(j).getParaUnit());
            recipePara.setParaName(recipeTemplates.get(j).getParaName());
            for (int i = j; i < recipeTemplates.size();) {
                if (i < resultList.size()) {
                    recipePara.setSetValue(resultList.get(i));
                    break;
                }
//                else{
//                    recipePara.setSetValue("--");
//                    break;
//                }
            }

            recipeParaList.add(recipePara);
        }
        return recipeParaList;
    }

    public static void main(String args[]) {
        Map map = new HashMap();
        map.put("Value", "<?xml version='1.0' encoding='ANSI_X3.4-1968'?> <!DOCTYPE datacon_parameters SYSTEM 'datacon_parameters.dtd'> <datacon_parameters>   <group id='100'>     <dataset>       <dataset_name>XD36-BD-150150B-B-co</dataset_name>       <parameter id='10014' unit='mm'>         <value>0.2500</value>       </parameter>       <parameter id='10040' unit='mm/s'>         <value>15.000</value>       </parameter>       <parameter id='10044' unit='mm'>         <value>0.4500</value>       </parameter>       <parameter id='10015' unit='ms'>         <value>150</value>       </parameter>       <parameter id='10022' unit='mm/s'>         <value>3</value>       </parameter>       <parameter id='10030' unit='mm/s'>         <value>3</value>       </parameter>       <parameter id='10028' unit='mm/s'>         <value>3</value>       </parameter>       <parameter id='10029' unit='mm/s'>         <value>2</value>       </parameter>       <parameter id='10025' unit='mm/s'>         <value>3</value>       </parameter>       <parameter id='10024' unit='ms'>         <value>50</value>       </parameter>       <parameter id='10016' unit='gram'>         <value>200</value>       </parameter>       <parameter id='10031' unit='ms'>         <value>200</value>       </parameter>       <parameter id='10021' unit='ms'>         <value>200</value>       </parameter>       <parameter id='10043' unit='mm/s'>         <value>3</value>       </parameter>       <parameter id='10046' unit='mm/s'>         <value>2</value>       </parameter>     </dataset>   </group>   <group id='133'>     <dataset>       <dataset_name>XD36-BD-150150B-B-cc</dataset_name>       <parameter id='26544' unit='mm'>         <value>4.0000</value>       </parameter>     </dataset>   </group>   <group id='110'>     <dataset>       <dataset_name>General</dataset_name>       <parameter id='15022' unit='percent'>         <value>85</value>       </parameter>       <parameter id='15061' unit='percent'>         <value>85</value>       </parameter>     </dataset>   </group>   <group id='15'>     <dataset>       <dataset_name>XD36-BD-150150B-B-bp-pbi</dataset_name>       <parameter id='1503'>         <value>1</value>       </parameter>       <parameter id='1504'>         <value>1</value>       </parameter>       <parameter id='1506'>         <value id='coordinate1' unit='mm'>0.0150</value>         <value id='coordinate2' unit='mm'>0.0150</value>       </parameter>       <parameter id='1507' unit='deg'>         <value>0.3000</value>       </parameter>     </dataset>   </group>   <group id='31'>     <dataset>       <dataset_name>Tool allocations</dataset_name>       <parameter id='3119'>         <link_value target_group_name='Flip tools' target_group_id='34'>           <value>Fliptool_left</value>         </link_value>       </parameter>       <parameter id='3120'>         <link_value target_group_name='Flip tools' target_group_id='34'>           <value>Fliptool_right</value>         </link_value>       </parameter>     </dataset>   </group>   <group id='34'>     <dataset>       <dataset_name>Fliptool_left</dataset_name>       <parameter id='3408'>         <value>120000</value>       </parameter>     </dataset>     <dataset>       <dataset_name>Fliptool_right</dataset_name>       <parameter id='3408'>         <value>120000</value>       </parameter>     </dataset>   </group>   <group id='32'>     <dataset>       <dataset_name>4needle_30mm-et</dataset_name>       <parameter id='3210'>         <value>100000</value>       </parameter>     </dataset>   </group>   <group id='23'>     <dataset>       <dataset_name>XD36-BD-150150B-B-bp</dataset_name>       <parameter id='2306' unit='deg'>         <value>180.0000</value>       </parameter>     </dataset>   </group>   <group id='30'>     <dataset>       <dataset_name>2.5X1.7MM-L-ppt</dataset_name>       <parameter id='3008'>         <value>5000</value>       </parameter>     </dataset>     <dataset>       <dataset_name>2.5X1.7MM-R-ppt</dataset_name>       <parameter id='3008'>         <value>5000</value>       </parameter>     </dataset>   </group> </datacon_parameters> ");
        map.put("deviceCode", "FC-029");
        map.put("msgType", "s1f4");
        transferSV(map);
//        List<RecipePara> recipeParas = transferFromDB(map);
//        System.out.println(recipeParas.size());
//        for (int i = 0; i < recipeParas.size(); i++) {
//            System.out.println(recipeParas.get(i).getParaName().toString() + "======" + recipeParas.get(i).getSetValue().toString());
//        }
    }

    public static Map transferSV(Map map) {
        Map resultMap = new HashMap();
        String XMLData = (String) map.get("Value");
        String[] resultTemp = XMLData.split("</value>");
        String[] result = new String[resultTemp.length - 1];
        for (int i = 0; i < result.length; i++) {
            result[i] = resultTemp[i];
        }
        for (String valueTemp : result) {
            String id = "";
            String value = "";
            if (valueTemp.contains("<parameter id=") || valueTemp.contains("<value")) {
//                System.out.println(valueTemp.replaceAll("\\n", ""));
                String[] ids = valueTemp.replaceAll("parameter id", "parameterid").replaceAll("value id", "valueid").replaceAll("'coordinate1' ", "").replaceAll("'coordinate2' ", "").split("\\s");

                for (int i = 0; i < ids.length; i++) {
                    if (ids[i].contains("parameterid")) {
                        //<parameterid='10014'
                        id = ids[i].replaceAll("parameterid=", "").replaceAll("<", "").replaceAll("'", "").replaceAll(">", "");
//                        System.out.println("=====" + id);
                    }
                    if (ids[i].contains("<value")) {

                        value = ids[i].replaceAll(" ", "").split(">")[1];
//                        System.out.println("=====" + value);
                    }
                }
                resultMap.put(id, value);
            }

        }
        System.out.println("" + resultMap);
        return resultMap;
    }

}
