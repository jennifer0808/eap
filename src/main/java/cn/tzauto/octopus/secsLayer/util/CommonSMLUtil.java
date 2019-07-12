package cn.tzauto.octopus.secsLayer.util;


import cn.tzauto.generalDriver.entity.msg.MsgSection;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author njtz
 */
public class CommonSMLUtil {

    public static ArrayList getECSVData(List<MsgSection> list) {
        ArrayList listtmp = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {//     
            short tmp = (short) list.get(i).getFormatCode();
            if (tmp == MsgSection.SECS_1BYTE_UNSIGNED_INTEGER
                    || tmp == MsgSection.SECS_2BYTE_UNSIGNED_INTEGER
                    || tmp == MsgSection.SECS_4BYTE_UNSIGNED_INTEGER
                    || tmp == MsgSection.SECS_8BYTE_UNSIGNED_INTEGER) {
                listtmp.add((long[]) list.get(i).getData());
                continue;
            }
            if (tmp == MsgSection.SECS_1BYTE_SIGNED_INTEGER
                    || tmp == MsgSection.SECS_2BYTE_SIGNED_INTEGER
                    || tmp == MsgSection.SECS_4BYTE_SIGNED_INTEGER
                    || tmp == MsgSection.SECS_8BYTE_SIGNED_INTEGER) {
                listtmp.add((int[]) list.get(i).getData());
                continue;
            }
            if (tmp == MsgSection.SECS_2BYTE_CHARACTER) {
                listtmp.add((Character) list.get(i).getData());
                continue;
            }
            if (tmp == MsgSection.SECS_4BYTE_FLOAT_POINT) {
                listtmp.add((float[]) list.get(i).getData());
                continue;
            }
            if (tmp == MsgSection.SECS_8BYTE_FLOAT_POINT) {
                listtmp.add((double[]) list.get(i).getData());
                continue;
            }
            if (tmp == MsgSection.SECS_ASCII) {
                listtmp.add((String) list.get(i).getData());
                continue;
            }
            if (tmp == MsgSection.SECS_BOOLEAN) {
                listtmp.add((boolean[]) list.get(i).getData());
                continue;
            }
            if (tmp == MsgSection.SECS_JIS8) {
                listtmp.add((String[]) list.get(i).getData());
                continue;
            }
            if (tmp == MsgSection.SECS_BINARY) {
                listtmp.add((byte[]) list.get(i).getData());
                continue;
            }
            if (tmp == MsgSection.SECS_LIST) {
                ArrayList<MsgSection> listc = (ArrayList) ((MsgSection) list.get(i)).getData();
                listtmp.add(getECSVData(listc));
            }
        }
        return listtmp;
    }
}
