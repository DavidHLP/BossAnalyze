package com.david.hlp.Spring.system.auth;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;
import org.springframework.lang.NonNull;

@Configuration
public class WebMvcConfig extends WebMvcConfigurationSupport {

    /**
     * @Description: 设置静态资源映射
     * @Author: 翰戈.summer
     * @Date: 2023/11/17
     * @Param: ResourceHandlerRegistry
     * @Return: void
     */
    @Override
    protected void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {

        //处理静态资源无法访问
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/");
        //处理Swagger无法访问
        registry.addResourceHandler("/swagger-ui.html")
                .addResourceLocations("classpath:/META-INF/resources/");
        registry.addResourceHandler("/doc.html")
                .addResourceLocations("classpath:/META-INF/resources/");
        //处理Swagger的js文件无法访问
        registry.addResourceHandler("/webjars/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/");
    }
}
