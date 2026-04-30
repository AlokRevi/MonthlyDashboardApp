package com.alok.monthlydashboard.service;

import com.alok.monthlydashboard.repository.CategoryRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:sqlite:target/category-integration-test.db",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false"
})
class CategoryIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        categoryRepository.deleteAll();
    }

    @Test
    void createsAndReadsCategoryMetadata() throws Exception {
        String request = """
                {
                  "name": "Health",
                  "color": "#16a34a",
                  "requires": "MOVEMENT",
                  "feelsLike": ["ACTIVATING", "FUN", "MILD_FUTURE_STRESS", "MEDIUM"]
                }
                """;

        String response = mockMvc.perform(post("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Health"))
                .andExpect(jsonPath("$.color").value("#16a34a"))
                .andExpect(jsonPath("$.requires").value("MOVEMENT"))
                .andExpect(jsonPath("$.feelsLike", contains(
                        "ACTIVATING",
                        "FUN",
                        "MILD_FUTURE_STRESS",
                        "MEDIUM"
                )))
                .andReturn()
                .getResponse()
                .getContentAsString();

        long categoryId = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(get("/api/v1/categories/{categoryId}", categoryId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Health"))
                .andExpect(jsonPath("$.requires").value("MOVEMENT"))
                .andExpect(jsonPath("$.feelsLike", contains(
                        "ACTIVATING",
                        "FUN",
                        "MILD_FUTURE_STRESS",
                        "MEDIUM"
                )));
    }

    @Test
    void updatesCategoryMetadata() throws Exception {
        String createRequest = """
                {
                  "name": "Learning",
                  "color": "#0891b2",
                  "requires": "FOCUS",
                  "feelsLike": ["TIRING", "OKAY", "NO_PRESSURE", "EASY"]
                }
                """;

        String createResponse = mockMvc.perform(post("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        long categoryId = objectMapper.readTree(createResponse).get("id").asLong();

        String updateRequest = """
                {
                  "name": "Outdoor",
                  "color": "#65a30d",
                  "requires": "OUTDOOR",
                  "feelsLike": ["ENERGIZING", "BLISSFUL", "URGENT_AND_IMPORTANT", "HARD"]
                }
                """;

        mockMvc.perform(put("/api/v1/categories/{categoryId}", categoryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Outdoor"))
                .andExpect(jsonPath("$.color").value("#65a30d"))
                .andExpect(jsonPath("$.requires").value("OUTDOOR"))
                .andExpect(jsonPath("$.feelsLike", contains(
                        "ENERGIZING",
                        "BLISSFUL",
                        "URGENT_AND_IMPORTANT",
                        "HARD"
                )));
    }

    @Test
    void defaultsMetadataForExistingCategoryCreatePayloads() throws Exception {
        String request = """
                {
                  "name": "Home",
                  "color": "#2563eb"
                }
                """;

        mockMvc.perform(post("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.requires").value("FOCUS"))
                .andExpect(jsonPath("$.feelsLike", empty()));
    }
}
