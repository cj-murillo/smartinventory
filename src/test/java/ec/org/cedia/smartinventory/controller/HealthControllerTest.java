package ec.org.cedia.smartinventory.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJsonTesters;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ec.org.cedia.smartinventory.config.SecurityConfig;

@WebMvcTest(HealthController.class)
@Import(SecurityConfig.class)
@AutoConfigureJsonTesters
class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    // ════════════════════════════════════════════════════
    // TEST 1 — Health check público → 200
    // Valida: API levantada, endpoint sin auth, campos del response
    // ════════════════════════════════════════════════════
    @Test
    void health_sinAuth_debeRetornar200ConStatusUp() throws Exception {
        mockMvc.perform(get("/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UP"))
            .andExpect(jsonPath("$.service").value("SmartInventory API"))
            .andExpect(jsonPath("$.timestamp").exists());
    }
}