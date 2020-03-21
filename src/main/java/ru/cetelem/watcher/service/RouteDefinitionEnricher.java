package ru.cetelem.watcher.service;

import org.apache.camel.model.OnCompletionDefinition;
import org.apache.camel.model.OnExceptionDefinition;
import org.apache.camel.model.RouteDefinition;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class RouteDefinitionEnricher {
    @Autowired
    private ErrorProcessor errorProcessor;
    @Autowired
    private CompleteProcessor completeProcessor;
    /**
     * Add extra functionality to the specified route
     * 1. Exception/OnComplete handling to watcher-app processors
     * 2. for ftp endpoints add bridgeErrorHandler=true + throwExceptionOnConnectFailed=true
     *
     * @param routeDefinition
     */
    void enrichRouteDefinition(RouteDefinition routeDefinition) {
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
    void cleanRouteDefinition(RouteDefinition routeDefinition) {
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