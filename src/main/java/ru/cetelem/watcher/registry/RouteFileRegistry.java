package ru.cetelem.watcher.registry;

import org.apache.camel.CamelContext;
import org.apache.camel.ServiceStatus;
import org.apache.camel.model.RouteDefinition;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
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

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Component
public class RouteFileRegistry {
    private static final Logger log = LoggerFactory.getLogger(RouteFileRegistry.class);

    private final String DISABLED_EXT = ".disabled";
    private final String ENABLED_EXT = ".xml";


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


    public List<RouteDefinition>  loadRoutes() {
        return loadRoutes(xmlRoutesDirectory);
    }



    /**
     * Load routes definition from repository (file system repository)
     * @param directory
     */
    private List<RouteDefinition> loadRoutes(String directory) {
        log.info("Loading additional Camel XML routes from: {}", directory);
        List<RouteDefinition> routes = new ArrayList<RouteDefinition>();
        try {
            Resource[] xmlRoutes = applicationContext.getResources(directory);
            for (Resource xmlRoute : xmlRoutes) {
                log.debug("Found XML route: {}", xmlRoute);
                if (!(xmlRoute.getFile().getName().endsWith(ENABLED_EXT)
                        || xmlRoute.getFile().getName().endsWith(DISABLED_EXT)))
                    continue;


                Optional.ofNullable(getRouteDefinition(FilenameUtils.removeExtension(xmlRoute.getFilename())))
                        .ifPresent(rd->routes.add(rd));


            }
        } catch (Exception e) {
            log.error("No XML routes found in {}. Skipping XML routes detection.", directory);
        }

        return routes;
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




    public RouteDefinition getRouteDefinition(String id) {
        Resource xmlRoute = findRouteResource(id);
        if(xmlRoute==null)
            return null;

        boolean autoStart;
        try {
            autoStart = xmlRoute.getFile().getName().endsWith(ENABLED_EXT);

            String xml = IOUtils.toString(xmlRoute.getInputStream(), StandardCharsets.UTF_8);
            RouteDefinition routeDefinition =
                    routeDefinitionConverter.getXmlAsRouteDefinition(id, xml);


            routeDefinition.setAutoStartup(String.valueOf(autoStart));

            log.info("Processed Route: {} - enabled: {}", xmlRoute.getFile().getName(), autoStart);

            return routeDefinition;

        } catch (IOException e) {
            log.error("Error during add or start route file {} {} ", id, e);
            return null;
        }


    }

    public void delete(RouteDefinition routeDefinition) {
        try {
            log.info("save delete for {} ", routeDefinition.getId());
            File routeFile = getRouteFile(routeDefinition);
            Resource[] xmlRoutes = applicationContext.getResources(xmlRoutesDirectory);
            Optional<Resource> rd = Arrays.asList(xmlRoutes).stream()
                    .filter(r -> FilenameUtils.removeExtension(r.getFilename()).equals(routeDefinition.getId()))
                    .findFirst();
            if (rd.isPresent()) {
                    rd.get().getFile().delete();
                    log.info("deleted file {} ", routeFile.getName());
            }
            else {
                log.warn("no file of route {}  to delete", routeFile.getName());
            }


        } catch (Exception e) {
            log.error("Error during delete definition", e);
        }
    }



    private File getRouteFile(RouteDefinition routeDefinition) {

        File f = null;
        ServiceStatus ss = camelContext.getRouteStatus(routeDefinition.getId());
        String ext = ss != null && ss.isStopped() ? DISABLED_EXT : ENABLED_EXT;
        String fileName = routeDefinition.getId() + ext;
        try {
            File routesDir = getRoutesDir();

            f = new File(routesDir, fileName);
        } catch (IOException e) {
            log.error("Error during getRouteFile", e);
        }
        return f;
    }

    private File getRoutesDir() throws IOException {
        File routesDir = new File(applicationContext
                .getResource(xmlRoutesDirectory)
                .getFile().getParent()).getAbsoluteFile();

        if(!routesDir.exists()) {
            log.info("Routes dir {} doesn't exist. Will be created.", xmlRoutesDirectory);

            routesDir.mkdir();
        }
        return routesDir;
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
