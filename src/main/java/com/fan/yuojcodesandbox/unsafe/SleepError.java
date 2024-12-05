package com.fan.yuojcodesandbox.unsafe;

/**
 * @author fanshuaiyao
 * @description: 无限睡眠 阻塞程序进行
 * @date 2024/11/27 19:59
 */
public class SleepError {
    public static void main(String[] args) throws InterruptedException {
        long ONE_HOUR = 1000L * 60 * 60;
        Thread.sleep(ONE_HOUR);
        System.out.println("睡完了！");
    }
}
