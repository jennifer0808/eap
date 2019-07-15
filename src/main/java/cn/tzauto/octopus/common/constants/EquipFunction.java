/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.common.constants;

/**
 *
 * 定义设备需要检查或者锁机功能的关键字
 */
public class EquipFunction {

    public static final String CE_SV_CHECK = "CESVCHK";//当事件触发时，是否校验实时SV
    public static final String CE_SV_CHK_ALM = "CESVCHKALM";//当事件触发的checkSV时，是否报警
    public static final String CE_SV_CHK_MSG = "CESVCHKMSG";//当事件触发的checkSV时，是否发送消息到设备
    public static final String CE_SV_CHK_LOCK = "CESVCHKLOCK";//当事件触发的checkSV时，是否锁机
    public static final String PRESS_USE_CHK_LOCK = "PRESSUSECHKLOCK";//当Press使用错误时，是否锁机
    public static final String PRESS_USE_CHK_MSG = "PRESSUSECHKMSG";//当Press使用错误时，是否发送消息到设备
    public static final String RECIPE_NAME_START_CHECK = "RCPNAMESTARTCHK";//开机检查程序名称
    public static final String NET_BROKEN_HOLD_LOT = "NETBRKHOLDLOT";//断网调用MES Hold Lot
    public static final String OTHER_REASON_LOCK = "OTHERREASONLOCK";//其他原因锁机，如RecipeName或参数异常
}
