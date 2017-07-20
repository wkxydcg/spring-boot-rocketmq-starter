package com.wkx;

import com.alibaba.rocketmq.client.exception.MQBrokerException;
import com.alibaba.rocketmq.client.exception.MQClientException;
import com.alibaba.rocketmq.client.producer.DefaultMQProducer;
import com.alibaba.rocketmq.common.message.Message;
import com.alibaba.rocketmq.remoting.exception.RemotingException;
import com.wkx.annotation.MQProducer;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Created by wkx on 2017/7/19.
 * @author wkx
 */
@Aspect
@Component
@ConditionalOnProperty(prefix = "rocketmq",value = {"ip","port"})
@ConditionalOnExpression("${rocketmq.producer-enable:true}")
public class ProducerConfig {

    private final DefaultMQProducer producer;

    @Autowired
    public ProducerConfig(DefaultMQProducer producer) {
        this.producer = producer;
    }

    @Around(value="@annotation(ds)")
    public Object producerMessage(ProceedingJoinPoint joinPoint, MQProducer ds) {
        Object retValue=null;
        try {
            retValue=joinPoint.proceed();
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
        if(retValue==null) return null;
        String topic=ds.topic();
        try {
            Message message=new Message();
            message.setTopic(topic);
            message.setBody(retValue.toString().getBytes());
            producer.send(message);
        } catch (MQClientException | RemotingException | InterruptedException | MQBrokerException e) {
            e.printStackTrace();
        }
        return null;
    }
}
