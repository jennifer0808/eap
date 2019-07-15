/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package test;

import cn.tzauto.octopus.common.ws.AxisUtility;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class TestWebservice {
    
    public static void main(String[] args) {
                List<String> mailList = new ArrayList<>();
        mailList.add("weiqy@tzinfo.cn");
        mailList.add("qigao.wang@cj-elec.com");
        Map paraMap = new HashMap();
        paraMap.put("lotId", "aaaabbbbbb1345");
        String subject = "CIM系统通知：批次锁定";
        String sendMailFlag = AxisUtility.sendFtlMail(mailList, subject, "holdLot.ftl", paraMap);
        System.out.println("发邮件结果：" + sendMailFlag);
    }
}
