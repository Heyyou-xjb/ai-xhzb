package com.xhzb;

import cn.hutool.core.util.CharsetUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.xhzb.framework.web.service.TokenService;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.*;

/**
 * Hutool-HttpUtil工具测试
 *
 * @Author xujingbo
 * @Date 2026-8-17 15:49
 */
@SpringBootTest
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


    /**
     * 测试调用第三方接口查询天气
     */
    @Test
    public void testQueryWeather() {

        String url = "https://ali-weather.showapi.com/hour24";

        HttpResponse response = HttpUtil.createGet(url)
                .header("Authorization", "APPCODE 918f99e9fe8e49d7ace8c85e26d7d31b")
                .form("area", "郑州")
                .execute();

        if (response.isOk()) {
            String result = response.body();

            System.out.println(result);
        }
    }

    /**
     * 测试调用微信小程序登录接口
     */
    @Test
    public void testInvokeWechat() {
        String url = "https://api.weixin.qq.com/sns/jscode2session";

        HttpResponse response = HttpUtil.createGet(url)
                .form(Map.of(
                        "appid", "wx40596cc4bccec681",
                        "secret", "9cafd9183ef015c8d658e0fa2e56ABCD",
                        "js_code", "0d12I9000h5KVW1adc3009mKRh42ABCD",
                        "grant_type", "authorization_code"
                ))
                .execute();

        int status = response.getStatus();
        System.out.println(status);

        if (response.isOk()) {
            String result = response.body();

            JSONObject object = JSONUtil.parseObj(result);
            //获取用户唯一身份标识
            String openid = object.getStr("openid");
            System.out.println(openid);
        }
    }

    @Autowired
    private TokenService tokenService;


    /**
     * 测试生成令牌
     */
    @Test
    public void testCreateToken() {
        List<String> NICK_NAME_LIST = Arrays.asList("好事发生", "大吉大利", "今晚吃鸡", "福星高照", "兴高采烈", "妙笔生花");

        //打乱集合中元素顺序
        Collections.shuffle(NICK_NAME_LIST);
        String prefix = NICK_NAME_LIST.get(0); //随机昵称前缀
        String postfix = RandomStringUtils.randomNumeric(4); //随机4位数字
        String nickName = prefix + postfix; //随机昵称
        System.out.println(nickName);


        String token = tokenService.createToken(Map.of("userId", 1, "nickName", nickName));
        System.out.println(token);
    }
}
