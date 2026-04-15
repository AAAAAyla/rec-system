package com.ly.springquickstart.utils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;

import java.util.Date;
import java.util.Map;

public class JwtUtils {

    // 这是你们游乐园的“防伪印章”（密钥），绝对不能泄露给前端！
    private static final String KEY = "ly_spring_secret_key";

    /**
     * 接收用户信息，生成 token（发手环）
     */
    public static String genToken(Map<String, Object> claims) {
        return JWT.create()
                .withClaim("claims", claims) // 把用户ID、名字等信息存进手环里
                .withExpiresAt(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 12)) // 设置手环 12 小时后失效
                .sign(Algorithm.HMAC256(KEY)); // 盖上防伪印章
    }

    /**
     * 接收 token，验证真伪，并把里面的信息拿出来（验手环）
     */
    public static Map<String, Object> parseToken(String token) {
        return JWT.require(Algorithm.HMAC256(KEY))
                .build()
                .verify(token) // 如果手环是伪造的或者过期了，这里会直接报错（被我们的全局异常拦截！）
                .getClaim("claims")
                .asMap();
    }
}