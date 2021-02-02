package ru.cetelem.watcher.registry;

import org.apache.camel.CamelContext;
import org.apache.camel.ServiceStatus;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.model.RoutesDefinition;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import ru.cetelem.watcher.service.CompleteProcessor;
import ru.cetelem.watcher.service.ErrorProcessor;
import ru.cetelem.watcher.service.RouteDefinitionConverter;
import ru.cetelem.watcher.service.RouteDefinitionEnricher;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Optional;

@Component
public class RouteFileRegistry {
    private static final Logger log = LoggerFactory.getLogger(RouteFileRegistry.class);

    private final String DISABLED_EXT = ".disabled";
    private final String ENABLED_EXT = ".xml";

    @Autowired
    private RouteDefinitionEnricher routeDefinitionEnricher;
    @Autowired
    RouteDefinitionConverter routeDefinitionConverter;

    @Autowired
    RouteFileRegistry routeFileRegistry;

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


    public void loadRoutes() {
        loadRoutes(xmlRoutesDirectory);
    }



    /**
     * Load routes definition from repository (file system repository)
     * @param directory
     */
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



    /**
     *  find Route definition id
     * @param id
     * @return
     */
    public RouteDefinition findById(String id) {

        return camelContext.getRouteDefinition(id);
    }

    public void save(String routeId, String xml) {
        log.info("save xml stared  ");
        log.debug("save xml stared for {}", xml);
        RoutesDefinition rd = routeDefinitionConverter.getXmlAsRoutesDefinition(xml);
        RouteDefinition routeDefinition = rd.getRoutes().get(0);
        loadRouteDefinition(routeId, routeDefinition);
        save(routeDefinition);
    }

    public void save(RouteDefinition routeDefinition) {
        try {
            log.info("save stared for {} ", routeDefinition.getId());
            String xml =  routeDefinitionConverter.routeDefinitionToXml(routeDefinition);
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

            routeDefinitionEnricher.cleanExtraRouteDefinition(routeDefinition);
            routeDefinitionEnricher.enrichRouteDefinition(routeId, routeDefinition);

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



    private void loadRouteDefinitionFromFile(String id) {
        Resource xmlRoute = findRouteResource(id);
        if(xmlRoute==null)
            return;

        boolean autoStart;
        try {
            autoStart = xmlRoute.getFile().getName().endsWith(ENABLED_EXT);

            loadRouteDefinitionFromStream(id, autoStart, xmlRoute.getInputStream());

            log.info("Processed Route: {} - enabled: {}", xmlRoute.getFile().getName(), autoStart);

        } catch (IOException e) {
            log.error("Error during add or start route file {} {} ", id, e);
        }


    }


    protected void loadRouteDefinitionFromStream(String routeId, boolean autoStart, InputStream inputStream) {
        RouteDefinition routeDefinition=null;
        try {
            RoutesDefinition xmlDefinition = camelContext.loadRoutesDefinition(inputStream);
            routeDefinition = xmlDefinition.getRoutes().get(0);

            xmlDefinition.getRoutes().forEach(rd -> rd.autoStartup("false"));

            loadRouteDefinition(routeId, routeDefinition);
            if(autoStart)
                camelContext.startRoute(routeId);

        } catch (Exception e) {
            log.error("Error during add or start route {} {} ", routeId, e);
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
