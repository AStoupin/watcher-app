
package ru.cetelem.watcher.service;

import org.apache.camel.CamelContext;
import org.apache.camel.model.RouteDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.cetelem.watcher.registry.RouteFileRegistry;

import javax.annotation.PostConstruct;

@Service
public class RouteService {
	private static final Logger log = LoggerFactory.getLogger(RouteService.class);

	@Autowired
	RouteFileRegistry routeFileRegistry;
	@Autowired
	private CamelContext camelContext;

	@PostConstruct
	private void init() {
		start();
		routeFileRegistry.loadRoutes();

	}

	private void start() {
		log.info("Starting Camel");
		try {
			camelContext.start();
		} catch (Exception e) {
			log.error("Error during start Camel {}", e);
			throw new RuntimeException(e);
		}
		log.info("Camel started");
	}


	/**
	 *  find Route definition id
	 * @param id
	 * @return
	 */
	public RouteDefinition findById(String id) {

		return camelContext.getRouteDefinition(id);
	}


	public void save(String routeId, String xml) {
		routeFileRegistry.save(routeId, xml);
	}

	public void save(RouteDefinition routeDefinition) {
		routeFileRegistry.save(routeDefinition);
	}

	public void delete(RouteDefinition routeDefinition, boolean deleteFile) {
		routeFileRegistry.delete(routeDefinition, deleteFile);
	}

}