package ru.cetelem.watcher.service;

import org.apache.camel.AsyncCallback;
import org.apache.camel.AsyncProcessor;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.NamedNode;
import org.apache.camel.Processor;
import org.apache.camel.model.ProcessorDefinition;
import org.apache.camel.spi.InterceptStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

//@Component
public class ContainerWideInterceptor implements InterceptStrategy {

	private static final Logger log = LoggerFactory.getLogger(ContainerWideInterceptor.class);
    private static int count;

    public ContainerWideInterceptor() {
    	log.info("ContainerWideInterceptor startd");
    }

    public int getCount() {
        return count;
    }

	@Override
	public Processor wrapProcessorInInterceptors(CamelContext context, ProcessorDefinition<?> definition,
			Processor target, Processor nextTarget) throws Exception {
		
        //log.info("I am the container wide interceptor. Intercepted : " + definition.getParent().getId());

        // as this is based on an unit test we are a bit lazy and just create an inlined processor
        // where we implement our interception logic.
        return new AsyncProcessor() {
            public boolean process(Exchange exchange, AsyncCallback callback) {
           
            	try {
					process(exchange);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					log.error("Error");
				}
          
    			return false;
            }

            @Override
            public String toString() {
                return "ContainerWideInterceptor[" + target + "]";
            }

			@Override
			public void process(Exchange exchange) throws Exception {
                // we just count number of interceptions
                count++;
                //log.info("I am the container wide interceptor. Intercepted total count: " + count);
                //log.info("I am the container wide interceptor. target: {} ", target.toString());
                //log.info("I am the container wide interceptor. nextTarget: {} ", nextTarget.toString());
                // its important that we delegate to the real target so we let target process the exchange
                
        		String completeMessage = "ok";
        		if (exchange.getProperty(Exchange.EXCEPTION_CAUGHT)!= null)
        			completeMessage = exchange.getProperty(Exchange.EXCEPTION_CAUGHT).toString();
        		
        		log.info("ContainerWideInterceptor - Route {} finished with result {}", exchange.getFromRouteId(), completeMessage);
                
                target.process(exchange);
				
			}

        };
	}
}