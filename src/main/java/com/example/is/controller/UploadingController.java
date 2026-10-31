package com.example.is.controller;

import com.example.is.entity.UserRole;
import com.example.is.dto.upload.OpenFileMetadataDTO;
import com.example.is.exception.FileTooBigException;
import com.example.is.service.AuthHelper;
import com.example.is.service.FileStorageService;
import com.example.is.service.UploadingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/uploading")
public class UploadingController {
   private final UploadingService uploadingService;

    public UploadingController(UploadingService uploadingService) {
        this.uploadingService = uploadingService;
    }

    @PostMapping("/open")
    public OpenFileMetadataDTO openFile(@RequestParam String filename, @RequestParam long fileSize,
                                        Authentication authentication) {
        String username = authentication.getName();

        return uploadingService.openFile(filename, username, fileSize);
    }

    @ExceptionHandler(FileTooBigException.class)
    public ResponseEntity<Map<String, String>> fileTooBig(FileTooBigException exception) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(Map.of("code", "TOO_BIG"));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> invalidArgument(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ResponseEntity<?> uploadChunk(@RequestParam int id, @RequestParam MultipartFile file,
                                         @RequestParam boolean isChunk, Authentication authentication) {
        String username = authentication.getName();

        try {
            uploadingService.uploadObjectOrChunk(file, username, id, isChunk);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

}
