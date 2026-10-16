package dev.krishnapriya.onboarding.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Wiring and HTTP contract. Business rules are covered by the plain-Java tests in domain/ and markets/. */
@SpringBootTest(properties = "markets.enabled=SE,DK")
@AutoConfigureMockMvc
class MarketApiTest {

    @Autowired
    MockMvc mvc;

    @Test
    void listsEnabledMarkets() throws Exception {
        mvc.perform(get("/api/markets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("SE"))
                .andExpect(jsonPath("$[1].code").value("DK"));
    }

    @Test
    void servesDenmarkOnboardingConfig() throws Exception {
        mvc.perform(get("/api/markets/dk/onboarding"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currency").value("DKK"))
                .andExpect(jsonPath("$.identityProvider.displayName").value("MitID"))
                .andExpect(jsonPath("$.nationalId.label").value("CPR number"));
    }

    @Test
    void unknownMarketIs404() throws Exception {
        mvc.perform(get("/api/markets/NO/onboarding")).andExpect(status().isNotFound());
    }

    /** Same error shape as our own 404s, and the request body (which may hold an ID number) is not echoed back. */
    @Test
    void malformedJsonIs400ProblemDetail() throws Exception {
        mvc.perform(post("/api/markets/DK/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nationalId\":\"121285-1234\""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(content().string(not(containsString("121285"))));
    }

    @Test
    void invalidApplicationIs422WithFieldErrors() throws Exception {
        mvc.perform(post("/api/markets/SE/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nationalId\":\"123\"}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.accepted").value(false))
                .andExpect(jsonPath("$.errors[0].field").value("identity"));
    }
}
