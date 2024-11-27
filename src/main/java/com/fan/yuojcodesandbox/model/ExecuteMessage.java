package com.fan.yuojcodesandbox.model;

import lombok.Data;

/**
 * @author fanshuaiyao
 * @description: 进程执行信息
 * @date 2024/11/25 22:21
 */
@Data
public class ExecuteMessage {

    private Integer exitVlue;

    private String message;

    private String errorMessage;

    private Long time;

}
