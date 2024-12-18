package com.fan.yuojcodesandbox.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author fanshuaiyao
 * @description: 题目判断信息
 * @date 2024/11/17 00:21
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class JudgeInfo {

    /**
     * 程序执行信息
     */
    private String message;

    /**
     * 消耗的内存
     */
    private Long memory;


    /**
     * 消耗的时间
     */
    private Long time;

}

