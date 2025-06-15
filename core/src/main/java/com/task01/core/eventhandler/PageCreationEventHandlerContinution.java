package com.task01.core.eventhandler;

import com.day.cq.workflow.WorkflowException;
import com.day.cq.workflow.WorkflowService;
import com.day.cq.workflow.WorkflowSession;
import com.day.cq.workflow.exec.WorkflowData;
import com.day.cq.workflow.model.WorkflowModel;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import java.util.HashMap;
import java.util.Map;

@Component(
    service = EventHandler.class,
    immediate = true,
    property = {
        EventConstants.EVENT_TOPIC + "=org/apache/sling/api/resource/Resource/ADDED",
        EventConstants.EVENT_FILTER + "=(path=/content/*/jcr:content)"
    }
)
public class PageCreationEventHandlerContinution implements EventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(PageCreationEventHandlerContinution.class);
    private static final String SERVICE_USER = "workflow-service-user";
    private static final String WORKFLOW_MODEL_PATH = "/var/workflow/models/page-expiry-workflow-continution"; // Reverted to /conf

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Reference
    private WorkflowService workflowService;

    @Override
    public void handleEvent(Event event) {
        LOGGER.info("Event received: {}", event.getTopic());
        String path = (String) event.getProperty("path");
        LOGGER.info("Event path: {}", path);
        if (path == null || !path.startsWith("/content")) {
            LOGGER.debug("Ignoring event for path: {}", path);
            return;
        }

        ResourceResolver resourceResolver = null;
        try {
            Map<String, Object> authInfo = new HashMap<>();
            authInfo.put(ResourceResolverFactory.SUBSERVICE, SERVICE_USER);
            resourceResolver = resourceResolverFactory.getServiceResourceResolver(authInfo);

            Resource resource = resourceResolver.getResource(path);
            if (resource == null) {
                LOGGER.error("Resource not found at path: {}", path);
                return;
            }

            // Check if the parent is a cq:Page
            Resource parentResource = resource.getParent();
            if (parentResource != null) {
                Node parentNode = parentResource.adaptTo(Node.class);
                if (parentNode != null && parentNode.isNodeType("cq:Page")) {
                    String pagePath = parentResource.getPath();
                    LOGGER.info("Page created at path: {}", pagePath);
                    startWorkflow(pagePath, resourceResolver);
                } else {
                    LOGGER.debug("Parent at path {} is not a page", parentResource.getPath());
                }
            } else {
                LOGGER.debug("No parent resource for path: {}", path);
            }
        } catch (Exception e) {
            LOGGER.error("Error processing event for path {}: {}", path, e.getMessage(), e);
        } finally {
            if (resourceResolver != null && resourceResolver.isLive()) {
                resourceResolver.close();
            }
        }
    }

    private void startWorkflow(String pagePath, ResourceResolver resourceResolver) {
        try {
            LOGGER.info("Attempting to start workflow for page: {}", pagePath);
            WorkflowSession workflowSession = workflowService.getWorkflowSession(resourceResolver.adaptTo(javax.jcr.Session.class));
            WorkflowModel workflowModel = workflowSession.getModel(WORKFLOW_MODEL_PATH);
            if (workflowModel == null) {
                LOGGER.error("Workflow model not found at path: {}", WORKFLOW_MODEL_PATH);
                return;
            }
            WorkflowData workflowData = workflowSession.newWorkflowData("JCR_PATH", pagePath);
            workflowSession.startWorkflow(workflowModel, workflowData);
            LOGGER.info("Workflow successfully started for page: {}", pagePath);
        } catch (WorkflowException e) {
            LOGGER.error("Failed to start workflow for page {}: {}", pagePath, e.getMessage(), e);
        }
    }
}