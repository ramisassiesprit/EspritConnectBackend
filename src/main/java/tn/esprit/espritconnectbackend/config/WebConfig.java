package tn.esprit.espritconnectbackend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        exposeDirectory("uploads/groupsImages", "/groupsImages/**", registry);
        exposeDirectory("uploads/jobsImages", "/jobImages/**", registry);
        exposeDirectory("uploads/resourceCovers", "/resourceCovers/**", registry);
        exposeDirectory("uploads", "/uploads/**", registry);
    }

    private void exposeDirectory(String dirName, String handler, ResourceHandlerRegistry registry) {
        Path uploadDir = Paths.get(dirName);
        String uploadPath = uploadDir.toFile().getAbsolutePath();

        registry.addResourceHandler(handler).addResourceLocations("file:/" + uploadPath + "/");
    }
}
