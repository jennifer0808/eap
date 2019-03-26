/**
 *
 */
package cn.tzauto.octopus.secsLayer.domain;

import java.util.concurrent.ConcurrentHashMap;

import cn.tzauto.octopus.secsLayer.exception.BehaviorStateException;

/**
 * @author  dingxiaoguo
 * @Company 南京钛志信息系统有限公司
 * @Create Date 2016-8-15
 * @(#)Cassette.java
 *
 */
public class Cassette extends MaterialContainer
{
	private ConcurrentHashMap<Integer, String> slots; //integer, wafer id pair
	private CarrierAccessingState state;

	public Cassette()
	{
		slots = new ConcurrentHashMap<Integer, String>(25);
		state = new CarrierAccessingState();
	}


	/*
	 * It can return null - means empty slot
	 */
	String getSlotWaferId(int slotNumber)
	{
		if(slotNumber > 24 || slotNumber < 0)
			throw new IllegalArgumentException("Argument slotNumber = " + slotNumber +
					". It must between 0 and 24");
		return slots.get(new Integer(slotNumber));
	}

	public void changeToAccessing() throws IllegalArgumentException, BehaviorStateException
	{
		state.transitState(CarrierAccessingState.IN_ACCESS);
	}

	public void changeToComplete() throws IllegalArgumentException, BehaviorStateException
	{
		state.transitState(CarrierAccessingState.CARRIER_COMPLETE);
	}

	public void changeToStopped() throws IllegalArgumentException, BehaviorStateException
	{
		state.transitState(CarrierAccessingState.CARRIER_STOPPED);
	}

	public void changeToNotAccessed() throws IllegalArgumentException, BehaviorStateException
	{
		state.transitState(CarrierAccessingState.NOT_ACCESSED);
	}

}
