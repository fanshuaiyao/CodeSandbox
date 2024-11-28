package com.fan.yuojcodesandbox;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import com.fan.yuojcodesandbox.model.ExecuteCodeRequest;
import com.fan.yuojcodesandbox.model.ExecuteCodeResponse;
import com.fan.yuojcodesandbox.model.ExecuteMessage;
import com.fan.yuojcodesandbox.model.JudgeInfo;
import com.fan.yuojcodesandbox.utils.ProcessUtils;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Volume;
import com.github.dockerjava.core.DockerClientBuilder;

import java.io.File;
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
        // if (FIRST_INIT){
        //     PullImageCmd pullImageCmd = dockerClient.pullImageCmd(image);
        //     PullImageResultCallback pullImageResultCallback = new PullImageResultCallback() {
        //         @Override
        //         public void onNext(PullResponseItem item) {
        //             System.out.println("下载镜像：" + item.getStatus());
        //             super.onNext(item);
        //         }
        //     };
        //     try {
        //         pullImageCmd.exec(pullImageResultCallback).awaitCompletion();
        //     } catch (InterruptedException e) {
        //         System.out.println("拉取镜像异常！");
        //         throw new RuntimeException(e);
        //     }
        //     // FIRST_INIT = false;
        // }
        // System.out.println("下载完成！");

        // 3.3 创建容器
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
        System.out.println("结束！！！！");

        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();


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
