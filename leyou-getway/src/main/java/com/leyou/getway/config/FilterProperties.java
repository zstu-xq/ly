package com.leyou.getway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * zuul过滤白名单配置
 */

@ConfigurationProperties(prefix = "leyou.filter")
public class FilterProperties {


    private List<String> allowPaths;

    public List<String> getAllowPaths() {
        return allowPaths;
    }

    public void setAllowPaths(List<String> allowPaths) {
        this.allowPaths = allowPaths;
    }
}
