package com.wkx;

import com.alibaba.rocketmq.client.exception.MQClientException;
import com.alibaba.rocketmq.client.producer.DefaultMQProducer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Created by wkx on 2017/7/19.
 * @author wkx
 */
@Configuration
@ConditionalOnExpression("${rocketmq.producer-enable:true}")
@EnableConfigurationProperties(value = {RocketMQProperties.class})
@ConditionalOnProperty(prefix = "rocketmq",value = {"ip","port"})
public class RocketMQAutoConfiguration {

    private final RocketMQProperties properties;

    @Autowired
    public RocketMQAutoConfiguration(RocketMQProperties properties) {
        this.properties = properties;
    }

    @Bean
    public DefaultMQProducer producer(){
        DefaultMQProducer producer=new DefaultMQProducer("ProducerGroupName");
        String address=properties.getIp()+":"+properties.getPort();
        producer.setNamesrvAddr(address);
        producer.setInstanceName("producer");
        try {
            producer.start();
        } catch (MQClientException e) {
            e.printStackTrace();
        }
        return producer;
    }

}
