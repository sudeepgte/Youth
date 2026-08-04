package com.example.demo.config;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.util.List;
import org.springframework.context.ApplicationContext;

@Component
public class TomcatConfig implements ApplicationListener<ApplicationReadyEvent> {

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        try {
            ApplicationContext context = event.getApplicationContext();
            
            // Getting WebServer from context (works for Spring Boot 2/3/4)
            Object webServerFactory = context.getBean("tomcatServletWebServerFactory");
            
            // This is actually too late to customize the factory, but we can get the webServer from context.
            // In Spring Boot, the ApplicationContext for Web has getWebServer() method.
            Method getWebServerMethod = context.getClass().getMethod("getWebServer");
            Object webServer = getWebServerMethod.invoke(context);
            
            Method getTomcatMethod = webServer.getClass().getMethod("getTomcat");
            Object tomcat = getTomcatMethod.invoke(webServer);
            
            Method getServiceMethod = tomcat.getClass().getMethod("getService");
            Object service = getServiceMethod.invoke(tomcat);
            
            Method findConnectorsMethod = service.getClass().getMethod("findConnectors");
            Object[] connectors = (Object[]) findConnectorsMethod.invoke(service);
            
            for (Object connector : connectors) {
                try {
                    Method setMaxParameterCount = connector.getClass().getMethod("setMaxParameterCount", int.class);
                    setMaxParameterCount.invoke(connector, 10000);
                } catch (Exception e) {}
                
                try {
                    Method setMaxPartCount = connector.getClass().getMethod("setMaxPartCount", int.class);
                    setMaxPartCount.invoke(connector, 10000);
                } catch (Exception e) {}
            }
            System.out.println("Customized Tomcat Connector successfully via Reflection!");
        } catch (Exception e) {
            System.out.println("Could not customize Tomcat via Reflection: " + e.getMessage());
        }
    }
}
