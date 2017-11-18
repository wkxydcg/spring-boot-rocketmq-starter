package com.wkx;

import com.alibaba.rocketmq.client.consumer.DefaultMQPushConsumer;
import com.alibaba.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import com.alibaba.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import com.alibaba.rocketmq.client.exception.MQClientException;
import com.alibaba.rocketmq.common.message.MessageExt;
import com.alibaba.rocketmq.common.protocol.heartbeat.MessageModel;
import com.wkx.annotation.MQConsumer;
import com.wkx.annotation.MQTag;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Created by wkx on 2017/7/19.
 *
 * @author wkx
 */
@Component
@ConditionalOnProperty(prefix = "rocketmq", value = {"ip", "port"})
@ConditionalOnExpression("${rocketmq.consumer-enable:true}")
public class ConsumerConfig implements CommandLineRunner {

    private static final Logger LOGGER= LoggerFactory.getLogger(ConsumerConfig.class);

    private final RocketMQProperties properties;

    @Autowired
    public ConsumerConfig(RocketMQProperties properties) {
        this.properties = properties;
    }

    @Override
    public void run(String... strings) throws Exception {
        Reflections reflections=new Reflections(properties.getConsumerPackage());
        Set<Class<?>> classList = reflections.getTypesAnnotatedWith(MQConsumer.class);
        LOGGER.info("成功扫描类文件:{}个",classList.size());
        handleConsumer(classList);
    }

    private void handleConsumer(Set<Class<?>> classes) throws MQClientException, IllegalAccessException, InstantiationException {
        String address = properties.getIp() + ":" + properties.getPort();
        Map<String,Set<Class>> topicMap=new HashMap<>();
        Map<String,MessageModel> topicModelMap=new HashMap<>();
        for (Class c:classes){
            MQConsumer mqConsumer=((MQConsumer)c.getAnnotation(MQConsumer.class));
            String topicId=mqConsumer.topic();
            Set<Class> classSet = topicMap.computeIfAbsent(topicId, k -> new HashSet<>());
            classSet.add(c);
            assert topicModelMap.get(topicId) == null || topicModelMap.get(topicId)==mqConsumer.messageModel();
            if(topicModelMap.get(topicId)==null) topicModelMap.put(topicId,mqConsumer.messageModel());
        }
        Map<String,Set<Method>> methodMap= MQStoreMap.getMethodMap();
        Map<Method,Object> methodObjectMap=MQStoreMap.getMethodObjectMap();
        for (Map.Entry<String,Set<Class>> entry:topicMap.entrySet()){
            Set<Class> classSet=entry.getValue();
            Set<Method> methodSet=new HashSet<>();
            for (Class c:classSet){
                Object obj=c.newInstance();
                List<Method> methodList=Arrays.asList(c.getMethods());
                methodList.forEach(method -> methodObjectMap.put(method,obj));
                methodSet.addAll(methodList);
            }
            methodMap.put(entry.getKey(),methodSet);
        }
        for (Map.Entry<String,Set<Method>> entry:methodMap.entrySet()){
            DefaultMQPushConsumer consumer=createConsumer(entry.getKey(),topicModelMap.get(entry.getKey()),address);
            MQBeanFactory.registerBean(entry.getKey(),consumer);
        }
    }

    private static DefaultMQPushConsumer createConsumer(String topic,MessageModel messageModel,String address) throws MQClientException {
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer("ConsumerGroupName");
        consumer.setNamesrvAddr(address);
        consumer.setInstanceName("consumer");
        consumer.subscribe(topic, "*");
        consumer.setMessageModel(messageModel);
        consumer.registerMessageListener((MessageListenerConcurrently) (list, consumeConcurrentlyContext) -> {
            list.forEach(message->{
                Set<Method> methodSet= MQStoreMap.getMethodMap().get(message.getTopic());
                if(methodSet.size()==1){
                    Method method=new ArrayList<>(methodSet).get(0);
                    if(method.getAnnotation(MQTag.class)==null){
                        methodInvoke(method,message);
                    }else{
                        handleTag(method,message);
                    }
                }else{
                    for (Method method:methodSet){
                        handleTag(method,message);
                    }
                }
            });
            return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
        });
        consumer.start();
        return consumer;
    }

    private static void handleTag(Method method, MessageExt message){
        MQTag mqTag=method.getAnnotation(MQTag.class);
        if(mqTag.tag().equals(message.getTags())){
            methodInvoke(method,message);
        }
    }

    private static void methodInvoke(Method method,MessageExt message){
        Object obj=MQStoreMap.getMethodObjectMap().get(method);
        try {
            method.invoke(obj,message);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }
}
