package com.ecom.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.ecom.service.FileStorageService;

@Configuration
public class WebConfig implements WebMvcConfigurer {

	@Autowired
	private FileStorageService fileStorageService;

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		registry.addResourceHandler("/uploads/**").addResourceLocations(fileStorageService.getUploadRootLocation());
	}
}
