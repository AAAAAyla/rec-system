package com.ly.springquickstart.pojo;

import lombok.Data;

@Data
public class Result {
    private Integer code;  // 1成功，0失败
    private String msg;
    private Object data;

    public static Result success(Object data) {
        Result r = new Result();
        r.setCode(1);
        r.setMsg("success");
        r.setData(data);
        return r;
    }

    public static Result success() {
        return success(null);
    }

    public static Result error(String msg) {
        Result r = new Result();
        r.setCode(0);
        r.setMsg(msg);
        return r;
    }
}