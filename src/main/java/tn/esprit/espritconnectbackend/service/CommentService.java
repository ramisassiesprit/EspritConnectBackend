package tn.esprit.espritconnectbackend.service;

import tn.esprit.espritconnectbackend.dto.CommentDTO;
import java.util.List;
import java.util.UUID;

public interface CommentService {

    CommentDTO addComment(UUID postId, String content, UUID parentId);

    CommentDTO updateComment(UUID commentId, String content);

    void deleteComment(UUID commentId);

    List<CommentDTO> getCommentsByPost(UUID postId);
}