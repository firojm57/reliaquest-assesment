package com.example.rqchallenge;

import com.example.rqchallenge.common.Constants;
import com.example.rqchallenge.employees.EmployeeController;
import com.example.rqchallenge.model.Employee;
import com.example.rqchallenge.service.api.ApiService;
import com.example.rqchallenge.service.employee.EmployeeService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


/**
 * <strong>Important Note</strong>: <br />
 * This Test covers the end-to-end testing
 * <ol>
 *     <li>Employee service is NOT MOCKED rather it is injected</li>
 *     <li>Api service is NOT MOCKED rather it is injected</li>
 *     <li>The only mocked service or class is RestTemplate, which calls the external Rest APIs
 *     from <a href="https://dummy.restapiexample.com/">https://dummy.restapiexample.com</a>
 *     </li>
 * </ol>
 * <p>Which makes it call the API via controller and execute all the service method till it reaches RestTemplate and there only the response is mocked</p>
 *
 */

@SpringBootTest
@ExtendWith({SpringExtension.class, MockitoExtension.class})
@AutoConfigureMockMvc
class RqChallengeApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private ApiService apiService;

    @Autowired
    private ObjectMapper mapper;

    @MockBean
    RestTemplate restTemplate;

    @InjectMocks
    private EmployeeController controller;

    @Value("${api.base.url}")
    private String apiBaseUrl;

    private static JsonNode allEmployeeJson;
    private static JsonNode singleEmployeeJson;
    private static List<String> empNames;

    @BeforeAll
    public static void init() {
        allEmployeeJson = TestUtils.readJson("all-emp-test-data.json");
        assertNotNull(allEmployeeJson);
        singleEmployeeJson = TestUtils.readJson("single-emp-test.json");
        assertNotNull(singleEmployeeJson);
        empNames = Arrays.asList("Paul Byrd", "Yuri Berry", "Charde Marshall", "Cedric Kelly", "Tatyana Fitzpatrick", "Brielle Williamson", "Jenette Caldwell", "Quinn Flynn", "Rhona Davidson", "Tiger Nixon");
    }

    @Test
    void getAllEmployees() throws Exception {
        mockGetAllSuccess();
        mockMvc.perform(get("/employees"))
                .andExpect(status().isOk())
                .andDo(result -> {
                    List<Employee> list = mapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {});
                    assertEquals(24, list.size());
                });
    }

    @Test
    void getEmployeesByNameSearch() throws Exception {
        mockGetAllSuccess();
        mockMvc.perform(get("/employees/search/rr"))
                .andExpect(status().isOk())
                .andDo(result -> {
                    List<Employee> list = mapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {});
                    assertEquals(3, list.size());
                });
    }

    @Test
    void getEmployeeById() throws Exception {
        mockSingleEmployee();
        mockMvc.perform(get("/employees/1"))
                .andExpect(status().isOk())
                .andDo(result -> {
                    Employee emp = mapper.readValue(result.getResponse().getContentAsString(), Employee.class);
                    assertEquals(emp.getEmployeeName(), "Tiger Nixon");
                    assertEquals(emp.getEmployeeSalary(), 320800);
                });
    }

    @Test
    void getHighestSalaryOfEmployees() throws Exception {
        mockGetAllSuccess();
        mockMvc.perform(get("/employees/highestSalary"))
                .andExpect(status().isOk())
                .andDo(result -> assertEquals("725000", result.getResponse().getContentAsString()));
    }

    @Test
    void getTopTenHighestEarningEmployeeNames() throws Exception {
        mockGetAllSuccess();
        mockMvc.perform(get("/employees/topTenHighestEarningEmployeeNames"))
                .andExpect(status().isOk())
                .andDo(result -> {
                    List<String> list = mapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {});
                    assertEquals(10, list.size());
                    assertIterableEquals(empNames, list);
                });
    }

    @Test
    void createEmployee() throws Exception {
        Map<String, Object> inputMap = new LinkedHashMap<>();
        inputMap.put("name", "Tiger Nixon");
        inputMap.put("salary", 320800);
        inputMap.put("age", 61);
        String requestBody = mapper.writeValueAsString(inputMap);
        when(restTemplate.postForEntity(apiBaseUrl + Constants.CREATE, inputMap, JsonNode.class))
                .thenReturn(ResponseEntity.ok(singleEmployeeJson));
        mockMvc.perform(post("/employees")
                        .content(requestBody)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(result -> {
                    Employee emp = mapper.readValue(result.getResponse().getContentAsString(), Employee.class);
                    assertEquals(emp.getEmployeeName(), "Tiger Nixon");
                    assertEquals(emp.getEmployeeSalary(), 320800);
                });
    }

    @Test
    void deleteEmployeeById() throws Exception {
        mockSingleEmployee();
        doNothing().when(restTemplate).delete(apiBaseUrl + Constants.DELETE + "/1");

        mockMvc.perform(delete("/employees/1"))
                .andExpect(status().isOk())
                .andDo(result -> assertEquals("Tiger Nixon", result.getResponse().getContentAsString()));
    }

    private void mockGetAllSuccess() {
        when(restTemplate.getForEntity(apiBaseUrl + Constants.EMPLOYEES, JsonNode.class))
                .thenReturn(ResponseEntity.ok(allEmployeeJson));
    }

    private void mockSingleEmployee() {
        when(restTemplate.getForEntity(apiBaseUrl + Constants.EMPLOYEES + "/1", JsonNode.class))
                .thenReturn(ResponseEntity.ok(singleEmployeeJson));
    }
}