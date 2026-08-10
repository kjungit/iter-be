package com.example.iter.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

// BaseTimeEntity/BaseCreatedAtEntity의 @CreatedDate, @LastModifiedDate가 동작하려면 필요
@Configuration
@EnableJpaAuditing
public class JpaConfig {
}
