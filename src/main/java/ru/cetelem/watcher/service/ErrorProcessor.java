package ru.cetelem.watcher.service;

import java.util.Optional;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.processor.DefaultExchangeFormatter;
import org.apache.commons.lang3.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("errorProcessor")
public class ErrorProcessor implements Processor{
	private static final Logger log = LoggerFactory.getLogger(ErrorProcessor.class);
	@Autowired
	private RouteStatService routeStatService;
	
	public ErrorProcessor(){
		log.info("ErrorProcessor created");
	}
	
	 public void process(Exchange exchange) throws Exception {
		log.info("ErrorProcessor process: {}", exchange.getFromRouteId());
		//DefaultErrorHandler
		DefaultExchangeFormatter formatter = new DefaultExchangeFormatter();
		formatter.setShowStackTrace(true);
		formatter.setShowCaughtException(true);
		formatter.setShowException(true);
		String errorMessage = org.apache.camel.util.MessageHelper.dumpMessageHistoryStacktrace(exchange, formatter, true);
		
		exchange.setProperty(RouteStatService.EXCHANGE_DETAILS,  StringEscapeUtils.escapeHtml4(errorMessage)  );
		
		routeStatService.add(exchange);
		//org.apache.camel.management.PublishEventNotifier
		 log.info("Route {} finished with KO result", exchange.getFromRouteId());
	  }
}
