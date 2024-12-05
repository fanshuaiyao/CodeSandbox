package com.fan.yuojcodesandbox.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.PingCmd;
import com.github.dockerjava.core.DockerClientBuilder;

/**
 * @author fanshuaiyao
 * @description: TODO
 * @date 2024/11/28 10:28
 */
public class DockerDemo {
    public static void main(String[] args) {
        DockerClient dockerClient = DockerClientBuilder.getInstance().build();
        PingCmd pingCmd = dockerClient.pingCmd();
        pingCmd.exec();
    }
}
