package com.shu.backend.global.file;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "file.storage")
public class FileStorageProperties {

    private String bucket;
    private String studentCardDir;
    private String baseUrl;
    private String chatDir;
}