/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.common.dataAccess.base.mybatisutil;

import java.io.IOException;
import java.io.Reader;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.log4j.Logger;

/**
 *
 * @author gavin
 */
public class MybatisSqlSession {

    private static final Logger logger = Logger.getLogger(MybatisSqlSession.class);
    public static SqlSession sqlSession = null;
    public static SqlSessionFactory sqlSessionFactory = null;

    static {

        String resource = "mybatis-config.xml";
        Reader reader = null;
        try {
            reader = Resources.getResourceAsReader(resource);
        } catch (IOException e) {
            logger.error("IOException:", e);
        }
        sqlSessionFactory = new SqlSessionFactoryBuilder().build(reader);

    }

    public static SqlSession getSqlSession() {
        sqlSession = sqlSessionFactory.openSession();
        return sqlSession;
    }

    public static SqlSession getBatchSqlSession() {
        sqlSession = sqlSessionFactory.openSession(ExecutorType.BATCH, false);
        return sqlSession;
    }
}
