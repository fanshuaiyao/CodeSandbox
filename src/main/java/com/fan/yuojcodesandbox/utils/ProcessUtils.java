package com.fan.yuojcodesandbox.utils;

import cn.hutool.core.util.StrUtil;
import com.fan.yuojcodesandbox.model.ExecuteMessage;
import org.springframework.util.StopWatch;

import java.io.*;

/**
 * @author fanshuaiyao
 * @description: 进程工具类
 * @date 2024/11/25 22:20
 */
public class ProcessUtils {
    /**
     * 执行进程并获取信息  参数式
     */
    public static ExecuteMessage runProcessAndGetMessage(Process runProcess, String opName) {
        ExecuteMessage executeMessage = new ExecuteMessage();

        try {
            // 开始计时
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();

            int exitValue = runProcess.waitFor();
            executeMessage.setExitVlue(exitValue);
            // 正常退出
            if (exitValue == 0) {
                System.out.println(opName + "成功！");
                // 拿到控制台的输出  也是线程的正差输流
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(runProcess.getInputStream()));
                StringBuilder compileOutputStringBuilder = new StringBuilder();
                // 逐行读取
                String compileOutputLine;
                // if compileOutputLine not null 我们就持续输出，否则不输出
                while ((compileOutputLine = bufferedReader.readLine()) != null) {
                    compileOutputStringBuilder.append(compileOutputLine).append("\n");
                }
                executeMessage.setMessage(compileOutputStringBuilder.toString());
            } else {
                // 非正常退出吧
                System.out.println(opName + "失败！错误码：" + exitValue);
                // 拿到控制台的输出  也是线程的正差输流
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(runProcess.getInputStream()));
                StringBuilder compileOutputStringBuilder = new StringBuilder();
                // 逐行读取
                String compileOutputLine;
                // if compileOutputLine not null 我们就持续输出，否则不输出
                while ((compileOutputLine = bufferedReader.readLine()) != null) {
                    compileOutputStringBuilder.append(compileOutputLine).append("\n");
                }
                executeMessage.setErrorMessage(compileOutputStringBuilder.toString());


                // 拿到控制台的输出  拿到错误流
                BufferedReader errorBufferedReader = new BufferedReader(new InputStreamReader(runProcess.getErrorStream()));
                StringBuilder errorCompileOutputStringBuilder = new StringBuilder();
                // 逐行读取
                String errorCompileOutputLine;
                // if compileOutputLine not null 我们就持续输出，否则不输出
                while ((errorCompileOutputLine = errorBufferedReader.readLine()) != null) {
                    errorCompileOutputStringBuilder.append(errorCompileOutputLine).append("\n");
                }
                executeMessage.setErrorMessage(errorCompileOutputStringBuilder.toString());

                // 结束计时
                stopWatch.stop();
                // 获取时间
                executeMessage.setTime(stopWatch.getLastTaskTimeMillis());

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return executeMessage;
    }

    /**
     * 交互式
     */
    public static ExecuteMessage runInteractProcessAndGetMessage(Process runProcess, String opName, String args) {
        ExecuteMessage executeMessage = new ExecuteMessage();

        try {

            // 向控制台输入程序
            OutputStream outputStream = runProcess.getOutputStream();

            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream);
            String[] s = args.split(" ");
            String join = StrUtil.join("\n", s) + "\n";
            outputStreamWriter.write(join);
            // 相当于回车，执行输入的发送
            outputStreamWriter.flush();

            // 拿到控制台的输出  也是线程的正差输流
            InputStream inputStream = runProcess.getInputStream();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder compileOutputStringBuilder = new StringBuilder();
            // 逐行读取
            String compileOutputLine;
            // if compileOutputLine not null 我们就持续输出，否则不输出
            while ((compileOutputLine = bufferedReader.readLine()) != null) {
                compileOutputStringBuilder.append(compileOutputLine);
            }
            executeMessage.setMessage(compileOutputStringBuilder.toString());

            // 资源回收
            outputStreamWriter.close();
            outputStream.close();
            inputStream.close();
            runProcess.destroy();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return executeMessage;
    }
}
