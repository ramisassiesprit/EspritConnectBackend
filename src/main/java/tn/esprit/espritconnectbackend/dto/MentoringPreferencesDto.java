package tn.esprit.espritconnectbackend.dto;

public class MentoringPreferencesDto {
    private boolean showOfferHelp = true;
    private boolean showSeekHelp = true;
    private boolean showOfferMentoring = true;
    private boolean showSeekMentoring = true;

    public MentoringPreferencesDto() {}

    public boolean isShowOfferHelp() { return showOfferHelp; }
    public void setShowOfferHelp(boolean showOfferHelp) { this.showOfferHelp = showOfferHelp; }

    public boolean isShowSeekHelp() { return showSeekHelp; }
    public void setShowSeekHelp(boolean showSeekHelp) { this.showSeekHelp = showSeekHelp; }

    public boolean isShowOfferMentoring() { return showOfferMentoring; }
    public void setShowOfferMentoring(boolean showOfferMentoring) { this.showOfferMentoring = showOfferMentoring; }

    public boolean isShowSeekMentoring() { return showSeekMentoring; }
    public void setShowSeekMentoring(boolean showSeekMentoring) { this.showSeekMentoring = showSeekMentoring; }
}
