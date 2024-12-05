package com.fan.yuojcodesandbox.security;

import java.security.Permission;

/**
 * @author fanshuaiyao
 * @description: 所有权限拒绝
 * @date 2024/11/27 23:12
 */
public class DenySecurityManager extends SecurityManager{

    @Override
    public void checkPermission(Permission perm) {
        throw new SecurityException("You do not have permission to do this");
    }
}
