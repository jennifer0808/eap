/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.common.resolver.towa;

import cn.tzauto.octopus.common.globalConfig.GlobalConstants;

/**
 *
 */
public class TowaConfig {

    public static String PMC_CFG = GlobalConstants.getProperty("SML_PRE_PATH") + "cn/tfinfo/jcauto/octopus/biz/recipe/util/rule/pmcppbodycfg.csv";
    //调试用
//    public static String PMC_CFG = "D:/recipe/pmcppbodycfg.csv";
    public static String YPM1180_CFG = GlobalConstants.getProperty("SML_PRE_PATH") + "cn/tfinfo/jcauto/octopus/biz/recipe/util/rule/ypm1180ppbodycfg.csv";
    public static String Y1R_CFG = GlobalConstants.getProperty("SML_PRE_PATH") + "cn/tfinfo/jcauto/octopus/biz/recipe/util/rule/y1rppbodycfg.csv";
    public static String Y1E_CFG = GlobalConstants.getProperty("SML_PRE_PATH") + "cn/tfinfo/jcauto/octopus/biz/recipe/util/rule/y1eppbodycfg.csv";
}
