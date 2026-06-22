package tn.esprit.espritconnectbackend.dto;

import java.util.List;

public class MentoringPreferencesDto {
    private boolean showOfferHelp = true;
    private boolean showSeekHelp = true;
    private boolean showOfferMentoring = true;
    private boolean showSeekMentoring = true;

    private List<String> offerHelpOptions;
    private List<String> seekHelpOptions;
    private List<String> offerMentorOptions;
    private List<String> seekMentorOptions;

    public MentoringPreferencesDto() {}

    public boolean isShowOfferHelp() { return showOfferHelp; }
    public void setShowOfferHelp(boolean showOfferHelp) { this.showOfferHelp = showOfferHelp; }

    public boolean isShowSeekHelp() { return showSeekHelp; }
    public void setShowSeekHelp(boolean showSeekHelp) { this.showSeekHelp = showSeekHelp; }

    public boolean isShowOfferMentoring() { return showOfferMentoring; }
    public void setShowOfferMentoring(boolean showOfferMentoring) { this.showOfferMentoring = showOfferMentoring; }

    public boolean isShowSeekMentoring() { return showSeekMentoring; }
    public void setShowSeekMentoring(boolean showSeekMentoring) { this.showSeekMentoring = showSeekMentoring; }

    public List<String> getOfferHelpOptions() { return offerHelpOptions; }
    public void setOfferHelpOptions(List<String> offerHelpOptions) { this.offerHelpOptions = offerHelpOptions; }

    public List<String> getSeekHelpOptions() { return seekHelpOptions; }
    public void setSeekHelpOptions(List<String> seekHelpOptions) { this.seekHelpOptions = seekHelpOptions; }

    public List<String> getOfferMentorOptions() { return offerMentorOptions; }
    public void setOfferMentorOptions(List<String> offerMentorOptions) { this.offerMentorOptions = offerMentorOptions; }

    public List<String> getSeekMentorOptions() { return seekMentorOptions; }
    public void setSeekMentorOptions(List<String> seekMentorOptions) { this.seekMentorOptions = seekMentorOptions; }
}
