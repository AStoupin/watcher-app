package ru.cetelem.watcher.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ru.cetelem.watcher.health.RouteStat;

@Configuration
public class MetricsConfig {


	@Bean
	public RouteStat routeStat(){
		return new RouteStat(); 
	}
}
