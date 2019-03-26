/**
 *
 */
package cn.tzauto.octopus.secsLayer.exception;

/**
 * @author  dingxiaoguo
 * @Company 南京钛志信息系统有限公司
 * @Create Date 2016-8-7
 * @(#)PackagingBaseException.java
 *
 * @Copyright tzinfo, Ltd. 2016.
 * This software and documentation contain confidential and
 * proprietary information owned by tzinfo, Ltd.
 * Unauthorized use and distribution are prohibited.
 * Modification History:
 * Modification Date     Author        Reason
 * class Description
 */
public class PackagingBaseException extends Exception
{
	private int deviceId;

	public PackagingBaseException(String message) {
		// TODO Auto-generated constructor stub
		super(message);
	}

	public PackagingBaseException(String message, int deviceId) {
		// TODO Auto-generated constructor stub
		super(message);
		this.deviceId = deviceId;
	}

}
