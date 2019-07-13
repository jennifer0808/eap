/**
 *
 */
package cn.tzauto.octopus.common.log;

import cn.tzauto.octopus.common.globalConfig.GlobalConstants;
import cn.tzauto.octopus.secsLayer.util.GlobalConstant;
import java.io.IOException;

import org.apache.log4j.Layout;
import org.apache.log4j.RollingFileAppender;
import org.apache.log4j.MDC;
import org.apache.log4j.spi.ErrorCode;
import org.apache.log4j.spi.LoggingEvent;
import java.io.File;

/**
 * @author dingxiaoguo
 * @Company 南京钛志信息系统有限公司
 * @Create Date 2016-8-8
 *
 */
public class MultiEquipRollingFileAppender extends RollingFileAppender {

    private static final String ORIG_LOG_FILE_NAME = "OrginalLogFileName";
    private static String LOG_FILE_PATH = "";

    static {
//		String temp = System.getenv("Host_Server_Log_File_Path");
        String temp = GlobalConstants.getProperty("Host_Server_Log_File_Path");
        if (temp.endsWith("/") || temp.endsWith("\\")) {
            LOG_FILE_PATH = temp;
        } else {
            LOG_FILE_PATH = temp + File.separator;
        }
    }

    public MultiEquipRollingFileAppender() {

    }

    /**
     * @param layout
     * @param filename
     * @throws IOException
     */
    public MultiEquipRollingFileAppender(Layout layout, String filename)
            throws IOException {
        super(layout, filename);

    }

    /**
     * @param layout
     * @param filename
     * @param append
     * @throws IOException
     */
    public MultiEquipRollingFileAppender(Layout layout, String filename,
            boolean append) throws IOException {
        super(layout, filename, append);

    }

    @Override
    public void activateOptions() {
        if (fileName != null) {
            MDC.put(ORIG_LOG_FILE_NAME, fileName);
        }
        //Begin Added on 2016/09/10
        if (fileName != null) {
            if (!fileName.startsWith(LOG_FILE_PATH)) {
                String fileNamePrefix = (String) MDC.get(GlobalConstant.WHICH_EQUIPHOST_CONTEXT);
                if (fileNamePrefix == null) {
                    fileName = LOG_FILE_PATH + File.separator + fileName;
                } else {
                    fileName = LOG_FILE_PATH + File.separator + fileNamePrefix + fileName;
                }
            }
        }
        //end of Added on 2016/09/10
        super.activateOptions();
    }

    @Override
    public void append(LoggingEvent event) {
        try {
            String propertyFileName = (String) MDC.get(ORIG_LOG_FILE_NAME);
            String fileName = findProperFileName(propertyFileName);
            if (fileName == null) {
                return;
            }
            setFile(fileName, true, bufferedIO, bufferSize);
        } catch (IOException ie) {
            errorHandler.error(
                    "Error occured while setting file for the log file " + event.getLevel()
                    + " " + event.toString(), ie, ErrorCode.FILE_OPEN_FAILURE);
        }
        super.append(event);
    }

    private String findProperFileName(String propertyLogFileName) {
        String fileNamePrefix = (String) MDC.get(GlobalConstant.WHICH_EQUIPHOST_CONTEXT);
        if (fileNamePrefix == null) {
            return LOG_FILE_PATH + File.separator + propertyLogFileName;
        }
        String newFileName = LOG_FILE_PATH + fileNamePrefix
                + File.separator + fileNamePrefix + propertyLogFileName;
        /*
        if (propertyLogFileName != null)
        {
        final File logFile = new File(propertyLogFileName);
        final String fn = logFile.getName();
        final int dotIndex = fn.indexOf(GlobalConstant.DOT);
        if (dotIndex != -1) {
        // the file name has an extension. so, insert the level
        // between the file name and the extension
        newFileName = newFileName + fn.substring(0, dotIndex) + HIPHEN + level + DOT
        + fn.substring(dotIndex + 1);
        } else {
        // the file name has no extension. So, just append the level
        // at the end.
        newFileName = fn + HIPHEN + level;
        }
        return logFile.getParent() + File.separator + newFileName;
        }
         */
        return newFileName;
    }
}
