/**
 *
 */
package cn.tzauto.octopus.secsLayer.domain;

/**
 * @author  dingxiaoguo
 * @Company 南京钛志信息系统有限公司
 * @Create Date 2016-7-14
 * @(#)Wafer.java
 *
 */
public class Wafer extends DomainObject
{
	public static float FOUR_INCH = 4;
	public static float SIX_INCH = 6;
	public static float EIGHT_INCH = 8;
	public static float TWELVE_INCH = 12;

	private float diameter; //4, 6, 8, 12, etc

	public float getDiameter() {
		return diameter;
	}

	public void setDiameter(float diameter) {
		this.diameter = diameter;
	}


}
