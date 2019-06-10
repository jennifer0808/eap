/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.common.util.tool;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.apache.log4j.Logger;

/**
 *
 * @author luosy
 */
public class RoundingOff {

    private static Logger logger = Logger.getLogger(RoundingOff.class);

    public static double roundOff(double srcNum, int length) {
        double f1 = 0;
        try {
            BigDecimal b = new BigDecimal(srcNum);
            f1 = b.setScale(length, RoundingMode.HALF_UP).doubleValue();
        } catch (Exception e) {
            logger.error(e);
        }
        return f1;
    }

    public static String roundOff1(String currentRecipeValue, String setValue) {
        //  String currentRecipeValue = "236482999000";
        //  String setValue = "236482999442323";
        String flag = currentRecipeValue.substring(currentRecipeValue.length() - 2, currentRecipeValue.length());
        if (".0".equals(flag)) {
            currentRecipeValue = currentRecipeValue.substring(0, currentRecipeValue.length() - 2);
        }
        int lastNotZero = 0;
        for (int i = currentRecipeValue.length() - 1; i < currentRecipeValue.length(); i--) {
            if (!String.valueOf(currentRecipeValue.charAt(i)).equals("0")) {
                lastNotZero = i;
                break;
            }
        }
        int srcLength = setValue.length();
        setValue = setValue.substring(0, lastNotZero + 2);
        int dealLength = setValue.length();
        setValue = String.valueOf(RoundingOff.roundOff(Double.valueOf(setValue) / 10, 0) * 10);
        if (setValue.contains("E")) {
            BigDecimal bd = new BigDecimal(setValue);
            setValue = bd.toPlainString();

        }
        flag = setValue.substring(setValue.length() - 2, setValue.length());
        if (".0".equals(flag)) {
            setValue = setValue.substring(0, setValue.length() - 2);
        }
        for (int i = dealLength; i < srcLength; i++) {
            setValue = setValue + "0";
        }
        return setValue;
    }

    public static void main(String[] args) {
        
        System.out.println("RoundingOff.main()" + roundOff1("3566040", "3566047"));
    }
}
