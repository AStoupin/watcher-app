package ru.cetelem.watcher.web;

import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import ru.cetelem.watcher.model.UriData;

import javax.enterprise.context.SessionScoped;
import javax.faces.context.FacesContext;
import java.io.IOException;
import java.io.Serializable;

@Component(value = "routeMasterController")
@Scope(value = "session")
public class RouteMasterController implements Serializable {
    private static final Logger log = LoggerFactory.getLogger(RouteMonitorController.class);

    @Setter
    @Getter
    private UriData uriFrom;
    @Setter
    @Getter
    private UriData uriTo;

    @Setter
    @Getter
    private int activeStep  =0;

    @Setter
    @Getter
    private String xml;

    @Autowired
    UriEditorController uriEditorController;
    @Autowired
    RouteEditController routeEditController;

    public RouteMasterController(){
        initWizard();
    }


    public void init(){
        FacesContext.getCurrentInstance().getExternalContext().getSessionMap().put("pageIndex", "-1");
        log.info("RouteMasterController init");
    }

    public void goBack(){
        activeStep--;
        return ;
    }
    public void goNext() throws IOException {
        if (activeStep==2) {
            redirectToRouteEditor(xml);
            return ;
        }

        activeStep++;
        if (activeStep==2){
            xml = uriEditorController.getRouteXml(uriFrom, uriTo);
        }
        return ;
    }

    private void redirectToRouteEditor(String xml) throws IOException{
        routeEditController.setXml(xml);
        initWizard();
        FacesContext.getCurrentInstance().getExternalContext().
                redirect("/wizard-result");

    }

    private void initWizard() {
        activeStep=0;
        this.uriFrom = UriData.builder()
                .uriDirection("from")
                .uriType("ftp")
                .delay("30")
                .transferType("moveAndDelete")
                .move(".done")
                .moveFailed(".error")
                .doneFileName(".ok")
                .flagProcess("fileOnly")
                .build();
        this.uriTo = UriData.builder()
                .uriDirection("to")
                .uriType("ftp")
                .flagProcess("fileOnly")
                .doneFileName(".ok")
                .build();
    }

    public String showForStep(int step){
        log.info("showForStep {}. current is {}", step, this.activeStep);
        if (this.activeStep==step)
            return "inline";
        else
            return "none";

    }



}
