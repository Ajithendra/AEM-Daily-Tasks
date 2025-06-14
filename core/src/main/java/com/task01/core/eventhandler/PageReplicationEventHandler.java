package com.task01.core.eventhandler;
//Page Replication EventHandler with changed Boolean true Property

import com.day.cq.replication.ReplicationAction;
import com.day.cq.replication.ReplicationActionType;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

@Component(
    service = EventHandler.class,
    immediate = true,
    property = {
        "event.topics=" + ReplicationAction.EVENT_TOPIC
    }
)
public class PageReplicationEventHandler implements EventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(PageReplicationEventHandler.class);
    private static final String CQ_PAGE_TYPE = "cq:Page";

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Override
    public void handleEvent(Event event) {
        try (ResourceResolver resourceResolver = getServiceResourceResolver()) {
            ReplicationAction action = ReplicationAction.fromEvent(event);
            if (action != null && isValidPageReplication(action, resourceResolver)) {
                String path = action.getPath();
                LOGGER.info("Page published: {}", path);
            }
        } catch (Exception e) {
            LOGGER.error("Error processing replication event: {}", e.getMessage(), e);
        }
    }

    private boolean isValidPageReplication(ReplicationAction action, ResourceResolver resourceResolver) {
        if (action.getType() != ReplicationActionType.ACTIVATE) {
            return false;
        }
        try {
            Resource resource = resourceResolver.getResource(action.getPath());
            return resource != null && CQ_PAGE_TYPE.equals(resource.getResourceType());
        } catch (Exception e) {
            LOGGER.error("Error checking resource type: {}", e.getMessage(), e);
            return false;
        }
    }

    private ResourceResolver getServiceResourceResolver() throws Exception {
        Map<String, Object> param = new HashMap<>();
        param.put(ResourceResolverFactory.SUBSERVICE, "eventHandlerService");
        return resourceResolverFactory.getServiceResourceResolver(param);
    }
}