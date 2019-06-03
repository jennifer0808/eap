package cn.tzauto.octopus.secsLayer.modules.alarm;

import cn.tzauto.generalDriver.entity.msg.DataMsgMap;
import cn.tzauto.generalDriver.entity.msg.SecsItem;
import cn.tzauto.octopus.secsLayer.util.ACKDescription;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by luosy on 2019/4/3.
 */
public class AlarmDealer {
    private static Logger logger = Logger.getLogger(AlarmDealer.class);

    public static Map deal(DataMsgMap data) {
        String ALID = (String) data.get("ALID");
        String ALCD = (String) data.get("ALCD");
        String ALTX = (String) data.get("ALTX");
//        logger.info("Recived s5f1 ID:" + ALID + "  with the ALCD=" + ALCD + " means " + ACKDescription.description((byte) ALCD, "ALCD") + ", and the ALTX is: " + ALTX);
//       UiLogUtil.getInstance().appendLog2SecsTab(deviceCode, "收到报警信息 " + " 报警ID:" + ALID + " 报警详情: " + ALTX);
        Map resultMap = new HashMap();
        resultMap.put("ALID", ALID);
        resultMap.put("ALCD", ALCD);
        resultMap.put("ALTX", ALTX);
        resultMap.put("TransactionId", data.getTransactionId());
        return resultMap;
    }
}
