package ru.cetelem.watcher.health;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.web.annotation.RestControllerEndpoint;
import org.springframework.web.bind.annotation.GetMapping;

import ru.cetelem.watcher.model.RouteStatItem;
import ru.cetelem.watcher.service.RouteStatService;

/**
 * extract route metrics (statistics) on /watcher-app/actuator/routeStat endpoint
 * @author Art
 *
 */
@RestControllerEndpoint(id = "routeStat")
public class RouteStat {
	@Autowired
	public RouteStatService routeStatService;
	
	@GetMapping
	public List<RouteStatItem> get(){
		return routeStatService.getStatList();
	}
}
