/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package test;

import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.biz.recipe.domain.RecipeTemplate;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.session.SqlSession;

/**
 *
 * @author 陈佳能
 */
public class TestProviderPrameter {

    public static void main(String[] args) throws IOException {
        Map map = new HashMap();
        map.put("Value", "<!DOCTYPE datacon_parameters SYSTEM 'datacon_parameters.dtd'>\n"
                + "<datacon_parameters>\n"
                + "  <group id='100'>\n"
                + "    <dataset>\n"
                + "      <dataset_name>SECS-co</dataset_name>\n"
                + "      <parameter id='10014' unit='mm'>\n"
                + "        <value>0.2000</value>\n"
                + "      </parameter>\n"
                + "      <parameter id='10040' unit='mm/s'>\n"
                + "        <value>15.000</value>\n"
                + "      </parameter>\n"
                + "      <parameter id='10044' unit='mm'>\n"
                + "        <value>0.5000</value>\n"
                + "      </parameter>\n"
                + "      <parameter id='10015' unit='ms'>\n"
                + "        <value>200</value>\n"
                + "      </parameter>\n"
                + "      <parameter id='10022' unit='mm/s'>\n"
                + "        <value>3</value>\n"
                + "      </parameter>\n"
                + "      <parameter id='10030' unit='mm/s'>\n"
                + "        <value>3</value>\n"
                + "      </parameter>\n"
                + "      <parameter id='10028' unit='mm/s'>\n"
                + "        <value>3</value>\n"
                + "      </parameter>\n"
                + "      <parameter id='10029' unit='mm/s'>\n"
                + "        <value>2</value>\n"
                + "      </parameter>\n"
                + "      <parameter id='10025' unit='mm/s'>\n"
                + "        <value>3</value>\n"
                + "      </parameter>\n"
                + "      <parameter id='10024' unit='ms'>\n"
                + "        <value>50</value>\n"
                + "      </parameter>\n"
                + "      <parameter id='10016' unit='gram'>\n"
                + "        <value>200</value>\n"
                + "      </parameter>\n"
                + "      <parameter id='10031' unit='ms'>\n"
                + "        <value>200</value>\n"
                + "      </parameter>\n"
                + "    </dataset>\n"
                + "  </group>\n"
                + "  <group id='133'>\n"
                + "    <dataset>\n"
                + "      <dataset_name>SECS-cc</dataset_name>\n"
                + "      <parameter id='26544' unit='mm'>\n"
                + "        <value>4.0000</value>\n"
                + "      </parameter>\n"
                + "    </dataset>\n"
                + "  </group>\n"
                + "  <group id='110'>\n"
                + "    <dataset>\n"
                + "      <dataset_name>General</dataset_name>\n"
                + "      <parameter id='15022' unit='percent'>\n"
                + "        <value>70</value>\n"
                + "      </parameter>\n"
                + "      <parameter id='15061' unit='percent'>\n"
                + "        <value>70</value>\n"
                + "      </parameter>\n"
                + "    </dataset>\n"
                + "  </group>\n"
                + "  <group id='15'>\n"
                + "    <dataset>\n"
                + "      <dataset_name>SECS-bp-pbi</dataset_name>\n"
                + "      <parameter id='1503'>\n"
                + "        <value>1</value>\n"
                + "      </parameter>\n"
                + "      <parameter id='1504'>\n"
                + "        <value>1</value>\n"
                + "      </parameter>\n"
                + "      <parameter id='1506'>\n"
                + "        <value id='coordinate1' unit='mm'>0.0200</value>\n"
                + "        <value id='coordinate2' unit='mm'>0.0200</value>\n"
                + "      </parameter>\n"
                + "      <parameter id='1507' unit='deg'>\n"
                + "        <value>0.3000</value>\n"
                + "      </parameter>\n"
                + "      <parameter id='1514' unit='mm' enabled='false'>\n"
                + "        <value>0.0000</value>\n"
                + "      </parameter>\n"
                + "    </dataset>\n"
                + "  </group>\n"
                + "  <group id='31'>\n"
                + "    <dataset>\n"
                + "      <dataset_name>Tool allocations</dataset_name>\n"
                + "      <parameter id='3119'>\n"
                + "        <link_value target_group_name='Flip tools' target_group_id='34'>\n"
                + "          <value>Fliptool_left</value>\n"
                + "        </link_value>\n"
                + "      </parameter>\n"
                + "      <parameter id='3120'>\n"
                + "        <link_value target_group_name='Flip tools' target_group_id='34'>\n"
                + "          <value>Fliptool_right</value>\n"
                + "        </link_value>\n"
                + "      </parameter>\n"
                + "    </dataset>\n"
                + "  </group>\n"
                + "  <group id='34'>\n"
                + "    <dataset>\n"
                + "      <dataset_name>Fliptool_left</dataset_name>\n"
                + "      <parameter id='3408'>\n"
                + "        <value>120000</value>\n"
                + "      </parameter>\n"
                + "    </dataset>\n"
                + "    <dataset>\n"
                + "      <dataset_name>Fliptool_right</dataset_name>\n"
                + "      <parameter id='3408'>\n"
                + "        <value>120000</value>\n"
                + "      </parameter>\n"
                + "    </dataset>\n"
                + "  </group>\n"
                + "  <group id='32'>\n"
                + "    <dataset>\n"
                + "      <dataset_name>4needle_30mm-et</dataset_name>\n"
                + "      <parameter id='3210'>\n"
                + "        <value>100000</value>\n"
                + "      </parameter>\n"
                + "    </dataset>\n"
                + "  </group>\n"
                + "  <group id='30'>\n"
                + "    <dataset>\n"
                + "      <dataset_name>PP-3MM-R-ppt</dataset_name>\n"
                + "      <parameter id='3008'>\n"
                + "        <value>7000</value>\n"
                + "      </parameter>\n"
                + "    </dataset>\n"
                + "    <dataset>\n"
                + "      <dataset_name>PP_3MM_L-ppt</dataset_name>\n"
                + "      <parameter id='3008'>\n"
                + "        <value>7000</value>\n"
                + "      </parameter>\n"
                + "    </dataset>\n"
                + "  </group>\n"
                + "  <group id='23'>\n"
                + "    <dataset>\n"
                + "      <dataset_name>SECS-bp</dataset_name>\n"
                + "      <parameter id='2306' unit='deg'>\n"
                + "        <value>90.0000</value>\n"
                + "      </parameter>\n"
                + "    </dataset>\n"
                + "  </group>\n"
                + "</datacon_parameters>");
        map.put("deviceCode", "FC-029");
        map.put("msgType", "s1f4");
        List<RecipePara> recipeParaList = null;
        String s = (String) map.get("Value");

        String[] ss = s.split("</value>");
        int c = ss.length;
        String[] sss = new String[c - 1];
        for (int i = 0; i < sss.length; i++) {
//            System.out.println(ss[i]);
            sss[i] = ss[i];
            System.out.println(sss[i]);
        }

        for (String a : sss) {
//            System.out.println(a);
            String[] ww = a.split(">");
//            String[] dd = a.split("parameter id='");
//            String bb = dd[1];
//            String[] gg = bb.split("'");
            String b = ww[ww.length - 1];
//            String ff = gg[0];
            System.out.println(b);
//            System.out.println(b);
        }

        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        RecipeService recipeService = new RecipeService(sqlSession);
        List<RecipeTemplate> recipeTemplates = recipeService.searchRecipeTemplateByDeviceTypeCode("8800SigmaPlus", "RecipePara");
        RecipePara recipePara = new RecipePara();
        for (RecipeTemplate recipeTemp : recipeTemplates) {
            recipePara.setParaCode(recipeTemp.getParaCode());
            recipePara.setParaMeasure(recipeTemp.getParaUnit());
            recipePara.setParaName(recipeTemp.getParaName());

        }
    }
}
