package ru.cetelem.watcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.ComponentScan;

import ru.cetelem.watcher.service.RouteService;

@ComponentScan({"ru.cetelem.watcher.service", "ru.cetelem.watcher.config"})
public class WatcherAppTestConfig {
	private static final Logger log = LoggerFactory.getLogger(WatcherAppTestConfig.class);
	
	
	public WatcherAppTestConfig() {
		log.info("WatcherAppTestConfig started");
	}

}
