package test;

import cn.tzauto.generalDriver.entity.msg.SecsFormatValue;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by leo
 */
public class maptest {

    @Test
    public void testMap(){
        Map map = new HashMap<>();
        map.put("", SecsFormatValue.SECS_ASCII);
        short value = (Short) map.get("");
        System.out.println(value);
    }
}
