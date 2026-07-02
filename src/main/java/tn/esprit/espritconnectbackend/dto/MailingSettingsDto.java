package tn.esprit.espritconnectbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MailingSettingsDto {
    @Builder.Default
    private boolean authEmailsEnabled = true;
    @Builder.Default
    private boolean eventEmailsEnabled = true;
    @Builder.Default
    private boolean mentoringEmailsEnabled = true;
    @Builder.Default
    private boolean videoChatEmailsEnabled = true;
}
