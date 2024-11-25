package com.fan.yuojcodesandbox.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author fanshuaiyao
 * @description: TODO
 * @date 2024/11/25 20:25
 */
@RestController("/")
public class MainController {

    @GetMapping("/health")
    public String health() {
        return "OK";
    }
}
