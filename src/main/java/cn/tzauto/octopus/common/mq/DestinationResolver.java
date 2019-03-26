/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.common.mq;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Session;

/**
 *
 * @author luosy
 */
public interface DestinationResolver {

    Destination resolveDestinationName(Session session, String destinationName, boolean pubSubDomain)
            throws JMSException;
}
