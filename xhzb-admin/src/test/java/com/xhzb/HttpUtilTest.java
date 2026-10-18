package com.xhzb;

import cn.hutool.core.util.CharsetUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * Hutool-HttpUtil工具测试
 *
 * @Author xujingbo
 * @Date 2026-8-17 15:49
 */
public class HttpUtilTest {


    /**
     * 测试GET请求
     */
    @Test
    public void testGet() {
        // 第一种GET请求方式 最简单的HTTP请求，可以自动通过header等信息判断编码，不区分HTTP和HTTPS
        String result1 = HttpUtil.get("http://localhost:8080/captchaImage");
        System.out.println(result1);

        // 第二种GET请求方式
        HttpResponse response = HttpUtil.createGet("http://localhost:8080/captchaImage")
                .execute();
        if (response.isOk()) {
            String result2 = response.body();
            System.out.println(result2);
        }
    }


    /**
     * 测试GET请求
     */
    @Test
    public void testGetAdvanced() {
        HttpResponse response = HttpUtil.createGet("http://localhost:8080/nursing/project/list")
                .form(Map.of("pageNum", 1, "pageSize", 3))
                .header("authorization", "Bearer eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJhZG1pbiIsImxvZ2luX3VzZXJfa2V5IjoiODgyM2JjNDItZDBmYi00ZGRhLWFmZjYtZDc0YTdlZjQ4Yjg0In0.f3LaRsjeMNlL8aH6EvsRk0Z_6HefQC32UD3brKKfL77mPwypqBbWoEA5t6xCIbGiY0bMdIELQ-4fjpuQZkc9pg")
                .execute();
        if (response.isOk()) {
            String result2 = response.body();
            System.out.println(result2);
        }
    }


    /**
     * 测试POST请求
     */
    @Test
    public void testPost() {
        HashMap<String, Object> paramMap = new HashMap<>();
        paramMap.put("name", "护理项目测试1111");
        paramMap.put("orderNo", 1);
        paramMap.put("unit", "次");
        paramMap.put("price", 10.00);
        paramMap.put("image", "https://yjy-slwl-oss.oss-cn-hangzhou.aliyuncs.com/ae7cf766-fb7b-49ff-a73c-c86c25f280e1.png");
        paramMap.put("nursingRequirement", "无特殊要求");
        paramMap.put("status", 1);
        HttpResponse response = HttpUtil.createPost("http://localhost:8080/nursing/project")
                .body(JSONUtil.toJsonStr(paramMap))
                .header("authorization", "Bearer eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJhZG1pbiIsImxvZ2luX3VzZXJfa2V5IjoiODgyM2JjNDItZDBmYi00ZGRhLWFmZjYtZDc0YTdlZjQ4Yjg0In0.f3LaRsjeMNlL8aH6EvsRk0Z_6HefQC32UD3brKKfL77mPwypqBbWoEA5t6xCIbGiY0bMdIELQ-4fjpuQZkc9pg")
                .execute();
        if (response.isOk()) {
            String result = response.body();
            System.out.println(result);
        }
    }


}
