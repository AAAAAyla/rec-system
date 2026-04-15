package com.ly.springquickstart.exception;

import com.ly.springquickstart.pojo.Result;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * 全局异常处理器（相当于系统的“底层保安”）
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 专门拦截我们刚才遇到的那个“参数类型不匹配”异常
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Result handleTypeMismatchException(MethodArgumentTypeMismatchException e) {
        // 在控制台悄悄记录一下，方便我们排查
        System.out.println("拦截到参数类型错误: " + e.getMessage());

        // 给前端返回优雅的 JSON 提示
        return Result.error("参数格式错误，请检查请求地址或参数类型！");
    }

    /**
     * 终极兜底：拦截所有不可预知的 Exception（比如空指针、数据库连不上等）
     */
    @ExceptionHandler(Exception.class)
    public Result handleException(Exception e) {
        // 打印完整的错误堆栈到控制台，方便咱们修 Bug
        e.printStackTrace();

        // 给用户返回一个甩锅式的友好提示，绝不暴露代码细节
        return Result.error("系统繁忙，请稍后再试！");
    }
}