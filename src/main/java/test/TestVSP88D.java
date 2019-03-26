/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package test;

import cn.tzauto.octopus.common.ws.AxisUtility;

/**
 *
 * @author njtz
 */
public class TestVSP88D {
    public static void main(String[] args) {
        String result = AxisUtility.plasma88DService("PLA-026", "W8260470102-399", "MesAolotLoadCheck");
        System.out.println(result);
    }
}
