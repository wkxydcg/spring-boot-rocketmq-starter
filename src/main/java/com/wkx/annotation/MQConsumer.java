package com.wkx.annotation;

import com.alibaba.rocketmq.common.protocol.heartbeat.MessageModel;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;


@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MQConsumer {

	String topic();

	MessageModel messageModel() default MessageModel.CLUSTERING;

}
