package tn.esprit.espritconnectbackend.dto;

public class HomepageSettingsDto {
    private boolean displayBanner = true;
    private String primaryColor = "#ed1c24";
    private String bannerImageUrl = "";

    public HomepageSettingsDto() {}

    public boolean isDisplayBanner() {
        return displayBanner;
    }

    public void setDisplayBanner(boolean displayBanner) {
        this.displayBanner = displayBanner;
    }

    public String getPrimaryColor() {
        return primaryColor;
    }

    public void setPrimaryColor(String primaryColor) {
        this.primaryColor = primaryColor;
    }

    public String getBannerImageUrl() {
        return bannerImageUrl;
    }

    public void setBannerImageUrl(String bannerImageUrl) {
        this.bannerImageUrl = bannerImageUrl;
    }
}
