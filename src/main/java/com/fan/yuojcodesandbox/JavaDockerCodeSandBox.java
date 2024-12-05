package com.fan.yuojcodesandbox;

import cn.hutool.core.date.StopWatch;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.ArrayUtil;
import com.fan.yuojcodesandbox.model.ExecuteCodeRequest;
import com.fan.yuojcodesandbox.model.ExecuteCodeResponse;
import com.fan.yuojcodesandbox.model.ExecuteMessage;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import org.springframework.stereotype.Component;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Docker 代码沙箱实现
 */
@Component
public class JavaDockerCodeSandBox extends JavaCodeSandboxTemplate{
    public static final boolean FIRST_INIT = true;
    // 程序运行超时时间
    public static final long TIME_OUT = 10000L;
    public static void main(String[] args) {
        JavaDockerCodeSandBox javaDockerCodeSandBox = new JavaDockerCodeSandBox();
        ExecuteCodeRequest executeCodeRequest = new ExecuteCodeRequest();
        executeCodeRequest.setInputList(Arrays.asList("1 2", "1 3"));
        // 从文件中读代码
        String code = ResourceUtil.readStr("testCode/simpleComputeArgs/Main.java", StandardCharsets.UTF_8);
        executeCodeRequest.setCode(code);
        executeCodeRequest.setLanguage("java");
        ExecuteCodeResponse executeCodeResponse = javaDockerCodeSandBox.executeCode(executeCodeRequest);
        System.out.println("executeCodeResponse = " + executeCodeResponse);
    }

    /**
     * 3. 覆盖父类的runFile方法，换成执行docker的执行方式
     * @param userCodeFile
     * @param inputList
     * @return
     */
    @Override
    public List<ExecuteMessage> runFile(File userCodeFile, List<String> inputList) {
        String userCodeParentPath = userCodeFile.getParentFile().getAbsolutePath();
        // 3. 创建容器
        // 3.1 创建docker服务
        DockerClient dockerClient = DockerClientBuilder.getInstance().build();

        // 3.2 拉取镜像
        String image = "openjdk:8-alpine";
        if (FIRST_INIT){
            PullImageCmd pullImageCmd = dockerClient.pullImageCmd(image);
            PullImageResultCallback pullImageResultCallback = new PullImageResultCallback() {
                @Override
                public void onNext(PullResponseItem item) {
                    System.out.println("下载镜像：" + item.getStatus());
                    super.onNext(item);
                }
            };
            try {
                pullImageCmd.exec(pullImageResultCallback).awaitCompletion();
            } catch (InterruptedException e) {
                System.out.println("拉取镜像异常！");
                throw new RuntimeException(e);
            }
            // FIRST_INIT = false;
        }
        System.out.println("下载完成！");

        //   3.3 创建容器
        CreateContainerCmd containerCmd = dockerClient.createContainerCmd(image);
        HostConfig hostConfig = new HostConfig();
        // 文件地址映射绑定  容器挂载目录
        hostConfig.setBinds(new Bind(userCodeParentPath, new Volume("/app")));
        // 设置内存大小  通过withMemory设置资源限制
        hostConfig.withMemory(100 * 1000 * 1000L);
        // 设置cpu使用核心数
        hostConfig.withCpuCount(1L);
        // 设置内存磁盘交换
        hostConfig.withMemorySwap(0L);
        // linux内核安全管理
        // hostConfig.withSecurityOpts(Arrays.asList("seccomp=安全管理配置字符串"));
        CreateContainerResponse createContainerResponse = containerCmd
                .withHostConfig(hostConfig)
                .withAttachStderr(true)
                .withAttachStdin(true)
                .withAttachStdout(true)
                .withTty(true)
                // 设置网络
                .withNetworkDisabled(true)
                // 限制用户不能向root目录写文件
                .withReadonlyRootfs(true)
                // 将JAVA安全管理器和容器安全配合使用
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
            System.out.println("已经创建命令完成，命令是：" + execCreateCmdResponse);

            ExecuteMessage executeMessage = new ExecuteMessage();
            executeMessage.setExitValue(0);

            final String[] message = {null};
            final String[] errorMassage = {null};
            long time = 0L;
            // 判断是否超时
            final boolean[] timeout = {true};

            String execId = execCreateCmdResponse.getId();
            ExecStartResultCallback execStartResultCallback = new ExecStartResultCallback() {
                @Override
                public void onComplete() {
                    // 如果在超时时间内执行完成  设置不超时
                    timeout[0] = false;
                    super.onComplete();
                }

                @Override
                public void onNext(Frame frame) {
                    StreamType streamType = frame.getStreamType();
                    if (StreamType.STDERR.equals(streamType)) {
                        errorMassage[0] = new String(frame.getPayload());
                        System.out.println("错误输出: " + errorMassage[0]);
                    } else {
                        message[0] = new String(frame.getPayload()).trim();
                        System.out.println("标准输出: " + message[0]);
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
                        // 超时退出
                        .awaitCompletion(TIME_OUT, TimeUnit.MILLISECONDS);
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
            System.out.println("用例" + inputArgs + "执行结果：");
            System.out.println(executeMessage);
            executeMessageList.add(executeMessage);
        }

        // 删除容器，确保容器被删除
        try {
            dockerClient.removeContainerCmd(containerId)
                    .withForce(true) // 强制删除正在运行的容器
                    .exec();
            System.out.println("容器 " + containerId + " 已删除");
        } catch (Exception e) {
            System.out.println("删除容器失败: " + e.getMessage());
        }

        return executeMessageList;
    }

}
