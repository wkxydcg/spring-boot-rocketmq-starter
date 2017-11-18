package com.wkx;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * Created by wkx on 2017/3/13.
 * @author wkx
 */
@Component
public class MQBeanFactory implements ApplicationContextAware {
    private static ApplicationContext applicationContext;

    public MQBeanFactory() {
    }

    public void setApplicationContext(ApplicationContext ac) throws BeansException {
        applicationContext = ac;
    }

    public static <T> T getBean(Class<T> beanClass) {
        return applicationContext.getBean(beanClass);
    }

    public static Object getBean(String name) {
        return applicationContext.getBean(name);
    }

    public static void registerBean(String name, Object obj) {
        DefaultListableBeanFactory beanFactory = (DefaultListableBeanFactory) applicationContext.getAutowireCapableBeanFactory();
        beanFactory.registerSingleton(name,obj);
    }
}
