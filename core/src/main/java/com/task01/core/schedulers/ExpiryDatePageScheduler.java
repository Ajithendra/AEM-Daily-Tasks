package com.task01.core.schedulers;

import com.day.cq.replication.ReplicationActionType;
import com.day.cq.replication.ReplicationException;
import com.day.cq.replication.Replicator;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.commons.scheduler.ScheduleOptions;
import org.apache.sling.commons.scheduler.Scheduler;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.query.Query;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TimeZone;

@Component(
    service = Runnable.class,
    immediate = true
)
public class ExpiryDatePageScheduler implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExpiryDatePageScheduler.class);
    private static final String SERVICE_USER = "workflow-service-user";
    private static final String EXPIRY_DATE_PROPERTY = "expiryDate";

    @Reference
    private Scheduler scheduler;

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Reference
    private Replicator replicator;

    @Override
    public void run() {
        LOGGER.info("ExpiryDatePageScheduler triggered at: {}", Calendar.getInstance().getTime());
        processPages();
    }

    public void activate() {
        ScheduleOptions options = scheduler.EXPR("0/5 * * * * ?"); // Every 5 seconds
        options.name("ExpiryDatePageScheduler");
        scheduler.schedule(this, options);
        LOGGER.info("ExpiryDatePageScheduler scheduled with expression: 0/5 * * * * ?");
    }

    public void deactivate() {
        scheduler.unschedule("ExpiryDatePageScheduler");
        LOGGER.info("ExpiryDatePageScheduler unscheduled");
    }

    private void processPages() {
        ResourceResolver resourceResolver = null;
        try {
            Map<String, Object> authInfo = new HashMap<>();
            authInfo.put(ResourceResolverFactory.SUBSERVICE, SERVICE_USER);
            resourceResolver = resourceResolverFactory.getServiceResourceResolver(authInfo);

            PageManager pageManager = resourceResolver.adaptTo(PageManager.class);
            if (pageManager == null) {
                LOGGER.error("Unable to adapt ResourceResolver to PageManager");
                return;
            }

            // Define current and tomorrow dates
            Calendar currentDateTime = Calendar.getInstance(TimeZone.getTimeZone("IST"));
            Calendar tomorrowDateTime = Calendar.getInstance(TimeZone.getTimeZone("IST"));
            tomorrowDateTime.add(Calendar.DAY_OF_MONTH, 1);

            // Query for pages with expiryDate property under /content
            String query = "SELECT * FROM [cq:PageContent] AS s WHERE ISDESCENDANTNODE(s, '/content') AND s.[expiryDate] IS NOT NULL";
            Iterator<Resource> results = resourceResolver.findResources(query, Query.JCR_SQL2);

            // Use a while loop to iterate over the Iterator
            while (results.hasNext()) {
                Resource resource = results.next();
                ValueMap properties = resource.adaptTo(ValueMap.class);
                if (properties == null) {
                    LOGGER.warn("Could not adapt resource to ValueMap: {}", resource.getPath());
                    continue;
                }

                Calendar expiryDate = properties.get(EXPIRY_DATE_PROPERTY, Calendar.class);
                if (expiryDate == null) {
                    LOGGER.warn("No expiryDate found for resource: {}", resource.getPath());
                    continue;
                }

                String pagePath = resource.getPath().replace("/jcr:content", "");
                Page page = pageManager.getPage(pagePath);
                if (page == null) {
                    LOGGER.warn("No page found at path: {}", pagePath);
                    continue;
                }

                // Compare expiryDate with current and tomorrow dates
                if (expiryDate.after(currentDateTime) && expiryDate.before(tomorrowDateTime)) {
                    // Publish the page
                    publishPage(pagePath, resourceResolver);
                    LOGGER.info("Published page: {} with expiryDate: {}", pagePath, expiryDate.getTime());
                } else if (expiryDate.before(currentDateTime)) {
                    // Unpublish the page
                    unpublishPage(pagePath, resourceResolver);
                    LOGGER.info("Unpublished page: {} with expiryDate: {}", pagePath, expiryDate.getTime());
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error in ExpiryDatePageScheduler: {}", e.getMessage(), e);
        } finally {
            if (resourceResolver != null && resourceResolver.isLive()) {
                resourceResolver.close();
            }
        }
    }

    private void publishPage(String pagePath, ResourceResolver resourceResolver) {
        try {
            replicator.replicate(resourceResolver.adaptTo(javax.jcr.Session.class), 
                ReplicationActionType.ACTIVATE, pagePath);
        } catch (ReplicationException e) {
            LOGGER.error("Failed to publish page {}: {}", pagePath, e.getMessage(), e);
        }
    }

    private void unpublishPage(String pagePath, ResourceResolver resourceResolver) {
        try {
            replicator.replicate(resourceResolver.adaptTo(javax.jcr.Session.class), 
                ReplicationActionType.DEACTIVATE, pagePath);
        } catch (ReplicationException e) {
            LOGGER.error("Failed to unpublish page {}: {}", pagePath, e.getMessage(), e);
        }
    }
}