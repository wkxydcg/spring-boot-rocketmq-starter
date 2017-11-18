package com.wkx;

import com.alibaba.rocketmq.client.consumer.DefaultMQPushConsumer;
import com.alibaba.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import com.alibaba.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import com.alibaba.rocketmq.client.exception.MQClientException;
import com.alibaba.rocketmq.common.protocol.heartbeat.MessageModel;
import com.wkx.annotation.MQConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

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
        List<Class> classList = scanClass(properties.getConsumerPackage());
        LOGGER.info("成功扫描类文件:{}个",classList.size());
        handleConsumer(classList);
    }

    private void handleConsumer(List<Class> classes) {
        for (Class c : classes) {
            if (!c.isAnnotation() || !c.isInterface()) {
                Method methods[] = c.getMethods();
                for (Method m : methods) {
                    generateConsumer(c, m);
                }
            }
        }
    }

    private void generateConsumer(Class aclass, Method method) {
        MQConsumer annotation = method.getAnnotation(MQConsumer.class);
        if (annotation != null) {
            try {
                final Object obj = aclass.newInstance();
                if (obj == null) return;
                String topic = annotation.topic();
                MessageModel messageModel = annotation.messageModel();
                DefaultMQPushConsumer consumer = new DefaultMQPushConsumer("ConsumerGroupName");
                String address = properties.getIp() + ":" + properties.getPort();
                consumer.setNamesrvAddr(address);
                consumer.setInstanceName("consumer");
                consumer.subscribe(topic, "*");
                consumer.setMessageModel(messageModel);
                consumer.registerMessageListener((MessageListenerConcurrently) (list, consumeConcurrentlyContext) -> {
                    list.forEach(message->{
                        try {
                            method.invoke(obj, message);
                        } catch (IllegalAccessException | InvocationTargetException e) {
                            LOGGER.error(e.getMessage(),e);
                        }
                    });
                    return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
                });
                consumer.start();
            } catch (InstantiationException | IllegalAccessException | MQClientException e) {
                e.printStackTrace();
            }
        }
    }

    private static List<Class> scanClass(String packageName) {
        String basePackage = packageName.replace(".", "/");
        List<Class> classList = new ArrayList<>();
        try {
            Enumeration<URL> dirs = Thread.currentThread().getContextClassLoader().getResources(basePackage);
            while (dirs.hasMoreElements()) {
                URL url = dirs.nextElement();
                String protocol = url.getProtocol();
                if ("file".equals(protocol)) {
                    String filePath = URLDecoder.decode(url.getFile(), "UTF-8");
                    List<Class> classes = scanFile(packageName, filePath);
                    if (classes != null) classList.addAll(classes);
                } else if ("jar".equals(protocol)) {
                    JarFile jar = ((JarURLConnection) url.openConnection()).getJarFile();
                    classList.addAll(scanJar(packageName, jar));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return classList;
    }

    private static List<Class> scanFile(String packageName, String packagePath) {
        List<Class> classList = new ArrayList<>();
        File dir = new File(packagePath);
        if (!dir.exists() || !dir.isDirectory()) {
            return null;
        }
        File[] files = dir.listFiles(file -> file.getName().endsWith(".class") || file.isDirectory());
        if (files == null) return null;
        for (File file : files) {
            if (file.isDirectory()) {
                List<Class> classes = scanFile(getPackageName(packageName, file.getName()), file.getAbsolutePath());
                if (classes != null) classList.addAll(classes);
            } else {
                String className = file.getName().substring(0, file.getName().length() - 6);
                try {
                    classList.add(Class.forName(getClassPath(packageName, className)));
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
        return classList;
    }

    private static List<Class> scanJar(String pack, JarFile jar) {
        String packageDir=pack.replace(".","/");
        List<Class> classList = new ArrayList<>();
        Enumeration<JarEntry> entries = jar.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            String name = entry.getName();
            if (name.charAt(0) == '/') {
                name = name.substring(1);
            }
            if(name.contains("META-INF")) break;
            if(!entry.isDirectory()&&name.endsWith(".class")){
                int beginIndex=name.indexOf(packageDir)+packageDir.length();
                String className=name.substring(beginIndex,name.length()-6);
                className=className.replace("/",".");
                try {
                    classList.add(Class.forName(className));
                } catch (Exception |Error ignored) {
                }
            }
        }
        return classList;
    }

    private static String getClassPath(String packageName, String className) {
        if (packageName == null || "".equals(packageName)) return className;
        return packageName.replace("/", ".") + "." + className;
    }

    private static String getPackageName(String packageName, String fileName) {
        if (packageName == null || "".equals(packageName)) return fileName;
        return packageName + "." + fileName;
    }
}
