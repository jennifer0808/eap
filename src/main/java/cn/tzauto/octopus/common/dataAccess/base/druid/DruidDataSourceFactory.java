/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.common.dataAccess.base.druid;

import com.alibaba.druid.pool.DruidDataSource;
import javax.sql.DataSource;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSourceFactory;

/**
 *
 * @author gavin
 */
public class DruidDataSourceFactory extends UnpooledDataSourceFactory {

    private DataSource dataSource;

    public DruidDataSourceFactory() {
        this.dataSource = new DruidDataSource();
    }
}
