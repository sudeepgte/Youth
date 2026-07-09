package com.example.demo;

import org.springframework.beans.factory.annotation.Autowired;
import com.example.demo.config.AuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private AuthInterceptor authInterceptor;

    @Override
    public void addInterceptors(org.springframework.web.servlet.config.annotation.InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/", "/home", "/login", "/register", "/css/**", "/js/**", "/images/**", "/uploads/**", "/play-*", "/play-runner");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path uploadDir = Paths.get("src/main/resources/static/uploads").toAbsolutePath();
        String uploadUrl = uploadDir.toUri().toString();
        if (!uploadUrl.endsWith("/")) {
            uploadUrl += "/";
        }

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadUrl, "classpath:/static/uploads/");
    }
}
