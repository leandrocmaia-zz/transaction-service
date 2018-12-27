package com.leomaya.transaction.controller;

import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.hamcrest.Matchers.equalTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
public class TransactionControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    public void should_create_transaction_endpoint() throws Exception {
        JSONObject request = new JSONObject();
        request.put("amount", 500.00);
        request.put("timestamp", Instant.now().minusSeconds(61).toEpochMilli()); // not interfere with next test scenario
        mockMvc.perform(
            post("/transaction")
            .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
            .content(request.toString())
        ).andExpect(status().isOk());
    }

    @Test
    public void should_get_statistics() throws Exception {
        JSONObject request = new JSONObject();
        request.put("amount", 100.00);
        request.put("timestamp", Instant.now().minusSeconds(59).toEpochMilli());

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(
                post("/transaction")
                    .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
                    .content(request.toString())
            ).andExpect(status().isOk());
        }


        mockMvc.perform(get("/statistics"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.count", equalTo(5)))
        .andExpect(jsonPath("$.max", equalTo(100.00)))
        .andExpect(jsonPath("$.min", equalTo(100.00)))
        .andExpect(jsonPath("$.avg", equalTo(100.00)))
        .andExpect(jsonPath("$.sum", equalTo(500.00)));

        // wait scheduler evict cache
        Thread.sleep(2000);

        mockMvc.perform(get("/statistics"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.count", equalTo(0)))
        .andExpect(jsonPath("$.max", equalTo(0.00)))
        .andExpect(jsonPath("$.min", equalTo(0.00)))
        .andExpect(jsonPath("$.avg", equalTo(0.00)))
        .andExpect(jsonPath("$.sum", equalTo(0.00)));



    }
}
