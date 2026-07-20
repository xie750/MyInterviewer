package com.kedaxunfei.myinterviewer.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.kedaxunfei.myinterviewer.repository")
public class MybatisPlusConfig {
}

