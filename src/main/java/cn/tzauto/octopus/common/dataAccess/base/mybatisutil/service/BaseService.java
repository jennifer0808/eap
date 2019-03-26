/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.common.dataAccess.base.mybatisutil.service;

import org.apache.ibatis.session.SqlSession;

/**
 *
 * @author gavin
 */
public class BaseService {
    protected  SqlSession session;
    
    public BaseService(SqlSession sqlSession){
        this.session=sqlSession;
    }
}
