
package cn.tzauto.octopus.secsLayer.util;

/**
 * @author  dingxiaoguo
 * @Company 南京钛志信息系统有限公司
 * @Create Date 2016-8-6
 * @(#)Utility.java
 *
 * @Copyright tzinfo, Ltd. 2016.
 * This software and documentation contain confidential and
 * proprietary information owned by tzinfo, Ltd.
 * Unauthorized use and distribution are prohibited.
 * Modification History:
 * Modification Date     Author        Reason
 * class Description
 */

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.log4j.Logger;

public class UtilityFengCe
{
	private static final Logger logger = Logger.getLogger(UtilityFengCe.class.getName());

	private static Pattern VALID_IPV4_PATTERN = null;
	private static Pattern VALID_IPV6_PATTERN = null;
	private static final String ipv4Pattern = "(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])";
	private static final String ipv6Pattern = "([0-9a-f]{1,4}:){7}([0-9a-f]){1,4}";

	static {
	    try {
	    	VALID_IPV4_PATTERN = Pattern.compile(ipv4Pattern, Pattern.CASE_INSENSITIVE);
	    	VALID_IPV6_PATTERN = Pattern.compile(ipv6Pattern, Pattern.CASE_INSENSITIVE);
	    } catch (PatternSyntaxException e) {
	    	logger.fatal("Unable to compile pattern", e);
	    }
	}

	  /**
	   * Determine if the given string is a valid IPv4 or IPv6 address.  This method
	   * uses pattern matching to see if the given string could be a valid IP address.
	   *
	   * @param ipAddress A string that is to be examined to verify whether or not
	   *  it could be a valid IP address.
	   * @return <code>true</code> if the string is a value that is a valid IP address,
	   *  <code>false</code> otherwise.
	   */
	  public static boolean isIpAddress(String ipAddress)
	  {
		  Matcher m1 = UtilityFengCe.VALID_IPV4_PATTERN.matcher(ipAddress);
		  if (m1.matches()) {
			  return true;
		  }
		  Matcher m2 = UtilityFengCe.VALID_IPV6_PATTERN.matcher(ipAddress);
		  return m2.matches();
	  }


}
