package com.task01.core.workflow;

import com.adobe.granite.workflow.WorkflowException;
import com.adobe.granite.workflow.WorkflowSession;
import com.adobe.granite.workflow.exec.WorkItem;
import com.adobe.granite.workflow.exec.WorkflowProcess;
import com.adobe.granite.workflow.metadata.MetaDataMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.ValueMap;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;
import java.util.TimeZone;

@Component(
    service = WorkflowProcess.class,
    property = {
        "process.label=Add Expiry Date Process continution"
    }
)
public class ExpiryDateWorkflowProcessContinution implements WorkflowProcess {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExpiryDateWorkflowProcessContinution.class);
    private static final String EXPIRY_DATE_PROPERTY = "expiryDate";
    private static final String CQ_TEMPLATE_PROPERTY = "cq:template";
    private static final String TASK01_TEMPLATE_PATH = "/conf/task01/settings/wcm/templates";

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

            ValueMap properties = pageResource.adaptTo(ValueMap.class);
            if (properties == null) {
                LOGGER.error("Could not adapt resource to ValueMap for page: {}", payloadPath);
                return;
            }

            // Get the cq:template property
            String cqTemplate = properties.get(CQ_TEMPLATE_PROPERTY, String.class);
            if (cqTemplate == null) {
                LOGGER.warn("No cq:template property found for page: {}", payloadPath);
                return;
            }
            LOGGER.info("cq:template value for page {}: {}", payloadPath, cqTemplate);

            ModifiableValueMap modifiableProperties = pageResource.adaptTo(ModifiableValueMap.class);
            if (modifiableProperties == null) {
                LOGGER.error("Could not adapt resource to ModifiableValueMap for page: {}", payloadPath);
                return;
            }

            // Determine the expiry date based on the template path
            Calendar expiryDate;
            boolean isTask01Template = cqTemplate != null && cqTemplate.startsWith(TASK01_TEMPLATE_PATH);
            LOGGER.info("Does cq:template {} start with {}? {}", cqTemplate, TASK01_TEMPLATE_PATH, isTask01Template);

            if (isTask01Template) {
                // Template belongs to /conf/task01/settings/wcm/templates, set to tomorrow
                expiryDate = Calendar.getInstance(TimeZone.getTimeZone("IST"));
                expiryDate.add(Calendar.DAY_OF_MONTH, 1); // Tomorrow
                expiryDate.set(Calendar.MILLISECOND, 0);
                LOGGER.info("Setting expiryDate to tomorrow for page {} with template {}: {}", payloadPath, cqTemplate, expiryDate.getTime());
            } else {
                // Template is from another project, set to previous date
                expiryDate = Calendar.getInstance(TimeZone.getTimeZone("IST"));
                expiryDate.add(Calendar.DAY_OF_MONTH, -1); // Previous day
                expiryDate.set(Calendar.MILLISECOND, 0);
                LOGGER.info("Setting expiryDate to previous date for page {} with template {}: {}", payloadPath, cqTemplate, expiryDate.getTime());
            }

            modifiableProperties.put(EXPIRY_DATE_PROPERTY, expiryDate);
            resourceResolver.commit();
            LOGGER.info("Added expiryDate property to page {}: {}", payloadPath, expiryDate.getTime());
        } catch (Exception e) {
            LOGGER.error("Error in workflow execution for payload {}: {}", workItem.getWorkflowData().getPayload(), e.getMessage(), e);
            throw new WorkflowException("Error setting expiry date", e);
        }
    }
}