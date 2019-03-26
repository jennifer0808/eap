/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.common.mq;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.Topic;

/**
 *
 * @author luosy
 */
public class DynamicDestinationResolver implements DestinationResolver {

    @Override
    public Destination resolveDestinationName(Session session, String destinationName, boolean pubSubDomain)
            throws JMSException {   
        if (pubSubDomain) {
            return resolveTopic(session, destinationName);
        } else {
            return resolveQueue(session, destinationName);
        }
    }

    /**
     * Resolve the given destination name to a {@link Topic}.
     *
     * @param session the current JMS Session
     * @param topicName the name of the desired {@link Topic}
     * @return the JMS {@link Topic}
     * @throws javax.jms.JMSException if resolution failed
     * @see Session#createTopic(String)
     */
    protected Topic resolveTopic(Session session, String topicName) throws JMSException {
        return session.createTopic(topicName);
    }

    /**
     * Resolve the given destination name to a {@link Queue}.
     *
     * @param session the current JMS Session
     * @param queueName the name of the desired {@link Queue}
     * @return the JMS {@link Queue}
     * @throws javax.jms.JMSException if resolution failed
     * @see Session#createQueue(String)
     */
    protected Queue resolveQueue(Session session, String queueName) throws JMSException {
        return session.createQueue(queueName);
    }
}
