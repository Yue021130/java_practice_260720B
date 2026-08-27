package com.example.nf.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * NIO 文件操作配置。
 *
 * <p>所有文件操作都限制在 {@code workDir} 指定的沙箱目录内，防止路径遍历。</p>
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "nio")
public class NioProperties {

    /**
     * 文件操作沙箱目录，默认项目根目录下的 nio-work。
     */
    private String workDir = "./nio-work";

    /**
     * 启动时确保沙箱目录存在。
     */
    @PostConstruct
    public void init() throws IOException {
        Path dir = Paths.get(workDir).toAbsolutePath().normalize();
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
        }
    }
}
