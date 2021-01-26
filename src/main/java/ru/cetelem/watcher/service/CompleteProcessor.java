package ru.cetelem.watcher.service;

import java.util.Optional;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("completeProcessor")
public class CompleteProcessor implements Processor {
	private static final Logger log = LoggerFactory.getLogger(ErrorProcessor.class);
	
	@Autowired
	private RouteStatService routeStatService;

	
	public CompleteProcessor(){
			log.info("CompleteProcessor created");
			
	}

	public void process(Exchange exchange) throws Exception {
		log.info("CompleteProcessor process: {}", exchange.getFromRouteId());
	
		String completeMessage = Optional.ofNullable(exchange.getProperty(Exchange.EXCEPTION_CAUGHT)).orElse("ok").toString();
		if("ok".equals(completeMessage)) {
			routeStatService.add(exchange);
			log.info("Route {} finished with OK result", exchange.getFromRouteId());
		}

	}
}
