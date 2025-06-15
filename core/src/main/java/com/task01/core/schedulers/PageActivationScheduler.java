package com.task01.core.schedulers;
//Scheduler For activating the child pages based on the parent page provided in the configuration
import com.day.cq.replication.ReplicationActionType;
import com.day.cq.replication.Replicator;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.scheduler.ScheduleOptions;
import org.apache.sling.commons.scheduler.Scheduler;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Session;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@Component(service = Runnable.class, immediate = true)
@Designate(ocd = PageActivationScheduler.Configuration.class)
public class PageActivationScheduler implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(PageActivationScheduler.class);
    private static final String SERVICE_USER = "scheduler-service-user";

    @Reference
    private Scheduler scheduler;

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Reference
    private Replicator replicator;

    private String cronExpression;
    private String contentPath;
    private String schedulerName;

    @ObjectClassDefinition(name = "Page Activation Scheduler Configuration",
            description = "Configuration for scheduling page activation")
    public @interface Configuration {
        @AttributeDefinition(name = "Cron Expression",
                description = "Cron expression for scheduling")
        String cronExpression() default "*/5 * * * * ?"; // Default: For Every 5 seconds

        @AttributeDefinition(name = "Content Path",
                description = "Root path to check for child pages")
        String contentPath() default "/content";

        @AttributeDefinition(name = "Scheduler Name",
                description = "Unique name for the scheduler")
        String schedulerName() default "page-activation-scheduler";
    }

    @Activate
    @Modified
    protected void activate(Configuration config) {
        removeScheduler();
        this.cronExpression = config.cronExpression();
        this.contentPath = config.contentPath();
        this.schedulerName = config.schedulerName();
        addScheduler();
        LOGGER.info("Scheduler activated with cron: {}, path: {}", cronExpression, contentPath);
    }

    @Deactivate
    protected void deactivate() {
        removeScheduler();
        LOGGER.info("Scheduler deactivated");
    }

    private void addScheduler() {
        ScheduleOptions options = scheduler.EXPR(cronExpression);
        options.name(schedulerName);
        options.canRunConcurrently(false);
        scheduler.schedule(this, options);
        LOGGER.info("Scheduler added with name: {}", schedulerName);
    }

    private void removeScheduler() {
        scheduler.unschedule(schedulerName);
        LOGGER.info("Scheduler removed with name: {}", schedulerName);
    }

    @Override
    public void run() {
        LOGGER.info("Starting page activation for path: {}", contentPath);
        ResourceResolver resourceResolver = null;
        try {
            Map<String, Object> authInfo = new HashMap<>();
            authInfo.put(ResourceResolverFactory.SUBSERVICE, SERVICE_USER);
            resourceResolver = resourceResolverFactory.getServiceResourceResolver(authInfo);

            Resource resource = resourceResolver.getResource(contentPath);
            if (resource == null) {
                LOGGER.error("No resource found at path: {}", contentPath);
                return;
            }

            PageManager pageManager = resourceResolver.adaptTo(PageManager.class);
            Page rootPage = pageManager.getContainingPage(resource);
            if (rootPage == null) {
                LOGGER.error("No page found at path: {}", contentPath);
                return;
            }

            Iterator<Page> childPages = rootPage.listChildren();
            Session session = resourceResolver.adaptTo(Session.class);

            while (childPages.hasNext()) {
                Page childPage = childPages.next();
                String pagePath = childPage.getPath();
                try {
                    replicator.replicate(session, ReplicationActionType.ACTIVATE, pagePath);
                    LOGGER.info("Successfully activated page: {}", pagePath);
                } catch (Exception e) {
                    LOGGER.error("Failed to activate page: {} - Error: {}", pagePath, e.getMessage());
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error in scheduler execution: {}", e.getMessage(), e);
        } finally {
            if (resourceResolver != null && resourceResolver.isLive()) {
                resourceResolver.close();
            }
        }
        LOGGER.info("Completed page activation for path: {}", contentPath);
    }
}