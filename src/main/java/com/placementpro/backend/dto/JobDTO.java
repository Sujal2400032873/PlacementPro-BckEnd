package com.placementpro.backend.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobDTO {
    private Long id;
    private String title;
    private String companyName;
    private String location;
    private String description;
    private String requirements;
    private String salary;
    private String type;
    private String status;
    private LocalDateTime postedAt;
    private Long employerId;
}
