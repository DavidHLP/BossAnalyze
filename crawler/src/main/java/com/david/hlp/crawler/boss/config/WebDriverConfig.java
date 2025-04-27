package com.david.hlp.crawler.boss.config;

import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.util.Random;

/**
 * WebDriver配置类
 */
@Configuration
@Slf4j
public class WebDriverConfig {

    @Value("${webdriver.chrome.driver:/home/david/Driver/chromedriver-linux64/chromedriver}")
    private String chromeDriverPath;

    @Value("${webdriver.chrome.headless:true}")
    private boolean headless;

    @Value("${webdriver.chrome.disable-gpu:true}")
    private boolean disableGpu;

    @Value("${webdriver.chrome.no-sandbox:true}")
    private boolean noSandbox;

    @Value("${webdriver.chrome.disable-dev-shm-usage:true}")
    private boolean disableDevShmUsage;

    /**
     * 随机数生成器，用于随机等待时间
     */
    @Bean
    public Random random() {
        return new Random();
    }

    /**
     * 初始化ChromeDriver
     */
    @PostConstruct
    public void initChromeDriver() {
        // 验证ChromeDriver是否存在
        File driverFile = new File(chromeDriverPath);
        if (!driverFile.exists() || !driverFile.canExecute()) {
            log.error("ChromeDriver不存在或无法执行: {}", chromeDriverPath);
            return;
        }
        System.setProperty("webdriver.chrome.driver", chromeDriverPath);
        log.info("ChromeDriver初始化成功，路径: {}", chromeDriverPath);
    }

    /**
     * 配置Chrome驱动选项
     *
     * @return ChromeOptions
     */
    @Bean
    public ChromeOptions chromeOptions() {
        ChromeOptions options = new ChromeOptions();
        if (headless) {
            options.addArguments("--headless=new");
        }
        if (disableGpu) {
            options.addArguments("--disable-gpu");
        }
        if (noSandbox) {
            options.addArguments("--no-sandbox");
        }
        if (disableDevShmUsage) {
            options.addArguments("--disable-dev-shm-usage");
        }
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.addArguments("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
        options.addArguments("--lang=zh-CN");
        options.addArguments("--disable-extensions");
        options.addArguments("--disable-infobars");
        options.addArguments("--remote-allow-origins=*");
        return options;
    }
} 