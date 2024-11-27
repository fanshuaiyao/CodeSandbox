package com.fan.yuojcodesandbox.security;

import java.io.FileDescriptor;
import java.security.Permission;

/**
 * @author fanshuaiyao
 * @description: TODO
 * @date 2024/11/27 23:19
 */
public class MySecurityManage extends SecurityManager {

    @Override
    public void checkPermission(Permission perm) {
        super.checkPermission(perm);
    }

    // 检测程序是否可执行文件
    @Override
    public void checkExec(String cmd) {
        throw new SecurityException("checkExec 权限异常：" + cmd);
    }

    // 检测程序是否可以读文件
    @Override
    public void checkRead(String file) {
        throw new SecurityException("checkRead 权限异常：" + file);
    }

    // 检测程序是否可以写文件
    @Override
    public void checkWrite(String file) {
        throw new SecurityException("checkWrite 权限异常：" + file);
    }

    // 检测程序是否可以删除文件
    @Override
    public void checkDelete(String file) {
        throw new SecurityException("checkDelete 权限异常：" + file);
    }

    // 检测程序是否可以连接网络
    @Override
    public void checkConnect(String host, int port) {
        throw new SecurityException("checkConnect  权限异常：" + host + ":" + port);
    }
}
