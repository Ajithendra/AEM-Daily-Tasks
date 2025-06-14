package com.task01.core.eventhandler;

//Page Replication EventHandler with changed Boolean true Property

import com.day.cq.replication.ReplicationAction;
import com.day.cq.replication.ReplicationActionType;
import org.apache.sling.api.resource.ModifiableValueMap;
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
public class PageReplicationEventHandlerBooleanProperty implements EventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(PageReplicationEventHandlerBooleanProperty.class);
    private static final String CQ_PAGE_TYPE = "cq:Page";
    private static final String JCR_CONTENT = "jcr:content";
    private static final String PROPERTY_CHANGED = "changed";
    private static final Boolean PROPERTY_VALUE = Boolean.TRUE;

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Override
    public void handleEvent(Event event) {
        try (ResourceResolver resourceResolver = getServiceResourceResolver()) {
            ReplicationAction action = ReplicationAction.fromEvent(event);
            if (action != null && isValidPageReplication(action, resourceResolver)) {
                String path = action.getPath();
                LOGGER.info("Page published: {}", path);
                addChangedProperty(path, resourceResolver);
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

    private void addChangedProperty(String pagePath, ResourceResolver resourceResolver) {
        try {
            String contentPath = pagePath + "/" + JCR_CONTENT;
            Resource contentResource = resourceResolver.getResource(contentPath);
            if (contentResource != null) {
                ModifiableValueMap properties = contentResource.adaptTo(ModifiableValueMap.class);
                if (properties != null) {
                    properties.put(PROPERTY_CHANGED, PROPERTY_VALUE);
                    resourceResolver.commit();
                    LOGGER.info("Added boolean property {}={} to {}", PROPERTY_CHANGED, PROPERTY_VALUE, contentPath);
                } else {
                    LOGGER.error("Could not adapt resource to ModifiableValueMap: {}", contentPath);
                }
            } else {
                LOGGER.error("jcr:content resource not found for path: {}", contentPath);
            }
        } catch (Exception e) {
            LOGGER.error("Error adding boolean property {} to {}: {}", PROPERTY_CHANGED, pagePath, e.getMessage(), e);
        }
    }

    private ResourceResolver getServiceResourceResolver() throws Exception {
        Map<String, Object> param = new HashMap<>();
        param.put(ResourceResolverFactory.SUBSERVICE, "eventHandlerServiceBoolean");
        return resourceResolverFactory.getServiceResourceResolver(param);
    }
}