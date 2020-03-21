package ru.cetelem.watcher;

import javax.servlet.ServletContext;

import org.apache.camel.builder.DeadLetterChannelBuilder;
import org.apache.camel.processor.RedeliveryPolicy;
import org.apache.camel.processor.interceptor.TraceFormatter;
import org.ocpsoft.rewrite.annotation.RewriteConfiguration;
import org.ocpsoft.rewrite.config.ConfigurationBuilder;
import org.ocpsoft.rewrite.config.Direction;
import org.ocpsoft.rewrite.servlet.config.Forward;
import org.ocpsoft.rewrite.servlet.config.HttpConfigurationProvider;
import org.ocpsoft.rewrite.servlet.config.Path;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryCustomizer;
import org.springframework.boot.web.server.MimeMappings;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

import ru.cetelem.watcher.service.ErrorProcessor;

@EnableAutoConfiguration
@ComponentScan({"ru.cetelem.watcher.web", "ru.cetelem.watcher.service"})

@ImportResource("classpath:camel-context.xml")
@Configuration
@RewriteConfiguration
public class WatcherApp  extends HttpConfigurationProvider 
	implements WebServerFactoryCustomizer<ConfigurableServletWebServerFactory>{
	@Autowired
	ErrorProcessor errorProcessor;
	
    public static void main(String[] args) {
		
        SpringApplication.run(WatcherApp.class, args);

    }
    @Override
    public void customize(ConfigurableServletWebServerFactory factory) {
        MimeMappings mappings = new MimeMappings(MimeMappings.DEFAULT);

        mappings.add("xsd", "text/xml; charset=utf-8");
        mappings.add("otf", "font/opentype");
        mappings.add("ico", "image/x-icon");
        mappings.add("svg", "image/svg+xml");
        mappings.add("png", "image/png");
        mappings.add("eot", "application/vnd.ms-fontobject");
        mappings.add("ttf", "application/x-font-ttf");
        mappings.add("woff", "application/x-font-woff");
        mappings.add("woff2", "application/x-font-woff2");
        mappings.add("xhtml", "application/xml");
        mappings.add("jsf", "application/xml");
        
        factory.setMimeMappings(mappings);
    }

    
    @Bean
    public DeadLetterChannelBuilder myErrorHandler() {
    	
        DeadLetterChannelBuilder deadLetterChannelBuilder = new DeadLetterChannelBuilder();
        deadLetterChannelBuilder.setDeadLetterUri("errorProcessor");
        deadLetterChannelBuilder.setRedeliveryPolicy(new RedeliveryPolicy().disableRedelivery());
        deadLetterChannelBuilder.useOriginalMessage();
        return deadLetterChannelBuilder;
    }
    
	@Override
	public org.ocpsoft.rewrite.config.Configuration getConfiguration(ServletContext context) {
	      return ConfigurationBuilder.begin()
	        .addRule()
	          .when(Direction.isInbound().and(Path.matches("/")))
	          .perform(Forward.to("/route-list.jsf"))
	        .addRule()
	          .when(Direction.isInbound().and(Path.matches("/monitor")))
	          .perform(Forward.to("/route-monitor.jsf"))	          
		     .addRule()
	          .when(Direction.isInbound().and(Path.matches("/route-add")))
	          .perform(Forward.to("/route-edit.jsf"))
	        .addRule()
	          .when(Direction.isInbound().and(Path.matches("/route/{routeId}")))
	          .perform(Forward.to("/route-edit.jsf?routeId={routeId}"));
	      

	      
	}

	@Override
	public int priority() {
		// TODO Auto-generated method stub
		return 10;
	}


}