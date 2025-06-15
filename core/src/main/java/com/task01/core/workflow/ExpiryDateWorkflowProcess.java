package com.task01.core.workflow;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.granite.workflow.WorkflowException;
import com.adobe.granite.workflow.WorkflowSession;
import com.adobe.granite.workflow.exec.WorkItem;
import com.adobe.granite.workflow.exec.WorkflowProcess;
import com.adobe.granite.workflow.metadata.MetaDataMap;

import java.util.Calendar;
import java.util.TimeZone;

@Component(
    service = WorkflowProcess.class,
    property = {
        "process.label=Add Expiry Date Process"
    }
)
public class ExpiryDateWorkflowProcess implements WorkflowProcess {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExpiryDateWorkflowProcess.class);
    private static final String EXPIRY_DATE_PROPERTY = "expiryDate";

    @Override
    public void execute(WorkItem workItem, WorkflowSession workflowSession, MetaDataMap args) throws WorkflowException {
        try {
            String payloadPath = workItem.getWorkflowData().getPayload().toString();
            LOGGER.info("Processing workflow for page: {}", payloadPath);

            ResourceResolver resourceResolver = workflowSession.adaptTo(ResourceResolver.class);
            if (resourceResolver == null) {
                LOGGER.error("Unable to adapt WorkflowSession to ResourceResolver");
                return;
            }

            Resource pageResource = resourceResolver.getResource(payloadPath + "/jcr:content");
            if (pageResource == null) {
                LOGGER.error("No jcr:content found for page: {}", payloadPath);
                return;
            }

            ModifiableValueMap properties = pageResource.adaptTo(ModifiableValueMap.class);
            if (properties != null) {
                Calendar tomorrow = Calendar.getInstance(TimeZone.getTimeZone("IST"));
                tomorrow.add(Calendar.DAY_OF_MONTH, 1); // Add 1 day to current date (tomorrow)
                tomorrow.set(Calendar.MILLISECOND, 0);
                properties.put(EXPIRY_DATE_PROPERTY, tomorrow);
                resourceResolver.commit();
                LOGGER.info("Added expiryDate property to page {}: {}", payloadPath, tomorrow.getTime());
            } else {
                LOGGER.error("Could not adapt resource to ModifiableValueMap for page: {}", payloadPath);
            }
        } catch (Exception e) {
            LOGGER.error("Error in workflow execution for payload {}: {}", workItem.getWorkflowData().getPayload(), e.getMessage(), e);
            throw new WorkflowException("Error setting expiry date", e);
        }
    }
}