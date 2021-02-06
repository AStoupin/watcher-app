package ru.cetelem.watcher.service;

import org.apache.camel.model.OnCompletionDefinition;
import org.apache.camel.model.OnExceptionDefinition;
import org.apache.camel.model.ProcessorDefinition;
import org.apache.camel.model.RouteDefinition;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class RouteDefinitionEnricher {
    private static final Logger log = LoggerFactory.getLogger(RouteDefinitionEnricher.class);

    @Autowired
    private ErrorProcessor errorProcessor;
    @Autowired
    private CompleteProcessor completeProcessor;
    
    /**
     * Add extra functionality to the specified route
     * <ul style="list-style-type: decimal">
     * <li> Exception/OnComplete handling to watcher-app processors (to handle statuses) </li>
     * <li> for ftp endpoints add bridgeErrorHandler=true + throwExceptionOnConnectFailed=true </li>
     * <ul>
     *
     * @param routeDefinition
     */
    public void enrichRouteDefinition(String routeId, RouteDefinition routeDefinition) {
        routeDefinition.setId(routeId);

        if(routeDefinition.getInputs().size()==0){
            log.warn("no inputs in routeDefinition {} ", routeDefinition.getId());
            return;
        }

        routeDefinition.onException(Exception.class).process(errorProcessor);
        routeDefinition.onCompletion().process(completeProcessor);

        if (routeDefinition.getInputs().get(0).getEndpointUri().startsWith("ftp:") &&
                !StringUtils.contains(routeDefinition.getInputs().get(0).getEndpointUri(), "bridgeErrorHandler")) {
            routeDefinition.getInputs().get(0).setUri(
                    routeDefinition.getInputs().get(0).getEndpointUri() + "&bridgeErrorHandler=true"
            );
        }

        if (routeDefinition.getInputs().get(0).getEndpointUri().startsWith("ftp:") &&
                !StringUtils.contains(routeDefinition.getInputs().get(0).getEndpointUri(), "throwExceptionOnConnectFailed")) {
            routeDefinition.getInputs().get(0).setUri(
                    routeDefinition.getInputs().get(0).getEndpointUri() + "&throwExceptionOnConnectFailed=true"
            );
        }

    }

    /**
     * Clean extra functionality to the specified route (added by enrichRouteDefinition)
     *
     * @param routeDefinition
     */
    public void cleanExtraRouteDefinition(RouteDefinition routeDefinition) {

        if(routeDefinition.getInputs().size()==0){
            log.warn("no inputs in routeDefinition {} ", routeDefinition.getId());
            return;
        }

        if (routeDefinition.getInputs().get(0).getEndpointUri().startsWith("ftp:")) {
            routeDefinition.getInputs().get(0).setUri(
                    StringUtils.replace(routeDefinition.getInputs().get(0).getEndpointUri(), "&bridgeErrorHandler=true", "")
            );
        }

        if (routeDefinition.getInputs().get(0).getEndpointUri().startsWith("ftp:")) {
            routeDefinition.getInputs().get(0).setUri(
                    StringUtils.replace(routeDefinition.getInputs().get(0).getEndpointUri(), "&throwExceptionOnConnectFailed=true", "")
            );
        }


        routeDefinition.getOutputs().removeIf(
                pd -> {
                    return pd instanceof OnExceptionDefinition;
                }
        );

        routeDefinition.getOutputs().removeIf(
                pd -> {
                    return pd instanceof OnCompletionDefinition;
                }
        );
    }




}