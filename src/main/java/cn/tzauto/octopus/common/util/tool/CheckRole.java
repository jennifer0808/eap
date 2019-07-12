/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.common.util.tool;

import cn.tzauto.octopus.common.globalConfig.GlobalConstants;

/**
 * 判断登陆者的权限是否正确
 *
 */
public class CheckRole {

    public static boolean ifLogin() { //不一定跳出登录框
        if (GlobalConstants.sysUser == null) {

            return false;
        } else {
            if (GlobalConstants.sysUser.getUserType().equalsIgnoreCase("1")) {
//                GlobalConstants.stage.getJB_LogQuery().setVisible(true);
//                GlobalConstants.stage.getJB_RcpMng().setVisible(true);
//                GlobalConstants.stage.getJB_SignOut().setVisible(true);
//                GlobalConstants.stage.getJB_MainPage().setVisible(true);
//                GlobalConstants.stage.getJB_AlarmQuery().setVisible(true);
//                GlobalConstants.stage.getJB_AlarmRepeat().setVisible(true);
//                GlobalConstants.stage.getJB_Login().setVisible(false);
                return true;
            } else {
                return false;
            }
        }
    }

    public static boolean ifLoginValid() { //需要跳出登录框
        if (GlobalConstants.isLocalMode) {
            return true;
        }
        return GlobalConstants.loginValid;
    }
}
