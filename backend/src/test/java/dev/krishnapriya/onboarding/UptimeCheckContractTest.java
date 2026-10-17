package dev.krishnapriya.onboarding;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Google's uptime check looks for exact text at an exact path. If either drifted from what the
 * app really answers (say, pretty-printed JSON), every check would fail and the alert would page
 * for an app that is fine. This asks the real app the same question the check does.
 */
@SpringBootTest
@AutoConfigureMockMvc
class UptimeCheckContractTest {

    @Autowired
    MockMvc mvc;

    @Test
    void theAppAnswersWhatTheUptimeCheckExpects() throws Exception {
        JsonNode check = JsonMapper.builder().build()
                .readTree(Files.readString(Path.of("../infra/uptime/health.json")));
        String path = check.get("httpCheck").get("path").asString();
        String expected = check.get("contentMatchers").get(0).get("content").asString();

        mvc.perform(get(path))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().string(containsString(expected)));
    }
}
