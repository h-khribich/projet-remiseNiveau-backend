package com.openclassrooms.etudiant.service;

import com.openclassrooms.etudiant.dto.StudentRequestDTO;
import com.openclassrooms.etudiant.dto.StudentResponseDTO;
import com.openclassrooms.etudiant.entities.Student;
import com.openclassrooms.etudiant.mapper.StudentDtoMapper;
import com.openclassrooms.etudiant.repository.StudentRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class StudentService {

    private final StudentRepository studentRepository;
    private final StudentDtoMapper studentDtoMapper;

    public StudentResponseDTO create(StudentRequestDTO studentRequestDTO) {
        Assert.notNull(studentRequestDTO, "Student request must not be null");
        validateEmailAvailability(studentRequestDTO.getEmail(), null);

        Student student = studentDtoMapper.toEntity(studentRequestDTO);
        Student savedStudent = studentRepository.save(student);
        log.info("Created student with id {}", savedStudent.getId());
        return studentDtoMapper.toResponseDTO(savedStudent);
    }

    public List<StudentResponseDTO> getAll() {
        return studentDtoMapper.toResponseDTOList(studentRepository.findAll());
    }

    public StudentResponseDTO getById(Long id) {
        return studentDtoMapper.toResponseDTO(findStudentById(id));
    }

    public StudentResponseDTO update(Long id, StudentRequestDTO studentRequestDTO) {
        Assert.notNull(studentRequestDTO, "Student request must not be null");

        Student student = findStudentById(id);
        validateEmailAvailability(studentRequestDTO.getEmail(), id);

        student.setFirstName(studentRequestDTO.getFirstName());
        student.setLastName(studentRequestDTO.getLastName());
        student.setEmail(studentRequestDTO.getEmail());

        Student updatedStudent = studentRepository.save(student);
        log.info("Updated student with id {}", updatedStudent.getId());
        return studentDtoMapper.toResponseDTO(updatedStudent);
    }

    public void delete(Long id) {
        Student student = findStudentById(id);
        studentRepository.delete(student);
        log.info("Deleted student with id {}", id);
    }

    private Student findStudentById(Long id) {
        Assert.notNull(id, "Student id must not be null");
        return studentRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Student with id " + id + " not found"));
    }

    private void validateEmailAvailability(String email, Long studentIdToExclude) {
        studentRepository.findByEmail(email)
                .filter(student -> !student.getId().equals(studentIdToExclude))
                .ifPresent(student -> {
                    throw new IllegalArgumentException("Student with email " + email + " already exists");
                });
    }
}
