package tn.esprit.espritconnectbackend.service;

import tn.esprit.espritconnectbackend.dto.PostReactionDTO;
import java.util.UUID;

public interface PostReactionService {

    PostReactionDTO reactToPost(UUID postId, String reactionType);

    void removeReaction(UUID postId);

    PostReactionDTO getMyReaction(UUID postId);
}