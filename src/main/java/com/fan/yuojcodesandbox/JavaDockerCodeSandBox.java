package com.fan.yuojcodesandbox;

import cn.hutool.core.date.StopWatch;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import com.fan.yuojcodesandbox.model.ExecuteCodeRequest;
import com.fan.yuojcodesandbox.model.ExecuteCodeResponse;
import com.fan.yuojcodesandbox.model.ExecuteMessage;
import com.fan.yuojcodesandbox.model.JudgeInfo;
import com.fan.yuojcodesandbox.utils.ProcessUtils;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.command.ExecStartResultCallback;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * @author fanshuaiyao
 * @description: 用程序操作命令行编译执行代码
 * 4. 收集结果
 * 5. 文件清理
 * 6. 错误处理
 * @date 2024/11/25 21:09
 */
public class JavaDockerCodeSandBox implements CodeSandBox{
    public static final String GLOBAL_CODE_DIR_NAME = "tempCode";
    public static final String GLOBL_JAVA_CLASS_NAME = "Main.java";
    public static final boolean FIRST_INIT = true;

    // 程序运行超时时间
    public static final long TIME_OUT = 5000L;


    public static void main(String[] args) {
        JavaDockerCodeSandBox javaDockerCodeSandBox = new JavaDockerCodeSandBox();
        ExecuteCodeRequest executeCodeRequest = new ExecuteCodeRequest();
        executeCodeRequest.setInputList(Arrays.asList("1 2", "1 3"));

        // 从文件中读代码
        // String code = ResourceUtil.readStr("testCode/simpleComputeArgs/Main.java", StandardCharsets.UTF_8);
        // 读异常程序
        String code = ResourceUtil.readStr("testCode/simpleComputeArgs/Main.java", StandardCharsets.UTF_8);
        executeCodeRequest.setCode(code);
        executeCodeRequest.setLanguage("java");
        ExecuteCodeResponse executeCodeResponse = javaDockerCodeSandBox.executeCode(executeCodeRequest);
        System.out.println("executeCodeResponse = " + executeCodeResponse);
    }

    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {

        // 设置一个默认的安全管理器
        // System.setSecurityManager(new DefaultSecurityManage());

        List<String> inputList = executeCodeRequest.getInputList();
        String code = executeCodeRequest.getCode();
        String language = executeCodeRequest.getLanguage();


        // 1. 把用户的代码保存为文件
        String userDir = System.getProperty("user.dir");
        // File.separator 为了兼容不同系统的斜杠和反斜杠
        String globalCodePathName = userDir + File.separator + GLOBAL_CODE_DIR_NAME;
        // 判断全局代码目录在不在,没有就新建
        if (!FileUtil.exist(globalCodePathName)){
            FileUtil.mkdir(globalCodePathName);
        }
        // 把用户的代码隔离存放
        String userCodeParentPath = globalCodePathName + File.separator + UUID.randomUUID();
        String userCodePath = userCodeParentPath + File.separator + GLOBL_JAVA_CLASS_NAME;
        File userCodeFile = FileUtil.writeString(code, userCodePath, "UTF-8");

        // 2. 通过命令行编译代码 得到class文件
        String compileCMD = String.format("javac -encoding utf-8 %s", userCodeFile.getAbsolutePath());
        try {
            // 执行程序
            Process compileProcess = Runtime.getRuntime().exec(compileCMD);
            ExecuteMessage executeMessage = ProcessUtils.runProcessAndGetMessage(compileProcess, "编译");

            System.out.println("executeMessage = " + userCodeParentPath);
        } catch (Exception e) {
            return getErrorResponse(e);
        }

        // 3. 创建容器
        // 3.1 创建docker服务
        DockerClient dockerClient = DockerClientBuilder.getInstance().build();

        // 3.2 拉取镜像
        String image = "openjdk:8-alpine";
//         if (FIRST_INIT){
//             PullImageCmd pullImageCmd = dockerClient.pullImageCmd(image);
//             PullImageResultCallback pullImageResultCallback = new PullImageResultCallback() {
//                 @Override
//                 public void onNext(PullResponseItem item) {
//                     System.out.println("下载镜像：" + item.getStatus());
//                     super.onNext(item);
//                 }
//             };
//             try {
//                 pullImageCmd.exec(pullImageResultCallback).awaitCompletion();
//             } catch (InterruptedException e) {
//                 System.out.println("拉取镜像异常！");
//                 throw new RuntimeException(e);
//             }
//             // FIRST_INIT = false;
//         }
//         System.out.println("下载完成！");

//         3.3 创建容器
        CreateContainerCmd containerCmd = dockerClient.createContainerCmd(image);
        HostConfig hostConfig = new HostConfig();
        // 文件地址映射绑定  容器挂载目录
        hostConfig.setBinds(new Bind(userCodeParentPath, new Volume("/app")));
        // 设置内存大小
        hostConfig.withMemory(100 * 1000 * 1000L);
        // 设置cpu使用核心数
        hostConfig.withCpuCount(1L);
        CreateContainerResponse createContainerResponse = containerCmd
                .withHostConfig(hostConfig)
                .withAttachStderr(true)
                .withAttachStdin(true)
                .withAttachStdout(true)
                .withTty(true)
                .exec();

        System.out.println("创建容器响应：" + createContainerResponse);
        // 获取创建容器的id
        String containerId = createContainerResponse.getId();

        // 启动容器
        dockerClient.startContainerCmd(containerId).exec();


        // 执行命令并且获取结果
        // docker exec containerName java -cp /app Main 1 3
        ArrayList<ExecuteMessage> executeMessageList = new ArrayList<>();
        for (String inputArgs : inputList) {
            StopWatch stopWatch = new StopWatch();
            String[] inputArgsArray = inputArgs.split(" ");
            String[] cmdArray = ArrayUtil.append(new String[]{"java", "-cp", "/app", "Main"}, inputArgsArray);
            ExecCreateCmdResponse execCreateCmdResponse = dockerClient.execCreateCmd(containerId)
                    .withCmd(cmdArray)
                    .withAttachStderr(true)
                    .withAttachStdin(true)
                    .withAttachStdout(true)
                    .exec();
            System.out.println("已经创建命令完成，命令：" + execCreateCmdResponse);

            ExecuteMessage executeMessage = new ExecuteMessage();

            final String[] message = {null};
            final String[] errorMassage = {null};
            long time = 0L;


            String execId = execCreateCmdResponse.getId();
            ExecStartResultCallback execStartResultCallback = new ExecStartResultCallback() {
                @Override
                public void onNext(Frame frame) {
                    StreamType streamType = frame.getStreamType();
                    if (StreamType.STDERR.equals(streamType)) {
                        errorMassage[0] = new String(frame.getPayload());
                        System.out.println("输出错误结果：" + errorMassage[0]);
                    } else {
                        message[0] = new String(frame.getPayload());
                        System.out.println("输出结果" + message[0]);
                    }
                    super.onNext(frame);
                }
            };

            // 获取占用内存
            final long[] maxMemory = {0L};
            StatsCmd statsCmd = dockerClient.statsCmd(containerId);
            ResultCallback<Statistics> statisticsResultCallback = statsCmd.exec(new ResultCallback<Statistics>() {
                @Override
                public void onNext(Statistics statistics) {
                    System.out.println("内存占用：" + statistics.getMemoryStats().getUsage());
                    maxMemory[0] = Math.max(statistics.getMemoryStats().getUsage(), maxMemory[0]);
                }

                @Override
                public void onStart(Closeable closeable) {

                }

                @Override
                public void onError(Throwable throwable) {

                }

                @Override
                public void onComplete() {

                }

                @Override
                public void close() throws IOException {

                }
            });
            statsCmd.exec(statisticsResultCallback);
            statsCmd.close();

            // 睡一会
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            try {
                // 在程序正式执行前开始计时
                stopWatch.start();
                dockerClient.execStartCmd(execId)
                        .exec(execStartResultCallback)
                        .awaitCompletion();
                // 结束时关闭计时
                stopWatch.stop();
                time = stopWatch.getLastTaskTimeMillis();
                statsCmd.close();
            } catch (InterruptedException e) {
                System.out.println("程序执行异常！");
                throw new RuntimeException(e);
            }

            // 将每一个测试用例信息加入信息列表
            executeMessage.setMessage(message[0]);
            executeMessage.setErrorMessage(errorMassage[0]);
            executeMessage.setTime(time);
            executeMessage.setMemory(maxMemory[0]);
            System.out.println("用例{}执行结果：" + inputArgs);
            System.out.println(executeMessage);
            executeMessageList.add(executeMessage);
        }


        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
        ArrayList<String> outputList = new ArrayList<>();
        // 取最大内存查看是否爆内存
        long maxMemory = 0L;
        // 取最大值判断是否超时
        long maxTime = 0;
        for (ExecuteMessage executeMessage : executeMessageList) {
            String errorMessage = executeMessage.getErrorMessage();
            if (StrUtil.isNotBlank(errorMessage)){
                executeCodeResponse.setMessage(errorMessage);
                // 用户提交的代码执行中存在错误
                executeCodeResponse.setStatus(3);
                break;
            }
            outputList.add(executeMessage.getMessage());
            Long time = executeMessage.getTime();
            if (time != null){
                maxTime = Math.max(maxTime, time);
            }
            Long memory = executeMessage.getMemory();
            if (memory != null){
                maxMemory = Math.max(maxMemory, memory);
            }
        }
        executeCodeResponse.setOutputList(outputList);
        // 正常执行
        if (outputList.size() == executeMessageList.size()){
            executeCodeResponse.setStatus(1);
        }
        JudgeInfo judgeInfo = new JudgeInfo();
        judgeInfo.setMemory(maxMemory);
        // 设置运行最大值
        judgeInfo.setTime(maxTime);
        executeCodeResponse.setJudgeInfo(judgeInfo);

        // 5. 文件清理
        if (userCodeFile.getParentFile() != null){
            boolean del = FileUtil.del(userCodeParentPath);
            System.out.println("删除" + (del ? "成功" : "失败"));
        }
        return executeCodeResponse;
    }

    /**
     * 获取错误响应
     */
    private ExecuteCodeResponse getErrorResponse(Throwable e){
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
        executeCodeResponse.setOutputList(new ArrayList<>());
        executeCodeResponse.setMessage(e.getMessage());
        // 代码沙箱错误
        executeCodeResponse.setStatus(2);
        executeCodeResponse.setJudgeInfo(new JudgeInfo());
        return executeCodeResponse;
    }
}
