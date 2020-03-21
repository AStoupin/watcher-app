package ru.cetelem.watcher.web;

import org.apache.camel.model.RouteDefinition;
import org.springframework.stereotype.Component;

@Component
public class SelectedRoute {
	private RouteDefinition selectedRouteDefinition;

	public RouteDefinition getSelectedRouteDefinition() {
		return selectedRouteDefinition;
	}

	public void setSelectedRouteDefinition(RouteDefinition selectedRouteDefinition) {
		this.selectedRouteDefinition = selectedRouteDefinition;
	}
}
