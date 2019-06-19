/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package test;

import org.apache.log4j.Logger;

/**
 *
 * @author rain
 */
public class TestLog4j {

    private static Logger logger = Logger.getLogger(TestLog4j.class);

    public static void main(String[] args) {

        logger.debug("This is debug message3.");
        System.out.println("print...");
    }
}
