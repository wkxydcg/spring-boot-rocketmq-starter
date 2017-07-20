package com.wkx;

import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Created by wkx on 2017/7/19.
 * @author wkx
 */
@Data
@Accessors(chain = true)
@ConfigurationProperties(prefix = "rocketmq")
@ConditionalOnProperty(prefix = "rocketmq",value = {"ip","port"})
public class RocketMQProperties {

    private String ip;

    private String port;

    private boolean producerEnable=true;

    private boolean consumerEnable=true;

    private String ConsumerPackage="";

}
