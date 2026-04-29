package com.openclassrooms.etudiant.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openclassrooms.etudiant.dto.LoginRequestDTO;
import com.openclassrooms.etudiant.dto.RegisterDTO;
import com.openclassrooms.etudiant.dto.StudentRequestDTO;
import com.openclassrooms.etudiant.repository.StudentRepository;
import com.openclassrooms.etudiant.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
class StudentControllerTest {

    private static final String REGISTER_URL = "/api/register";
    private static final String LOGIN_URL = "/api/login";
    private static final String STUDENTS_URL = "/api/students";

    @Container
    static MySQLContainer<?> mySQLContainer = new MySQLContainer<>("mysql:latest");

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private StudentRepository studentRepository;

    private String jwtToken;

    @DynamicPropertySource
    static void configureTestProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mySQLContainer::getJdbcUrl);
        registry.add("spring.datasource.username", mySQLContainer::getUsername);
        registry.add("spring.datasource.password", mySQLContainer::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create");
    }

    @BeforeEach
    void setUp() throws Exception {
        jwtToken = registerAndLoginDefaultUser();
    }

    @AfterEach
    void tearDown() {
        studentRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void get_all_students_without_token_returns_unauthorized() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get(STUDENTS_URL)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isUnauthorized());
    }

    @Test
    void create_student_returns_created_student() throws Exception {
        StudentRequestDTO requestDTO = createStudentRequest("John", "Doe", "john.doe@test.com");

        mockMvc.perform(MockMvcRequestBuilders.post(STUDENTS_URL)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken())
                        .content(objectMapper.writeValueAsString(requestDTO))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").isNumber())
                .andExpect(MockMvcResultMatchers.jsonPath("$.firstName", is("John")))
                .andExpect(MockMvcResultMatchers.jsonPath("$.lastName", is("Doe")))
                .andExpect(MockMvcResultMatchers.jsonPath("$.email", is("john.doe@test.com")));
    }

    @Test
    void create_student_with_invalid_payload_returns_bad_request() throws Exception {
        StudentRequestDTO requestDTO = createStudentRequest("", "Doe", "invalid-email");

        mockMvc.perform(MockMvcRequestBuilders.post(STUDENTS_URL)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken())
                        .content(objectMapper.writeValueAsString(requestDTO))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isBadRequest());
    }

    @Test
    void create_student_with_duplicate_email_returns_bad_request() throws Exception {
        StudentRequestDTO requestDTO = createStudentRequest("John", "Doe", "john.doe@test.com");

        mockMvc.perform(MockMvcRequestBuilders.post(STUDENTS_URL)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken())
                        .content(objectMapper.writeValueAsString(requestDTO))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isCreated());

        mockMvc.perform(MockMvcRequestBuilders.post(STUDENTS_URL)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken())
                        .content(objectMapper.writeValueAsString(requestDTO))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message", is("Student with email john.doe@test.com already exists")));
    }

    @Test
    void get_all_students_returns_created_students() throws Exception {
        createStudentThroughApi("John", "Doe", "john.doe@test.com");
        createStudentThroughApi("Jane", "Smith", "jane.smith@test.com");

        mockMvc.perform(MockMvcRequestBuilders.get(STUDENTS_URL)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken())
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$", hasSize(2)));
    }

    @Test
    void get_student_by_id_returns_student_details() throws Exception {
        Long studentId = createStudentThroughApi("John", "Doe", "john.doe@test.com");

        mockMvc.perform(MockMvcRequestBuilders.get(STUDENTS_URL + "/" + studentId)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken())
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.id", is(studentId.intValue())))
                .andExpect(MockMvcResultMatchers.jsonPath("$.email", is("john.doe@test.com")));
    }

    @Test
    void get_student_by_id_unknown_student_returns_not_found() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get(STUDENTS_URL + "/999")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken())
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    @Test
    void update_student_returns_updated_student() throws Exception {
        Long studentId = createStudentThroughApi("John", "Doe", "john.doe@test.com");
        StudentRequestDTO requestDTO = createStudentRequest("Johnny", "Doe", "johnny.doe@test.com");

        mockMvc.perform(MockMvcRequestBuilders.put(STUDENTS_URL + "/" + studentId)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken())
                        .content(objectMapper.writeValueAsString(requestDTO))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.id", is(studentId.intValue())))
                .andExpect(MockMvcResultMatchers.jsonPath("$.firstName", is("Johnny")))
                .andExpect(MockMvcResultMatchers.jsonPath("$.email", is("johnny.doe@test.com")));
    }

    @Test
    void update_student_with_duplicate_email_returns_bad_request() throws Exception {
        Long studentId = createStudentThroughApi("John", "Doe", "john.doe@test.com");
        createStudentThroughApi("Jane", "Smith", "jane.smith@test.com");
        StudentRequestDTO requestDTO = createStudentRequest("John", "Doe", "jane.smith@test.com");

        mockMvc.perform(MockMvcRequestBuilders.put(STUDENTS_URL + "/" + studentId)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken())
                        .content(objectMapper.writeValueAsString(requestDTO))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message", is("Student with email jane.smith@test.com already exists")));
    }

    @Test
    void delete_student_returns_no_content() throws Exception {
        Long studentId = createStudentThroughApi("John", "Doe", "john.doe@test.com");

        mockMvc.perform(MockMvcRequestBuilders.delete(STUDENTS_URL + "/" + studentId)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken()))
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isNoContent());

        mockMvc.perform(MockMvcRequestBuilders.get(STUDENTS_URL + "/" + studentId)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    private Long createStudentThroughApi(String firstName, String lastName, String email) throws Exception {
        StudentRequestDTO requestDTO = createStudentRequest(firstName, lastName, email);

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post(STUDENTS_URL)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken())
                        .content(objectMapper.writeValueAsString(requestDTO))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private String registerAndLoginDefaultUser() throws Exception {
        RegisterDTO registerDTO = new RegisterDTO();
        registerDTO.setFirstName("Agent");
        registerDTO.setLastName("Test");
        registerDTO.setLogin("agent");
        registerDTO.setPassword("password");

        mockMvc.perform(MockMvcRequestBuilders.post(REGISTER_URL)
                        .content(objectMapper.writeValueAsString(registerDTO))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isCreated());

        LoginRequestDTO loginRequestDTO = new LoginRequestDTO();
        loginRequestDTO.setLogin("agent");
        loginRequestDTO.setPassword("password");

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post(LOGIN_URL)
                        .content(objectMapper.writeValueAsString(loginRequestDTO))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.TEXT_PLAIN))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn();

        return result.getResponse().getContentAsString();
    }

    private String bearerToken() {
        return "Bearer " + jwtToken;
    }

    private StudentRequestDTO createStudentRequest(String firstName, String lastName, String email) {
        StudentRequestDTO requestDTO = new StudentRequestDTO();
        requestDTO.setFirstName(firstName);
        requestDTO.setLastName(lastName);
        requestDTO.setEmail(email);
        return requestDTO;
    }
}
