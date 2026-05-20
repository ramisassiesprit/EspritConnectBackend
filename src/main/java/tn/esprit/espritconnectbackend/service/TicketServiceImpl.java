package tn.esprit.espritconnectbackend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.espritconnectbackend.dto.TicketCommentDTO;
import tn.esprit.espritconnectbackend.dto.TicketPostDTO;
import tn.esprit.espritconnectbackend.dto.UserDTO;
import tn.esprit.espritconnectbackend.entities.TicketComment;
import tn.esprit.espritconnectbackend.entities.TicketPost;
import tn.esprit.espritconnectbackend.entities.User;
import tn.esprit.espritconnectbackend.repositories.TicketCommentRepository;
import tn.esprit.espritconnectbackend.repositories.TicketPostRepository;
import tn.esprit.espritconnectbackend.repositories.UserRepository;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class TicketServiceImpl implements TicketService {

    private final TicketPostRepository ticketPostRepository;
    private final TicketCommentRepository ticketCommentRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    // ── Helper: get logged-in user ─────────────────────────────────────────
    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    // ── Helper: map TicketPost → TicketPostDTO ──────────────────────────────
    private TicketPostDTO mapToDTO(TicketPost post, User currentUser) {
        TicketPostDTO dto = new TicketPostDTO();
        dto.setId(post.getId());
        dto.setTitle(post.getTitle());
        dto.setContent(post.getContent());
        dto.setCategory(post.getCategory());
        dto.setStatus(post.getStatus());
        dto.setCreatedAt(post.getCreatedAt());
        dto.setUpdatedAt(post.getUpdatedAt());
        dto.setUpvotes(post.getUpvoters().size());
        dto.setHasUpvoted(post.getUpvoters().contains(currentUser));

        UserDTO userDTO = new UserDTO();
        userDTO.setId(post.getUser().getId());
        userDTO.setFirstName(post.getUser().getFirstName());
        userDTO.setLastName(post.getUser().getLastName());
        userDTO.setEmail(post.getUser().getEmail());
        userDTO.setAvatarUrl(post.getUser().getAvatarUrl());
        userDTO.setRole(post.getUser().getRole());
        dto.setUser(userDTO);

        if (post.getComments() != null) {
            dto.setComments(post.getComments().stream()
                    .map(comment -> mapCommentToDTO(comment, currentUser))
                    .collect(Collectors.toList()));
        }
        return dto;
    }

    // ── Helper: map TicketComment → TicketCommentDTO ────────────────────────
    private TicketCommentDTO mapCommentToDTO(TicketComment comment, User currentUser) {
        TicketCommentDTO dto = new TicketCommentDTO();
        dto.setId(comment.getId());
        dto.setContent(comment.getContent());
        dto.setIsSolution(comment.getIsSolution());
        dto.setTicketPostId(comment.getTicketPost().getId());
        dto.setCreatedAt(comment.getCreatedAt());
        dto.setUpdatedAt(comment.getUpdatedAt());
        dto.setUpvotes(comment.getUpvoters().size());
        dto.setHasUpvoted(comment.getUpvoters().contains(currentUser));

        UserDTO userDTO = new UserDTO();
        userDTO.setId(comment.getUser().getId());
        userDTO.setFirstName(comment.getUser().getFirstName());
        userDTO.setLastName(comment.getUser().getLastName());
        userDTO.setEmail(comment.getUser().getEmail());
        userDTO.setAvatarUrl(comment.getUser().getAvatarUrl());
        userDTO.setRole(comment.getUser().getRole());
        dto.setUser(userDTO);

        return dto;
    }

    @Override
    public TicketPostDTO createPost(String title, String content, String category) {
        User currentUser = getCurrentUser();

        TicketPost post = TicketPost.builder()
                .title(title)
                .content(content)
                .category(category)
                .status("EN_COURS")
                .user(currentUser)
                .build();

        // The creator automatically upvotes their own problem
        post.getUpvoters().add(currentUser);

        TicketPost savedPost = ticketPostRepository.save(post);
        return mapToDTO(savedPost, currentUser);
    }

    @Override
    public List<TicketPostDTO> getAllPosts(String category, String status, String search) {
        User currentUser = getCurrentUser();
        List<TicketPost> posts;

        if (search != null && !search.trim().isEmpty()) {
            posts = ticketPostRepository.findByTitleContainingIgnoreCaseOrContentContainingIgnoreCaseOrderByCreatedAtDesc(search, search);
        } else {
            posts = ticketPostRepository.findAllByOrderByCreatedAtDesc();
        }

        // Apply filters in-memory for simpler, robust query chains
        return posts.stream()
                .filter(post -> category == null || category.equalsIgnoreCase("TOUS") || post.getCategory().equalsIgnoreCase(category))
                .filter(post -> status == null || status.equalsIgnoreCase("TOUS") || post.getStatus().equalsIgnoreCase(status))
                .map(post -> mapToDTO(post, currentUser))
                .collect(Collectors.toList());
    }

    @Override
    public TicketPostDTO getPostById(UUID postId) {
        User currentUser = getCurrentUser();
        TicketPost post = ticketPostRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Ticket SOS non trouvé"));
        return mapToDTO(post, currentUser);
    }

    @Override
    public TicketPostDTO togglePostStatus(UUID postId, String status) {
        User currentUser = getCurrentUser();
        TicketPost post = ticketPostRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Ticket SOS non trouvé"));

        post.setStatus(status);
        TicketPost savedPost = ticketPostRepository.save(post);
        return mapToDTO(savedPost, currentUser);
    }

    @Override
    public TicketPostDTO upvotePost(UUID postId) {
        User currentUser = getCurrentUser();
        TicketPost post = ticketPostRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Ticket SOS non trouvé"));

        if (post.getUpvoters().contains(currentUser)) {
            post.getUpvoters().remove(currentUser);
        } else {
            post.getUpvoters().add(currentUser);
            // Trigger Notification
            if (!post.getUser().getId().equals(currentUser.getId())) {
                String title = "Nouveau soutien sur votre SOS";
                String body = currentUser.getFirstName() + " " + currentUser.getLastName() + " a soutenu votre ticket : \"" + post.getTitle() + "\"";
                notificationService.createNotification(
                    post.getUser(),
                    title,
                    body,
                    tn.esprit.espritconnectbackend.entities.enums.NotificationType.POST_REACTION,
                    "TICKET",
                    post.getId()
                );
            }
        }

        TicketPost savedPost = ticketPostRepository.save(post);
        return mapToDTO(savedPost, currentUser);
    }

    @Override
    public TicketCommentDTO addComment(UUID postId, String content) {
        User currentUser = getCurrentUser();
        TicketPost post = ticketPostRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Ticket SOS non trouvé"));

        TicketComment comment = TicketComment.builder()
                .content(content)
                .ticketPost(post)
                .user(currentUser)
                .isSolution(false)
                .build();

        TicketComment savedComment = ticketCommentRepository.save(comment);

        // Trigger Notification
        if (!post.getUser().getId().equals(currentUser.getId())) {
            String title = "Nouvelle réponse sur votre SOS";
            String body = currentUser.getFirstName() + " " + currentUser.getLastName() + " a répondu à votre ticket : \"" + post.getTitle() + "\"";
            notificationService.createNotification(
                post.getUser(),
                title,
                body,
                tn.esprit.espritconnectbackend.entities.enums.NotificationType.POST_COMMENT,
                "TICKET",
                post.getId()
            );
        }

        return mapCommentToDTO(savedComment, currentUser);
    }

    @Override
    public TicketCommentDTO upvoteComment(UUID commentId) {
        User currentUser = getCurrentUser();
        TicketComment comment = ticketCommentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Commentaire non trouvé"));

        if (comment.getUpvoters().contains(currentUser)) {
            comment.getUpvoters().remove(currentUser);
        } else {
            comment.getUpvoters().add(currentUser);
        }

        TicketComment savedComment = ticketCommentRepository.save(comment);
        return mapCommentToDTO(savedComment, currentUser);
    }

    @Override
    public TicketCommentDTO markCommentAsSolution(UUID commentId) {
        User currentUser = getCurrentUser();
        TicketComment comment = ticketCommentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Commentaire non trouvé"));

        TicketPost post = comment.getTicketPost();

        // Verify that the currently logged-in user is the author of the Ticket
        if (!post.getUser().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Seul l'auteur du ticket peut valider cette solution !");
        }

        // Reset any other solution comment for this post
        post.getComments().forEach(c -> c.setIsSolution(false));
        ticketCommentRepository.saveAll(post.getComments());

        // Mark this comment as the solution
        comment.setIsSolution(true);
        post.setStatus("RESOLU");

        ticketPostRepository.save(post);
        TicketComment savedComment = ticketCommentRepository.save(comment);

        // Trigger Notification
        if (!comment.getUser().getId().equals(currentUser.getId())) {
            String title = "Votre solution a été acceptée !";
            String body = "L'auteur du SOS \"" + post.getTitle() + "\" a marqué votre réponse comme Meilleure Solution ! 🏆";
            notificationService.createNotification(
                comment.getUser(),
                title,
                body,
                tn.esprit.espritconnectbackend.entities.enums.NotificationType.MENTORING_ACCEPTED,
                "TICKET",
                post.getId()
            );
        }

        return mapCommentToDTO(savedComment, currentUser);
    }
}
