package com.wkx.annotation;

import com.alibaba.rocketmq.common.protocol.heartbeat.MessageModel;

import java.lang.annotation.*;


@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
public @interface MQConsumer {

	String topic();

	MessageModel messageModel() default MessageModel.CLUSTERING;

}
