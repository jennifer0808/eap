package cn.tzauto.octopus.secsLayer.util;

import cn.tzinfo.smartSecsDriver.userapi.SecsItem;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author njtz
 */
public class CommonSMLUtil {

    public static ArrayList getECSVData(List<SecsItem> list) {
        ArrayList<Object> listtmp = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {//     
            short tmp = (short) list.get(i).getFormatCode();
            if (tmp == SecsItem.SECS_1BYTE_UNSIGNED_INTEGER
                    || tmp == SecsItem.SECS_2BYTE_UNSIGNED_INTEGER
                    || tmp == SecsItem.SECS_4BYTE_UNSIGNED_INTEGER
                    || tmp == SecsItem.SECS_8BYTE_UNSIGNED_INTEGER) {
                listtmp.add((long[]) list.get(i).getData());
                continue;
            }
            if (tmp == SecsItem.SECS_1BYTE_SIGNED_INTEGER
                    || tmp == SecsItem.SECS_2BYTE_SIGNED_INTEGER
                    || tmp == SecsItem.SECS_4BYTE_SIGNED_INTEGER
                    || tmp == SecsItem.SECS_8BYTE_SIGNED_INTEGER) {
                listtmp.add((int[]) list.get(i).getData());
                continue;
            }
            if (tmp == SecsItem.SECS_2BYTE_CHARACTER) {
                listtmp.add((Character) list.get(i).getData());
                continue;
            }
            if (tmp == SecsItem.SECS_4BYTE_FLOAT_POINT) {
                listtmp.add((float[]) list.get(i).getData());
                continue;
            }
            if (tmp == SecsItem.SECS_8BYTE_FLOAT_POINT) {
                listtmp.add((double[]) list.get(i).getData());
                continue;
            }
            if (tmp == SecsItem.SECS_ASCII) {
                listtmp.add((String) list.get(i).getData());
                continue;
            }
            if (tmp == SecsItem.SECS_BOOLEAN) {
                listtmp.add((boolean[]) list.get(i).getData());
                continue;
            }
            if (tmp == SecsItem.SECS_JIS8) {
                listtmp.add((String[]) list.get(i).getData());
                continue;
            }
            if (tmp == SecsItem.SECS_BINARY) {
                listtmp.add((byte[]) list.get(i).getData());
                continue;
            }
            if (tmp == SecsItem.SECS_LIST) {
                ArrayList<SecsItem> listc = (ArrayList) ((SecsItem) list.get(i)).getData();
                listtmp.add(getECSVData(listc));
            }
        }
        return listtmp;
    }
}
