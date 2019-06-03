/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.isecsLayer.resolver;

import org.apache.log4j.Logger;

import java.io.Closeable;
import java.io.IOException;

/**
 *
 * @author Administrator
 */
public class IOUtil {

    private static final Logger logger = Logger.getLogger(IOUtil.class);

    /**
         * 关闭一个或多个流对象
         * 
         * @param closeables
         *            可关闭的流对象列表
         * @throws IOException
         */
    public static void close(Closeable... closeables) throws IOException {
        if (closeables != null) {
            for (Closeable closeable : closeables) {
                if (closeable != null) {
                    closeable.close();
                }
            }
        }
    }

    /**
         * 关闭一个或多个流对象
         * 
         * @param closeables
         *            可关闭的流对象列表
         */
    public static void closeQuietly(Closeable... closeables) {
        try {
            close(closeables);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
    }
}
