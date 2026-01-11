package com.felixkroemer.file;

import com.felixkroemer.common.ErrorCode;
import com.felixkroemer.file.error.FileMoveStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "file_move")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Setter
public class FileMoveEntity {
    
    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    @Convert(converter = LocalDateTimeConverter.class)
    private LocalDateTime createdAt;
    
    @Column(nullable = false)
    private String sourceFileName;

    @Column(nullable = false)
    private String sourceDirectory;
    
    private String targetFileName;

    private String targetDirectory;

    @Column(name = "file_hash", length = 64)
    private String fileHash;

    @Convert(converter = LocalDateTimeConverter.class)
    private LocalDateTime moveCompletedAt;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FileMoveStatus status;

    @Enumerated(EnumType.STRING)
    private ErrorCode errorCode;
    
    private String errorMessage;
    
    @Column(nullable = false)
    private long fileSize;
}
