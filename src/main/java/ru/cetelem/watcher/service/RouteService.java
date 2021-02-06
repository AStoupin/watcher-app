
package ru.cetelem.watcher.service;

import org.apache.camel.CamelContext;
import org.apache.camel.model.RouteDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.cetelem.watcher.model.RouteStatItem;
import ru.cetelem.watcher.registry.RouteFileRegistry;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

@Service
public class RouteService {
	private static final Logger log = LoggerFactory.getLogger(RouteService.class);

	@Autowired
	RouteFileRegistry routeFileRegistry;

	@Autowired
	RouteDefinitionConverter routeDefinitionConverter;

	@Autowired
	RouteStatService routeStatService;

	@Autowired
	private CamelContext camelContext;

	private List<RouteDefinition> routes = Collections.synchronizedList(new ArrayList<>());

	private ExecutorService threadPoolService = Executors.newFixedThreadPool(20);

	@PostConstruct
	private void init() {
		log.info("RouteService init start");
		start();


		routeFileRegistry
				.loadRoutes()
				.forEach(this::loadRouteDefinitionToCamelAsync);

		log.info("RouteService init finish");
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

		return routes.stream().filter(rd->id.equals(rd.getId())).findFirst().orElse(null); //camelContext.getRouteDefinition(id);
	}



	public void save(String routeId, String xml) {
		log.info("save xml stared  {}", routeId);
		log.debug("save xml stared for {}", xml);

		//try to load the xml
		RouteDefinition routeDefinition = routeDefinitionConverter.getXmlAsRouteDefinition(routeId, xml);

		loadRouteDefinitionToCamel(routeId, routeDefinition, true);

		save(routeDefinition);

		log.info("save xml finished  {}", routeId);

	}


	public void save(RouteDefinition routeDefinition) {
		log.info("save route started {} ", routeDefinition.getId());

		routeFileRegistry.save(routeDefinition);

		log.info("save route finished {} ", routeDefinition.getId());
	}

	public void delete(RouteDefinition routeDefinition) {
		log.info("delete route started {} ", routeDefinition.getId());

		routeFileRegistry.delete(routeDefinition);
		deleteFromCamel((routeDefinition));

		log.info("delete route finished {} ", routeDefinition.getId());
	}

	private void deleteFromCamel(RouteDefinition routeDefinition){
		log.info("deleteFromCamel route started {} ", routeDefinition.getId());

		try {
			camelContext.removeRoute(routeDefinition.getId());
			camelContext.removeRouteDefinition(routeDefinition);
			routes.removeIf(rd->routeDefinition.getId().equals(rd.getId()));

			log.info("route {} removed from camel", routeDefinition.getId());
		} catch (Exception e) {
			log.error("error during deleteFromCamel {}", routeDefinition.getId());
		}
	}

	private Future<?> loadRouteDefinitionToCamelAsync(RouteDefinition routeDefinition){

		if (routeDefinition == null){
			log.warn("loadRouteDefinitionToCamel with routeDefinition is null");
			return threadPoolService.submit(()->{});
		}

		return threadPoolService.submit(() -> {
			loadRouteDefinitionToCamel(routeDefinition.getId(),
					routeDefinition, false);});

	}

	private void loadRouteDefinitionToCamel(String routeId, RouteDefinition routeDefinition, boolean withRollback) {
		log.info("loadRouteDefinition into Camel stared for {} ", routeId);
		RouteDefinition oldRouteDefition =  findById(routeId);

			if (oldRouteDefition != null) {
				deleteFromCamel(oldRouteDefition);
			}

			if(routeDefinition==null) {
				log.warn("try to load empty routeDefinition {} ", routeId );

				return ;
			}


			try {
				routes.add(routeDefinition);
				//This operation may takes long time
				camelContext.addRouteDefinitions(Arrays.asList(routeDefinition));

				log.info("loadRouteDefinition into Camel finished for {} ", routeId);
			} catch (Exception e) {
				log.error("Error during loadRouteDefinition definition", e);

				routeStatService.addStatItem(routeId,
						new RouteStatItem(routeId,"", false,
								"Error during the load." +
										(oldRouteDefition!=null && withRollback ? " With rollback":""),
								e.getMessage()));

				deleteFromCamel(routeDefinition);

				//rollback from file
				if (oldRouteDefition!=null && withRollback) {
					log.error("rolling back {}", oldRouteDefition.getId());

					loadRouteDefinitionToCamel(routeId,
							routeFileRegistry.getRouteDefinition(oldRouteDefition.getId())
							, false);


				}

				throw new RuntimeException(e);
			}



	}


	public List<RouteDefinition> getRoutes() {
		return routes;
	}
}