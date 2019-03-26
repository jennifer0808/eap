/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package test;

import cn.tzauto.octopus.biz.device.service.DeviceService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import org.apache.ibatis.session.SqlSession;

/**
 *
 * @author rain
 */
public class TestProperty {
    public static void main(String[] args){
        
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        DeviceService deviceService = new DeviceService(sqlSession);
        String deviceCode = "D3500-0004";
        String functionCode = "CESVCHKMSG";
        boolean result = deviceService.checkFunctionSwitch(deviceCode, functionCode);
        sqlSession.close();
        System.out.println("result: "+result);
        
    }
}
