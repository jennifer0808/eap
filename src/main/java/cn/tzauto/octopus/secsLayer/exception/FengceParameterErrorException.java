package cn.tzauto.octopus.secsLayer.exception;

/**
 * Insert the type's description here.
 * Creation date: (11/5/01 1:32:53 PM)
 * @author: Stephen Zhou
 */
public class FengceParameterErrorException extends PackagingBaseException
{
	private static final long serialVersionUID = -733057588889423979L;

/**
 * DeviceNotRegisteredException constructor comment.
 * @param s java.lang.String
 */
public FengceParameterErrorException(String s) {
	super(s);
}
/**
 * DeviceNotRegisteredException constructor comment.
 * @param s java.lang.String
 * @param deviceId int
 */
public FengceParameterErrorException(String s, int deviceId) {
	super(s, deviceId);
}
}
