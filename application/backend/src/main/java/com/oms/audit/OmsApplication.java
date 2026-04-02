package com.oms.audit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Spring Boot 应用主启动类
 * 包含应用程序的入口点，并启用了定时任务功能
 */
@SpringBootApplication
@EnableScheduling // 启用 Spring 的定时任务支持（例如用于日志同步任务）
public class OmsApplication {

	/**
	 * 主函数，启动 Spring Boot 应用程序
	 * @param args 命令行参数
	 */
	public static void main(String[] args) {
		SpringApplication.run(OmsApplication.class, args);
	}

}
