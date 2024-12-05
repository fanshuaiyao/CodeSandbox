import java.util.ArrayList;
import java.util.List;

/**
 * @author fanshuaiyao
 * @description: 无限占用空间 浪费系统内存
 * @date 2024/11/27 20:09
 */
public class Main {
    public static void main(String[] args) {
        List<byte[]> bytes = new ArrayList<>();
        while (true) {
            bytes.add(new byte[10000]);
        }
    }
}