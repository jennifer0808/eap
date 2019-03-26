/**
 *
 */
package cn.tzauto.octopus.secsLayer.domain;

import java.util.Vector;

import org.apache.log4j.Logger;

/**
 * @author  dingxiaoguo
 * @Company 南京钛志信息系统有限公司
 * @Create Date 2016-7-14
 * @(#)Lot.java
 *
 */
public class Lot extends DomainObject
{
	private static final Logger logger = Logger.getLogger(Lot.class.getName());

	private Vector<Wafer> wafers; //all wafers of this lot
	private Vector<LotHistory> lotHistory;  //holding on to the history of lot locations

	/*
	job <MaterialJob>
	The job this lot belongs to.

	recipeContext <undefined>



	Instance Methods
	accessing
	job


	job:anAbstractJob


	recipeContext


	recipeContext:anObject


	accessing-cassettes
	carrierWithId:aString
	visit all wafers to find the cassette with given id; Return: Cassette (nil, if no such cassette exists)

	myCarrier
	Return the cassette my wafers are in. Its sufficient to just ask one wafer.

	accessing-wafers
	addWafer:aWafer
	register the given wafer in the list of wafers

	wafers


	waferWithId:anId


	initialize-release
	initialize


	initializeFromPptLotInCassette_struct:aPptLotInCassette_struct cassette:aCassette
	Don't forget to create slots, put the wafer inside, put the slot into the cassette and then we are done.

	Class Methods
	ADvance
	ad2ClassInfo


	instance creation
	newFromPptLotInCassette_struct:aPptLotInCassette_struct cassette:aCassette job:anAbstractJob
	Create a new Lot with given ID and initialize it from parameters. The ID is already tested to be unique.


	--------------------------------------------------------------------------------
	AMD.CEI36.Lot
	*/
}
