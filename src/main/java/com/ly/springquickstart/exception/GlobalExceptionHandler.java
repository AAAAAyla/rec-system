package com.ly.springquickstart.exception;

import com.ly.springquickstart.pojo.Result;
import org.springframework.dao.DataAccessException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Result handleTypeMismatchException(MethodArgumentTypeMismatchException e) {
        System.out.println("参数类型错误: " + e.getMessage());
        return Result.error("参数格式错误，请检查请求地址或参数类型");
    }

    /**
     * 业务异常：直接将 message 返回给前端（如"库存不足"、"订单不存在"等）
     */
    @ExceptionHandler(RuntimeException.class)
    public Result handleRuntimeException(RuntimeException e) {
        System.out.println("[业务异常] " + e.getMessage());
        return Result.error(e.getMessage());
    }

    /**
     * 数据库访问异常：给出提示，告知需要执行初始化脚本
     */
    @ExceptionHandler(DataAccessException.class)
    public Result handleDataAccessException(DataAccessException e) {
        e.printStackTrace();
        String msg = e.getMostSpecificCause().getMessage();
        if (msg != null && msg.contains("doesn't exist")) {
            return Result.error("数据库表不存在，请先执行 db_migration.sql 初始化脚本");
        }
        return Result.error("数据库操作失败：" + msg);
    }

    /**
     * 兜底：未预料到的系统异常
     */
    @ExceptionHandler(Exception.class)
    public Result handleException(Exception e) {
        e.printStackTrace();
        return Result.error("系统繁忙，请稍后再试");
    }
}
