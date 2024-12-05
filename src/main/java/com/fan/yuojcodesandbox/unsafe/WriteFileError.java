package com.fan.yuojcodesandbox.unsafe;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

/**
 * @author fanshuaiyao
 * @description: TODO
 * @date 2024/11/27 20:41
 */
public class WriteFileError {
    public static void main(String[] args) throws IOException {
        String userDir = System.getProperty("user.dir");

        String filePath = userDir + File.separator + "src/main/resources/木马程序.bat";
        String errorProgram = "java -version 2>&1";
        Files.write(Paths.get(filePath), Arrays.asList(errorProgram));
        System.out.println("木马写入成功！");

    }
}
