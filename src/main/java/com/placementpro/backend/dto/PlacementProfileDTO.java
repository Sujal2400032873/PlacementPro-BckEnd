package com.placementpro.backend.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlacementProfileDTO {
    private String department;
    private String designation;
    private String collegeName;
    private String contactNumber;
}
