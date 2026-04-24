package com.ly.springquickstart.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RoleRequired {
    /** 允许的角色值：0=买家 1=商家 2=管理员，可多值 */
    int[] value();
}
