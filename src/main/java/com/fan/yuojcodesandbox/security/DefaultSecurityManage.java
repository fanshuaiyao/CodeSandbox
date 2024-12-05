package com.fan.yuojcodesandbox.security;

import java.security.Permission;

/**
 * @author fanshuaiyao
 * @description: TODO
 * @date 2024/11/27 22:39
 */
public class DefaultSecurityManage extends SecurityManager {

    @Override
    public void checkPermission(Permission perm) {
        System.out.println("默认不使用任何限制！");
        super.checkPermission(perm);
    }
}
