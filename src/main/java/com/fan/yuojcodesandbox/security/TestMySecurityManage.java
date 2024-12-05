package com.fan.yuojcodesandbox.security;

import cn.hutool.core.collection.SpliteratorUtil;
import cn.hutool.core.io.FileUtil;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * @author fanshuaiyao
 * @description: 需要严格控制 太麻烦
 * @date 2024/11/27 23:35
 */
public class TestMySecurityManage {
    public static void main(String[] args) {
        // 先开启安全管理器
        System.setSecurityManager(new MySecurityManage());
        List<String> strings = FileUtil.readLines("D:\\2024IDEA\\project\\yuoj-code-sandbox\\src\\main\\java\\com\\fan\\yuojcodesandbox\\CodeSandBox.java", StandardCharsets.UTF_8);
        System.out.println("strings = " + strings);;


    }
}
