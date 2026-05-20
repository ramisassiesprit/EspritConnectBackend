package tn.esprit.espritconnectbackend.service;

import tn.esprit.espritconnectbackend.dto.TicketPostDTO;
import tn.esprit.espritconnectbackend.dto.TicketCommentDTO;
import java.util.List;
import java.util.UUID;

public interface TicketService {
    TicketPostDTO createPost(String title, String content, String category);
    List<TicketPostDTO> getAllPosts(String category, String status, String search);
    TicketPostDTO getPostById(UUID postId);
    TicketPostDTO togglePostStatus(UUID postId, String status);
    TicketPostDTO upvotePost(UUID postId);
    TicketCommentDTO addComment(UUID postId, String content);
    TicketCommentDTO upvoteComment(UUID commentId);
    TicketCommentDTO markCommentAsSolution(UUID commentId);
}
