
package ru.cetelem.watcher.service;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import javax.annotation.PostConstruct;
import javax.xml.bind.JAXBException;
import org.apache.camel.CamelContext;
import org.apache.camel.ServiceStatus;
import org.apache.camel.model.ModelHelper;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.model.RoutesDefinition;
import org.apache.camel.spring.boot.CamelConfigurationProperties;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Service
public class RouteService {
	private static final Logger log = LoggerFactory.getLogger(RouteService.class);
	private final String DISABLED_EXT = ".disabled";
	private final String ENABLED_EXT = ".xml";
	@Autowired
	CamelConfigurationProperties camelConfigurationProperties;
	@Autowired
	ApplicationContext applicationContext;
	@Value("${xmlRoutes.directory}")
	String xmlRoutesDirectory;
	@Autowired
	private CamelContext camelContext;
	
	@Autowired
	ErrorProcessor errorProcessor;
	@Autowired
	CompleteProcessor completeProcessor;
	//@Autowired
	//ContainerWideInterceptor containerWideInterceptor;
	
	@PostConstruct
	private void init() {
		start();
		loadRoutes(xmlRoutesDirectory);

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


	private void loadRoutes(String directory) {
		log.info("Loading additional Camel XML routes from: {}", directory);
		try {
			Resource[] xmlRoutes = applicationContext.getResources(directory);
			for (Resource xmlRoute : xmlRoutes) {
				log.debug("Found XML route: {}", xmlRoute);
				if (!(xmlRoute.getFile().getName().endsWith(ENABLED_EXT)
						|| xmlRoute.getFile().getName().endsWith(DISABLED_EXT)))
					continue;

				loadRouteDefinitionFromFile(FilenameUtils.removeExtension(xmlRoute.getFilename()));
			}
		} catch (Exception e) {
			log.error("No XML routes found in {}. Skipping XML routes detection.", directory);
		}
	}

	private void fixRouteDefinition(String routeId, RouteDefinition routeDefinition) {
		routeDefinition.setId(routeId);
		routeDefinition.getInputs().forEach(f -> {
			f.setCustomId(false);
			f.setId(null);
		});
		
		routeDefinition.getOutputs().forEach(f -> {
			f.setCustomId(false);
		});
				
		cleanRouteDefinition(routeDefinition);
		enrichRouteDefinition(routeDefinition);
		
	}

	public RouteDefinition findById(String id) {
		return camelContext.getRouteDefinition(id);
	}

	public void save(String routeId, String xml) {
		log.info("save xml stared  ");
		log.debug("save xml stared for {}", xml);
		RoutesDefinition rd = getXmlAsRoutesDefinition(xml);
		RouteDefinition routeDefinition = rd.getRoutes().get(0);
		loadRouteDefinition(routeId, routeDefinition);
		save(routeDefinition);
	}

	public void save(RouteDefinition routeDefinition) {
		try {
			log.info("save stared for {} ", routeDefinition.getId());
			String xml = getRouteDefinitionAsXml(routeDefinition);
			File routeFile = getRouteFile(routeDefinition);
			Resource[] xmlRoutes = applicationContext.getResources(xmlRoutesDirectory);
			Optional<Resource> rd = Arrays.asList(xmlRoutes).stream()
					.filter(r -> FilenameUtils.removeExtension(r.getFilename()).equals(routeDefinition.getId()))
					.findFirst();
			if (rd.isPresent()) {
				rd.get().getFile().delete();
				log.info("Updated route {} ", routeFile.getName());
			} else {
				log.info("Saved new route {} ", routeFile.getName());
			}
			try (PrintWriter out = new PrintWriter(routeFile)) {
				out.println(xml);
			}
		} catch (IOException e) {
			log.error("Error during save definition", e);
		}
	}

	private void loadRouteDefinition(String routeId, RouteDefinition routeDefinition) {
		log.info("loadRouteDefinition stared for {} ", routeId);
		RouteDefinition oldRouteDefition = findById(routeId);
		try {
			if (oldRouteDefition != null) {
				delete(oldRouteDefition, false);
			}
			
			fixRouteDefinition(routeId, routeDefinition);

			camelContext.addRouteDefinitions(Arrays.asList(routeDefinition));
			
		} catch (Exception e) {
			log.error("Error during loadRouteDefinition definition", e);
			//rollback
			delete(routeDefinition, false);
			if (oldRouteDefition!=null) {
				log.error("rolling back {}", oldRouteDefition.getId());
				loadRouteDefinitionFromFile(oldRouteDefition.getId());
			}
			
			throw new RuntimeException(e);
			
		}
	}
	

	private void enrichRouteDefinition(RouteDefinition routeDefinition) {
		routeDefinition.onException(Exception.class).process(errorProcessor);
		routeDefinition.onCompletion().process(completeProcessor);
		
		if(routeDefinition.getInputs().get(0).getEndpointUri().startsWith("ftp:") &&
				!StringUtils.contains(routeDefinition.getInputs().get(0).getEndpointUri(), "bridgeErrorHandler")){
			routeDefinition.getInputs().get(0).setUri(
					routeDefinition.getInputs().get(0).getEndpointUri() + "&bridgeErrorHandler=true"
					);
		}

		if(routeDefinition.getInputs().get(0).getEndpointUri().startsWith("ftp:") &&
				!StringUtils.contains(routeDefinition.getInputs().get(0).getEndpointUri(), "throwExceptionOnConnectFailed")){
			routeDefinition.getInputs().get(0).setUri(
					routeDefinition.getInputs().get(0).getEndpointUri() + "&throwExceptionOnConnectFailed=true"
					);
		}
		
	}
	
	private void cleanRouteDefinition(RouteDefinition routeDefinition) {
		if(routeDefinition.getInputs().get(0).getEndpointUri().startsWith("ftp:")){
			routeDefinition.getInputs().get(0).setUri(
					StringUtils.replace(routeDefinition.getInputs().get(0).getEndpointUri(), "&bridgeErrorHandler=true", "") 
					);
		}

		if(routeDefinition.getInputs().get(0).getEndpointUri().startsWith("ftp:")){
			routeDefinition.getInputs().get(0).setUri(
					StringUtils.replace(routeDefinition.getInputs().get(0).getEndpointUri(), "&throwExceptionOnConnectFailed=true", "") 
					);
		}

		
		routeDefinition.getOutputs().removeIf(
				pd->{
					return pd instanceof org.apache.camel.model.OnExceptionDefinition;
					}
			);
		
		routeDefinition.getOutputs().removeIf(
				pd->{
					return pd instanceof org.apache.camel.model.OnCompletionDefinition;
					}
			);		
	}
	
	private void loadRouteDefinitionFromFile(String id) {
		Resource xmlRoute = findRouteResource(id);
		if(xmlRoute==null)
			return;
		RouteDefinition routeDefinition=null;
		try {
			RoutesDefinition xmlDefinition = camelContext.loadRoutesDefinition(xmlRoute.getInputStream());
			routeDefinition = xmlDefinition.getRoutes().get(0);
			String routeId = FilenameUtils.removeExtension(xmlRoute.getFile().getName());

			boolean autoStart = xmlRoute.getFile().getName().endsWith(ENABLED_EXT);
			xmlDefinition.getRoutes().forEach(rd -> rd.autoStartup("false"));
			
			loadRouteDefinition(routeId, routeDefinition);
			if(autoStart)
				camelContext.startRoute(routeId);
			
			log.info("Processed Route: {} - enabled: {}", xmlRoute.getFile().getName(), autoStart);
		} catch (Exception e) {
			log.error("Error during add or start route {} {} ", routeDefinition.getId(), e);
		}				

		
	}

	public void delete(RouteDefinition routeDefinition, boolean deleteFile) {
		try {
			log.info("save delete for {} ", routeDefinition.getId());
			File routeFile = getRouteFile(routeDefinition);
			Resource[] xmlRoutes = applicationContext.getResources(xmlRoutesDirectory);
			Optional<Resource> rd = Arrays.asList(xmlRoutes).stream()
					.filter(r -> FilenameUtils.removeExtension(r.getFilename()).equals(routeDefinition.getId()))
					.findFirst();
			if (rd.isPresent()) {
				if(deleteFile) {
					rd.get().getFile().delete();
					log.info("deleted file {} ", routeFile.getName());
				}
				
			}
			camelContext.removeRoute(routeDefinition.getId());
			camelContext.removeRouteDefinition(routeDefinition);
			log.info("deleted route {} ", routeFile.getName());
		} catch (Exception e) {
			log.error("Error during delete definition", e);
		}
	}
	
	
	
	public String getRouteDefinitionAsXml(RouteDefinition routeDefinition) {
		String xml = "";
		try {
			
			cleanRouteDefinition(routeDefinition);
			
			xml = ModelHelper.dumpModelAsXml(camelContext, routeDefinition);
		} catch (JAXBException e) {
			log.error("Error during getRouteXml ", e);
		}
		return xml;
	}

	public RoutesDefinition getXmlAsRoutesDefinition(String xml) {
		RoutesDefinition xmlDefinition = null;
		try {
			xmlDefinition = camelContext.loadRoutesDefinition(new ByteArrayInputStream(xml.getBytes()));
		} catch (Exception e) {
			log.error("Error during getXmlAsRoutesDefinition ", e);
			throw new RuntimeException(e);
		}
		return xmlDefinition;
	}

	private File getRouteFile(RouteDefinition routeDefinition) {
		Resource[] xmlRoutes = null;
		File f = null;
		ServiceStatus ss = camelContext.getRouteStatus(routeDefinition.getId());
		String ext = ss != null && ss.isStopped() ? DISABLED_EXT : ENABLED_EXT;
		String fileName = routeDefinition.getId() + ext;
		try {
			xmlRoutes = applicationContext.getResources(xmlRoutesDirectory);
			f = new File(xmlRoutes[0].getFile().getParentFile(), fileName);
		} catch (IOException e) {
			log.error("Error during getRouteFile", e);
		}
		return f;
	}

	private Resource findRouteResource(String routeId) {

		File f = null;
		Resource[] xmlRoutes;
		try {
			xmlRoutes = applicationContext.getResources(xmlRoutesDirectory);
			Optional<Resource> rd = Arrays.asList(xmlRoutes).stream()
				.filter(r -> FilenameUtils.removeExtension(r.getFilename()).equals(routeId))
				.findFirst();
			if (rd.isPresent()) {
				return rd.get();
			}
		} catch (IOException e) {
			log.error("Error during getRouteResource", e);
			return null;
		}

		return null;
	}	
}