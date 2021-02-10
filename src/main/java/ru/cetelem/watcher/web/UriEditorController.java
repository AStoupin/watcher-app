package ru.cetelem.watcher.web;

import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import ru.cetelem.watcher.model.UriData;

import java.util.StringJoiner;

@Scope(value = "application")
@Component(value = "uriEditorController")
public class UriEditorController {

    String routeTemplate = "<route xmlns=\"http://camel.apache.org/schema/spring\" autoStartup=\"false\" description=\"\" >\n" +
            "    <from uri=\"%s\"/>\n" +
            "    <to uri=\"%s\"/>\n" +
            "</route>";

    public String showFrom(UriData uriData){
        if ("from".equals(uriData.getUriDirection()))
            return "inline";
        else
            return "none";

    }

    public String showRemote(UriData uriData){
        if ("local".equals(uriData.getUriType()))
            return "none";
        else
            return "inline";

    }
    public String showMove(UriData uriData){
        if ("moveAndStore".equals(uriData.getTransferType())
            && "from".equals(uriData.getUriDirection()))
            return "inline";
        else
            return "none";

    }

    public String showFlag(UriData uriData){
        if ("fileOnly".equals(uriData.getFlagProcess()))
            return "none";
        else
            return "inline";

    }



    public String getUriBase(UriData uriData){
        String result = "";
        if("local".equals(uriData.getUriType())) {
            result = String.format("file://%s", uriData.getFolder());
            return result;
        }
        if("smb".equals(uriData.getUriType())) {
            result = String.format("%s://%s:%s@%s%s", uriData.getUriType(),
                    uriData.getAccountName(), uriData.getAccountPassword(),
                    uriData.getServerName(), uriData.getFolder());
            return result;
        }

        result  =  String.format("%s://%s@%s%s?password=%s", uriData.getUriType(),
                uriData.getAccountName(), uriData.getServerName(),
                uriData.getFolder(), uriData.getAccountPassword());

        return result;
    }


    public String getRouteXml(UriData uriFrom, UriData uriTo){
        String from = new UriJoiner(getUriBase(uriFrom))
                            .add(getUriFlagInfo(uriFrom))
                            .add(getUriDelay(uriFrom))
                            .add(getUriMask(uriFrom))
                            .add(getUriTrasferType(uriFrom))
                            .toString();
        String to = new UriJoiner(getUriBase(uriTo))
                            .add(getUriFlagInfo(uriTo))
                            .add(getUriDelay(uriTo))
                            .add(getUriMask(uriTo))
                            .add(getUriTrasferType(uriTo))
                            .toString();


        return  String.format(routeTemplate, from, to);
    }


    private String getUriFlagInfo(UriData uri) {
        if ("fileAndFlag".equals(uri.getFlagProcess())){
            return String.format("doneFileName=${file:name.noext}%s",uri.getDoneFileName());
        }
        return "";
    }
    private String getUriTrasferType(UriData uri) {
        if ("from".equals(uri.getUriDirection())
                && "moveAndStore".equals(uri.getTransferType())){
            return String.format("move=%s&amp;moveFailed=%s",uri.getMove(), uri.getMoveFailed());
        }
        if ("from".equals(uri.getUriDirection())
                && "moveAndDelete".equals(uri.getTransferType())){
            return String.format("delete=true");
        }
        return "";
    }
    private String getUriDelay(UriData uri) {
        if ("from".equals(uri.getUriDirection())){
            return String.format("delay=%d",Integer.parseInt(uri.getDelay()) * 1000);
        }
        return "";
    }
    private String getUriMask(UriData uri) {
        if ("from".equals(uri.getUriDirection())){
            return String.format("antInclude=%s",uri.getFileMask());
        }
        return "";
    }





    public static class UriJoiner {
            private String uri;

            public UriJoiner(String uri){
                this.uri = uri;
            }

            private UriJoiner add(String str) {
                if ("".equals(str) || str == null) {
                    return new UriJoiner(uri);
                }

                if (uri.contains("?"))
                    return new UriJoiner(uri + "&amp;" + str);
                else
                    return new UriJoiner(uri + "?" + str);
            }

        @Override
        public String toString() {
            return uri;
        }
    }
}
