package tn.esprit.espritconnectbackend.dto;

import java.util.List;

public class JobsSettingsDto {
    private List<String> employmentTypes = List.of(
            "Full-time",
            "Part-time",
            "Final project Internship",
            "Job offer",
            "Short Internship"
    );
    private boolean displayJobWidgetOnFeedPage = true;
    private boolean displayExternalJobListingWidgetOnJobsPage = true;
    private ExternalJobsWidgetDto externalJobsWidget = new ExternalJobsWidgetDto();
    private AutomatedFeedDto automatedFeed = new AutomatedFeedDto();

    public JobsSettingsDto() {}

    public List<String> getEmploymentTypes() {
        return employmentTypes;
    }

    public void setEmploymentTypes(List<String> employmentTypes) {
        this.employmentTypes = employmentTypes;
    }

    public boolean isDisplayJobWidgetOnFeedPage() {
        return displayJobWidgetOnFeedPage;
    }

    public void setDisplayJobWidgetOnFeedPage(boolean displayJobWidgetOnFeedPage) {
        this.displayJobWidgetOnFeedPage = displayJobWidgetOnFeedPage;
    }

    public boolean isDisplayExternalJobListingWidgetOnJobsPage() {
        return displayExternalJobListingWidgetOnJobsPage;
    }

    public void setDisplayExternalJobListingWidgetOnJobsPage(boolean displayExternalJobListingWidgetOnJobsPage) {
        this.displayExternalJobListingWidgetOnJobsPage = displayExternalJobListingWidgetOnJobsPage;
    }

    public ExternalJobsWidgetDto getExternalJobsWidget() {
        return externalJobsWidget;
    }

    public void setExternalJobsWidget(ExternalJobsWidgetDto externalJobsWidget) {
        this.externalJobsWidget = externalJobsWidget;
    }

    public AutomatedFeedDto getAutomatedFeed() {
        return automatedFeed;
    }

    public void setAutomatedFeed(AutomatedFeedDto automatedFeed) {
        this.automatedFeed = automatedFeed;
    }

    public static class ExternalJobsWidgetDto {
        private String bannerImageUrl = "";
        private String language = "en-GB";
        private String title = "";
        private String description = "";
        private String linkUrl = "";
        private String buttonText = "";

        public ExternalJobsWidgetDto() {}

        public String getBannerImageUrl() {
            return bannerImageUrl;
        }

        public void setBannerImageUrl(String bannerImageUrl) {
            this.bannerImageUrl = bannerImageUrl;
        }

        public String getLanguage() {
            return language;
        }

        public void setLanguage(String language) {
            this.language = language;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getLinkUrl() {
            return linkUrl;
        }

        public void setLinkUrl(String linkUrl) {
            this.linkUrl = linkUrl;
        }

        public String getButtonText() {
            return buttonText;
        }

        public void setButtonText(String buttonText) {
            this.buttonText = buttonText;
        }
    }

    public static class AutomatedFeedDto {
        private String provider = "handshake";
        private String feedUrl = "";

        public AutomatedFeedDto() {}

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }

        public String getFeedUrl() {
            return feedUrl;
        }

        public void setFeedUrl(String feedUrl) {
            this.feedUrl = feedUrl;
        }
    }
}
