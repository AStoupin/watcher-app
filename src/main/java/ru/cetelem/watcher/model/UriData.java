package ru.cetelem.watcher.model;

import lombok.Builder;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.cetelem.watcher.web.RouteListController;


@Data
@Builder
public class UriData {
    private static final Logger log = LoggerFactory.getLogger(UriData.class);

    private String uriDirection="to";

    private String uriType="local";
    private String serverName = "";
    private String accountName = "";
    private String accountPassword = "";
    private String folder = "";
    private String fileMask = "";

    private String transferType = "moveAndDelete";

    private String flagProcess = "fileOnly";


    private String move = ".done";
    private String moveFailed = ".error";

    private String delay = "10";

    private String doneFileName = ".ok";
}
