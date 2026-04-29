package com.openclassrooms.etudiant.service;

import com.openclassrooms.etudiant.dto.StudentRequestDTO;
import com.openclassrooms.etudiant.dto.StudentResponseDTO;
import com.openclassrooms.etudiant.entities.Student;
import com.openclassrooms.etudiant.mapper.StudentDtoMapper;
import com.openclassrooms.etudiant.repository.StudentRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
class StudentServiceTest {

    private static final Long STUDENT_ID = 1L;
    private static final String FIRST_NAME = "John";
    private static final String LAST_NAME = "Doe";
    private static final String EMAIL = "john.doe@test.com";

    @Mock
    private StudentRepository studentRepository;
    @Mock
    private StudentDtoMapper studentDtoMapper;
    @InjectMocks
    private StudentService studentService;

    @Test
    void create_null_request_throws_illegal_argument_exception() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> studentService.create(null));
    }

    @Test
    void create_existing_email_throws_illegal_argument_exception() {
        StudentRequestDTO requestDTO = createStudentRequest(FIRST_NAME, LAST_NAME, EMAIL);
        Student existingStudent = createStudent(STUDENT_ID, FIRST_NAME, LAST_NAME, EMAIL);

        when(studentRepository.findByEmail(EMAIL)).thenReturn(Optional.of(existingStudent));

        Assertions.assertThrows(IllegalArgumentException.class, () -> studentService.create(requestDTO));
        verify(studentDtoMapper, never()).toEntity(any());
    }

    @Test
    void create_student_successfully() {
        StudentRequestDTO requestDTO = createStudentRequest(FIRST_NAME, LAST_NAME, EMAIL);
        Student studentToSave = createStudent(null, FIRST_NAME, LAST_NAME, EMAIL);
        Student savedStudent = createStudent(STUDENT_ID, FIRST_NAME, LAST_NAME, EMAIL);
        StudentResponseDTO responseDTO = createStudentResponse(STUDENT_ID, FIRST_NAME, LAST_NAME, EMAIL);

        when(studentRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
        when(studentDtoMapper.toEntity(requestDTO)).thenReturn(studentToSave);
        when(studentRepository.save(studentToSave)).thenReturn(savedStudent);
        when(studentDtoMapper.toResponseDTO(savedStudent)).thenReturn(responseDTO);

        StudentResponseDTO result = studentService.create(requestDTO);

        assertThat(result).isEqualTo(responseDTO);
        verify(studentRepository).save(studentToSave);
    }

    @Test
    void get_all_returns_mapped_students() {
        Student student = createStudent(STUDENT_ID, FIRST_NAME, LAST_NAME, EMAIL);
        StudentResponseDTO responseDTO = createStudentResponse(STUDENT_ID, FIRST_NAME, LAST_NAME, EMAIL);

        when(studentRepository.findAll()).thenReturn(List.of(student));
        when(studentDtoMapper.toResponseDTOList(List.of(student))).thenReturn(List.of(responseDTO));

        List<StudentResponseDTO> result = studentService.getAll();

        assertThat(result).containsExactly(responseDTO);
    }

    @Test
    void get_by_id_returns_student() {
        Student student = createStudent(STUDENT_ID, FIRST_NAME, LAST_NAME, EMAIL);
        StudentResponseDTO responseDTO = createStudentResponse(STUDENT_ID, FIRST_NAME, LAST_NAME, EMAIL);

        when(studentRepository.findById(STUDENT_ID)).thenReturn(Optional.of(student));
        when(studentDtoMapper.toResponseDTO(student)).thenReturn(responseDTO);

        StudentResponseDTO result = studentService.getById(STUDENT_ID);

        assertThat(result).isEqualTo(responseDTO);
    }

    @Test
    void get_by_id_unknown_student_throws_no_such_element_exception() {
        when(studentRepository.findById(STUDENT_ID)).thenReturn(Optional.empty());

        Assertions.assertThrows(NoSuchElementException.class, () -> studentService.getById(STUDENT_ID));
    }

    @Test
    void update_null_request_throws_illegal_argument_exception() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> studentService.update(STUDENT_ID, null));
    }

    @Test
    void update_unknown_student_throws_no_such_element_exception() {
        StudentRequestDTO requestDTO = createStudentRequest(FIRST_NAME, LAST_NAME, EMAIL);
        when(studentRepository.findById(STUDENT_ID)).thenReturn(Optional.empty());

        Assertions.assertThrows(NoSuchElementException.class, () -> studentService.update(STUDENT_ID, requestDTO));
    }

    @Test
    void update_with_email_used_by_another_student_throws_illegal_argument_exception() {
        StudentRequestDTO requestDTO = createStudentRequest("Jane", "Doe", "shared@test.com");
        Student student = createStudent(STUDENT_ID, FIRST_NAME, LAST_NAME, EMAIL);
        Student otherStudent = createStudent(2L, "Jack", "Smith", "shared@test.com");

        when(studentRepository.findById(STUDENT_ID)).thenReturn(Optional.of(student));
        when(studentRepository.findByEmail("shared@test.com")).thenReturn(Optional.of(otherStudent));

        Assertions.assertThrows(IllegalArgumentException.class, () -> studentService.update(STUDENT_ID, requestDTO));
        verify(studentRepository, never()).save(any());
    }

    @Test
    void update_student_successfully_when_email_belongs_to_same_student() {
        StudentRequestDTO requestDTO = createStudentRequest("Jane", "Doe", EMAIL);
        Student existingStudent = createStudent(STUDENT_ID, FIRST_NAME, LAST_NAME, EMAIL);
        StudentResponseDTO responseDTO = createStudentResponse(STUDENT_ID, "Jane", "Doe", EMAIL);

        when(studentRepository.findById(STUDENT_ID)).thenReturn(Optional.of(existingStudent));
        when(studentRepository.findByEmail(EMAIL)).thenReturn(Optional.of(existingStudent));
        when(studentRepository.save(existingStudent)).thenReturn(existingStudent);
        when(studentDtoMapper.toResponseDTO(existingStudent)).thenReturn(responseDTO);

        StudentResponseDTO result = studentService.update(STUDENT_ID, requestDTO);

        assertThat(result).isEqualTo(responseDTO);
        assertThat(existingStudent.getFirstName()).isEqualTo("Jane");
        assertThat(existingStudent.getLastName()).isEqualTo("Doe");
        assertThat(existingStudent.getEmail()).isEqualTo(EMAIL);
    }

    @Test
    void delete_existing_student() {
        Student student = createStudent(STUDENT_ID, FIRST_NAME, LAST_NAME, EMAIL);
        when(studentRepository.findById(STUDENT_ID)).thenReturn(Optional.of(student));

        studentService.delete(STUDENT_ID);

        verify(studentRepository).delete(student);
    }

    @Test
    void delete_unknown_student_throws_no_such_element_exception() {
        when(studentRepository.findById(STUDENT_ID)).thenReturn(Optional.empty());

        Assertions.assertThrows(NoSuchElementException.class, () -> studentService.delete(STUDENT_ID));
    }

    @Test
    void delete_null_id_throws_illegal_argument_exception() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> studentService.delete(null));
    }

    @Test
    void create_saves_mapped_entity_before_returning_response() {
        StudentRequestDTO requestDTO = createStudentRequest(FIRST_NAME, LAST_NAME, EMAIL);
        Student mappedStudent = createStudent(null, FIRST_NAME, LAST_NAME, EMAIL);
        Student savedStudent = createStudent(STUDENT_ID, FIRST_NAME, LAST_NAME, EMAIL);
        StudentResponseDTO responseDTO = createStudentResponse(STUDENT_ID, FIRST_NAME, LAST_NAME, EMAIL);

        when(studentRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
        when(studentDtoMapper.toEntity(requestDTO)).thenReturn(mappedStudent);
        when(studentRepository.save(mappedStudent)).thenReturn(savedStudent);
        when(studentDtoMapper.toResponseDTO(savedStudent)).thenReturn(responseDTO);

        studentService.create(requestDTO);

        ArgumentCaptor<Student> captor = ArgumentCaptor.forClass(Student.class);
        verify(studentRepository).save(captor.capture());
        assertThat(captor.getValue().getEmail()).isEqualTo(EMAIL);
    }

    private StudentRequestDTO createStudentRequest(String firstName, String lastName, String email) {
        StudentRequestDTO requestDTO = new StudentRequestDTO();
        requestDTO.setFirstName(firstName);
        requestDTO.setLastName(lastName);
        requestDTO.setEmail(email);
        return requestDTO;
    }

    private Student createStudent(Long id, String firstName, String lastName, String email) {
        Student student = new Student();
        student.setId(id);
        student.setFirstName(firstName);
        student.setLastName(lastName);
        student.setEmail(email);
        student.setCreated_at(LocalDateTime.now());
        student.setUpdated_at(LocalDateTime.now());
        return student;
    }

    private StudentResponseDTO createStudentResponse(Long id, String firstName, String lastName, String email) {
        return new StudentResponseDTO(id, firstName, lastName, email, LocalDateTime.now(), LocalDateTime.now());
    }
}
