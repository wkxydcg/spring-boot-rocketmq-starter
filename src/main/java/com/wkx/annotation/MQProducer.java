package com.wkx.annotation;

import java.lang.annotation.*;

/**
 * Created by wkx on 2017/7/18.
 * @author wkx
 */
@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MQProducer {

    String topic();

}
