package tn.esprit.espritconnectbackend.dto;

import java.util.List;

public class HomepageSettingsDto {
    private boolean displayBanner = true;
    private String primaryColor = "#ed1c24";
    private String bannerImageUrl = "";
    private List<String> webTiles = List.of();
    private List<String> mobileTiles = List.of();

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

    public List<String> getWebTiles() {
        return webTiles;
    }

    public void setWebTiles(List<String> webTiles) {
        this.webTiles = webTiles;
    }

    public List<String> getMobileTiles() {
        return mobileTiles;
    }

    public void setMobileTiles(List<String> mobileTiles) {
        this.mobileTiles = mobileTiles;
    }
}
