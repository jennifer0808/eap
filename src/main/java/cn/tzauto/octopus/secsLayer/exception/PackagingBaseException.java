/**
 *
 */
package cn.tzauto.octopus.secsLayer.exception;

/**
 * @author  dingxiaoguo
 * @Company 南京钛志信息系统有限公司

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
