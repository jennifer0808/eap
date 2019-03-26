/**
 *
 */
package cn.tzauto.octopus.secsLayer.domain;

import cn.tzauto.octopus.secsLayer.exception.BehaviorStateException;

/**
 * @author  dingxiaoguo
 * @Company 南京钛志信息系统有限公司
 * @Create Date 2016-8-15
 * @(#)CarrierAccessingState.java
 *
 */
public class CarrierAccessingState extends DomainObject
{
	//E87 states
	public static final int NOT_ACCESSED = 1;
	public static final int IN_ACCESS = 0;
	public static final int CARRIER_COMPLETE = 8;
	public static final int CARRIER_STOPPED = 32;

	private int currentState;

	public CarrierAccessingState()
	{
		currentState = NOT_ACCESSED; //default
	}

	public CarrierAccessingState(int initial_state)
	throws IllegalArgumentException
	{
		if(! isLegalState(initial_state))
			throw new IllegalArgumentException("Wrong Argument = " + initial_state + ".");
		currentState = initial_state;
	}

	public void transitState(int newState)
	throws IllegalArgumentException, BehaviorStateException
	{
		if( ! isLegalState(newState))
			throw new IllegalArgumentException("TransitState Argument = " + newState + ". It is a wrong value!");
		switch(newState)
		{
			case NOT_ACCESSED:
				break;
			case IN_ACCESS:
				if(this.currentState != NOT_ACCESSED )
					throw new BehaviorStateException("Wrong Carrier State transition: current state: " +
							this.mapStateToString(this.currentState) + " target state: IN_ACCESS.");
				break;
			case CARRIER_COMPLETE:
				if(this.currentState != IN_ACCESS )
					throw new BehaviorStateException("Wrong Carrier State transition: current state: " +
							this.mapStateToString(this.currentState) + " target state: CARRIER_COMPLETE.");
				break;
			case CARRIER_STOPPED:
				if(this.currentState != IN_ACCESS )
					throw new BehaviorStateException("Wrong Carrier State transition: current state: " +
							this.mapStateToString(this.currentState) + " target state: CARRIER_STOPPED.");
				break;
		}
		this.currentState = newState;
	}

	public boolean isLegalState(int state)
	{
		return state == NOT_ACCESSED ||
				state == IN_ACCESS ||
				state == CARRIER_COMPLETE ||
				state == CARRIER_STOPPED;
	}


	public int getState() {
		return currentState;
	}

	public String mapStateToString(int state)
	throws IllegalArgumentException
	{
		if( ! isLegalState(state))
			throw new IllegalArgumentException("mapStateToString Argument = " + state +
					". It is a wrong value!");
		switch(state)
		{
			case NOT_ACCESSED:
				return "NOT_ACCESSED";
			case IN_ACCESS:
				return "IN_ACCESS";
			case CARRIER_COMPLETE:
				return "CARRIER_COMPLETE";
			case CARRIER_STOPPED:
				return "CARRIER_STOPPED";
		}
		return "";
	}


}
