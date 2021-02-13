package ru.cetelem.watcher.web;

import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import ru.cetelem.watcher.model.UriData;
import ru.cetelem.watcher.service.RouteDefinitionConverter;


import javax.el.ELContext;
import javax.el.ValueExpression;
import javax.faces.component.FacesComponent;
import javax.faces.component.UINamingContainer;
import java.util.StringJoiner;

//@Scope(value = "application")
//@Component(value = "uriEditorController")
@FacesComponent("uriEditorController")
@Scope(value = "view" )
public class UriEditorController  extends UINamingContainer {

    public UriData getUriData(){
        ValueExpression expr = super.getValueExpression("uri");
        ELContext ctx = super.getFacesContext().getELContext();

        return (UriData) expr.getValue(ctx);
    }

    public String showFrom(){

        if ("from".equals(getUriData().getUriDirection()))
            return "inline";
        else
            return "none";

    }

    public String showRemote(){
        if ("local".equals(getUriData().getUriType()))
            return "none";
        else
            return "inline";

    }
    public String showMove(){
        if ("moveAndStore".equals(getUriData().getTransferType())
            && "from".equals(getUriData().getUriDirection()))
            return "inline";
        else
            return "none";

    }

    public String showFlag(){
        if ("fileOnly".equals(getUriData().getFlagProcess()))
            return "none";
        else
            return "inline";

    }

    public String getUriBase(UriData uriData){
        return RouteDefinitionConverter.getUriBase(uriData);
    }

}
