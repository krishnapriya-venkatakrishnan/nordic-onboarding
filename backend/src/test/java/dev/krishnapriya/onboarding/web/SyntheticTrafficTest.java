package dev.krishnapriya.onboarding.web;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Synthetic sign-ups are tagged in the log so dashboards and alerts can leave them out. */
@SpringBootTest(properties = "logging.structured.format.console=dev.krishnapriya.onboarding.logging.CloudLoggingFormat")
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class SyntheticTrafficTest {

    private final JsonMapper json = JsonMapper.builder().build();

    @Autowired
    MockMvc mvc;

    @Test
    void eventsFromTheSyntheticCheckAreTagged(CapturedOutput output) throws Exception {
        mvc.perform(post("/api/markets/SE/identity/sessions").header("X-Synthetic-Check", "true"))
                .andExpect(status().isOk());

        assertEquals("true", lastEvent(output).get("synthetic").asString());
    }

    @Test
    void realTrafficIsNotTagged(CapturedOutput output) throws Exception {
        mvc.perform(post("/api/markets/DK/identity/sessions")).andExpect(status().isOk());

        assertNull(lastEvent(output).get("synthetic"));
    }

    private JsonNode lastEvent(CapturedOutput output) {
        List<String> events = output.getOut().lines().filter(l -> l.contains("\"logger\":\"onboarding\"")).toList();
        return json.readTree(events.getLast());
    }
}
