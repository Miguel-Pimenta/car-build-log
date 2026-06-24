package com.miguelpimenta.buildlog;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * End-to-end test against a real PostgreSQL instance started by Testcontainers (wired in via {@link
 * TestcontainersConfiguration} and {@code @ServiceConnection}).
 *
 * <p>Creates a vehicle, adds a modification and a dyno result, then asserts the aggregated summary
 * - exercising controller -> service -> repository -> Postgres. Requires Docker; runs under {@code
 * mvn verify} (Failsafe), not {@code mvn test}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class VehicleApiIT {

  @Autowired MockMvc mockMvc;
  @Autowired ObjectMapper objectMapper;

  @Test
  void createVehicleAddModificationAndDynoThenSummarise() throws Exception {
    MvcResult created =
        mockMvc
            .perform(
                post("/api/v1/vehicles")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                                {"make":"Volkswagen","model":"Golf GTI","year":2016,"engineCode":"EA888","notes":"track build"}
                                """))
            .andExpect(status().isCreated())
            .andExpect(header().exists("Location"))
            .andExpect(jsonPath("$.id").exists())
            .andReturn();

    String vehicleId =
        objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText();

    mockMvc
        .perform(
            post("/api/v1/vehicles/{id}/modifications", vehicleId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                {"category":"TUNING","name":"Stage 1 remap","cost":450.00,"installedAt":"2024-03-10","mileageKmAtInstall":52000}
                                """))
        .andExpect(status().isCreated());

    mockMvc
        .perform(
            post("/api/v1/vehicles/{id}/dyno", vehicleId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                {"powerHp":290,"torqueNm":410,"measuredAt":"2024-03-11"}
                                """))
        .andExpect(status().isCreated());

    mockMvc
        .perform(get("/api/v1/vehicles/{id}/summary", vehicleId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.vehicleId").value(vehicleId))
        .andExpect(jsonPath("$.totalModifications").value(1))
        .andExpect(jsonPath("$.totalSpend").value(450.00))
        .andExpect(jsonPath("$.spendByCategory.TUNING").value(450.00))
        .andExpect(jsonPath("$.currentPowerHp").value(290))
        .andExpect(jsonPath("$.currentTorqueNm").value(410))
        .andExpect(jsonPath("$.latestDyno.powerHp").value(290))
        .andExpect(jsonPath("$.latestDyno.torqueNm").value(410));
  }
}
