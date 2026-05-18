package tn.esprit.espritconnectbackend.service;

import tn.esprit.espritconnectbackend.dto.ResourceFolderDTO;
import tn.esprit.espritconnectbackend.dto.ResourceFolderDetailsDTO;
import tn.esprit.espritconnectbackend.dto.CreateResourceFolderRequest;
import org.springframework.web.multipart.MultipartFile;
import tn.esprit.espritconnectbackend.dto.ResourceFileDTO;

import java.util.List;
import java.util.UUID;

public interface ResourceService {
    List<ResourceFolderDTO> getAllFolders();
    ResourceFolderDetailsDTO getFolderDetails(UUID folderId);
    ResourceFolderDTO createFolder(CreateResourceFolderRequest request);
    ResourceFolderDTO updateFolder(UUID folderId, CreateResourceFolderRequest request);
    ResourceFolderDTO uploadFolderCover(UUID folderId, MultipartFile file);
    ResourceFileDTO uploadFile(UUID folderId, MultipartFile file);
    void deleteFolder(UUID folderId);
    void deleteFile(UUID fileId);
}
