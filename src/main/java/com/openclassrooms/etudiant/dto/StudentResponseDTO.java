package com.openclassrooms.etudiant.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentResponseDTO {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private LocalDateTime created_at;
    private LocalDateTime updated_at;
}
