package com.task01.core.models;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.injectorspecific.ChildResource;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;
import java.util.Collections;
import java.util.List;

@Model(
    adaptables = Resource.class,
    defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL,
    resourceType = "task01/components/task3-sidebarcomponent"
)
public class Sidebar {

    // Tab 1: Header
    @ValueMapValue
    private String logoPath;

    @ValueMapValue
    private String logoMobileImage;

    @ValueMapValue
    private String logoLink;

    @ValueMapValue
    private boolean enableSwitch;

    // Tab 2: Header Links (Multifield)
    @ChildResource
    private List<HeaderLink> headerLinks;

    // Tab 3: Sidebar Navigation (Multifield with Nested Multifield)
    @ChildResource
    private List<NavigationItem> sidebarNavigation;

    // Tab 4: Region
    @ValueMapValue
    private String country;

    @ValueMapValue
    private String info;

    // Getters
    public String getLogoPath() {
        return logoPath;
    }

    public String getLogoMobileImage() {
        return logoMobileImage;
    }

    public String getLogoLink() {
        return logoLink;
    }

    public boolean isEnableSwitch() {
        return enableSwitch;
    }

    public List<HeaderLink> getHeaderLinks() {
        return headerLinks != null ? headerLinks : Collections.emptyList();
    }

    public List<NavigationItem> getSidebarNavigation() {
        return sidebarNavigation != null ? sidebarNavigation : Collections.emptyList();
    }

    public String getCountry() {
        return country;
    }

    public String getInfo() {
        return info;
    }

    // Inner class for Header Links (Tab 2)
    @Model(
        adaptables = Resource.class,
        defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL
    )
    public static class HeaderLink {
        @ValueMapValue
        private String name;

        @ValueMapValue
        private String image;

        public String getName() {
            return name;
        }

        public String getImage() {
            return image;
        }
    }

    // Inner class for Sidebar Navigation (Tab 3)
    @Model(
        adaptables = Resource.class,
        defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL
    )
    public static class NavigationItem {
        @ValueMapValue
        private String desktopIcon;

        @ValueMapValue
        private String mobileIcon;

        @ChildResource
        private List<NavigationUrl> navigationUrls;

        public String getDesktopIcon() {
            return desktopIcon;
        }

        public String getMobileIcon() {
            return mobileIcon;
        }

        public List<NavigationUrl> getNavigationUrls() {
            return navigationUrls != null ? navigationUrls : Collections.emptyList();
        }
    }

    // Inner class for Nested Multifield in Sidebar Navigation
    @Model(
        adaptables = Resource.class,
        defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL
    )
    public static class NavigationUrl {
        @ValueMapValue
        private String navigationUrl;

        public String getNavigationUrl() {
            return navigationUrl;
        }
    }
}