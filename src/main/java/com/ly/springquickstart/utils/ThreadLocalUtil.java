package com.ly.springquickstart.utils;

import java.util.Map;

/**
 * ThreadLocal 工具类（相当于全局共享的个人储物柜管理器）
 * 作用：在同一个请求的整个生命周期内，随时随地存取用户数据
 */
public class ThreadLocalUtil {

    // 创建一个全局的 ThreadLocal 对象，里面专门用来装 Map<String, Object>（也就是解析出来的用户数据）
    private static final ThreadLocal<Map<String, Object>> THREAD_LOCAL = new ThreadLocal<>();

    /**
     * 往储物柜里放东西（保安查完票后调用）
     */
    public static void set(Map<String, Object> value) {
        THREAD_LOCAL.set(value);
    }

    /**
     * 从储物柜里取东西（Controller 或 Service 需要知道当前是谁时调用）
     */
    public static Map<String, Object> get() {
        return THREAD_LOCAL.get();
    }

    /**
     * 清空储物柜（极其重要！）
     * 为什么必须要清空？因为现在的服务器都是“线程池”运作，也就是保安下班后，他的位置会被下一个人接管。
     * 如果不清空，下一个人来可能会读到上一个人的数据，这就是严重的“内存泄漏”和“数据串位”安全事故。
     */
    public static void remove() {
        THREAD_LOCAL.remove();
    }
}