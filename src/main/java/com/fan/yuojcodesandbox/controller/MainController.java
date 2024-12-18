package com.fan.yuojcodesandbox.controller;

import cn.hutool.http.HttpResponse;
import cn.hutool.http.server.HttpServerRequest;
import cn.hutool.http.server.HttpServerResponse;
import com.fan.yuojcodesandbox.JavaDockerCodeSandBox;
import com.fan.yuojcodesandbox.JavaNativeSandbox;
import com.fan.yuojcodesandbox.model.ExecuteCodeRequest;
import com.fan.yuojcodesandbox.model.ExecuteCodeResponse;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * @author fanshuaiyao
 * @description: TODO
 * @date 2024/11/25 20:25
 */
@RestController("/")
public class MainController {

    // 定义鉴权请求头和密钥
    // 1. 双方约定好一个字符串 进行校验
    // 2. 扩展  用API签名认证
    public static final String AUTH_REQUEST_HEADER = "auth";
    public static final String AUTH_REQUEST_SECRET  = "secretKey";

    @Resource
    private JavaNativeSandbox javaNativeSandbox;

    @Resource
    private JavaDockerCodeSandBox javaDockerCodeSandBox;

    @GetMapping("/health")
    public String health() {
        return "OK";
    }

    @PostMapping("/executeCode")
    ExecuteCodeResponse executeCode(@RequestBody ExecuteCodeRequest executeCodeRequest,
                                    HttpServerRequest request, HttpServerResponse response) {
        // String requestHeader = request.getHeader(AUTH_REQUEST_HEADER);
        // if (!AUTH_REQUEST_SECRET.equals(requestHeader)) {
        //     throw new RuntimeException("权限不够！");
        // }
        if (executeCodeRequest == null) {
            throw new RuntimeException("请求参数为空！");
        }
        System.out.println("请求参数为 = " + executeCodeRequest);
        // return javaNativeSandbox.executeCode(executeCodeRequest);
        // ExecuteCodeResponse executeCodeResponse = javaNativeSandbox.executeCode(executeCodeRequest);
        ExecuteCodeResponse executeCodeResponse = javaDockerCodeSandBox.executeCode(executeCodeRequest);
        System.out.println("代码沙箱的相应 = " + executeCodeResponse);
        return executeCodeResponse;
    }
    }
