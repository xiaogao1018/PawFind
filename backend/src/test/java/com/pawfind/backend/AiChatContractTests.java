package com.pawfind.backend;

import com.pawfind.backend.common.exception.GlobalExceptionHandler;
import com.pawfind.backend.common.exception.InvalidCredentialsException;
import com.pawfind.backend.controller.AiChatController;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AiChatContractTests {
    @Test
    void unavailableModelReturnsJsonEvenWhenClientRequestsEventStream() throws Exception {
        var mvc = MockMvcBuilders.standaloneSetup(new AiChatController())
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        mvc.perform(post("/api/ai/chat/stream").accept(MediaType.TEXT_EVENT_STREAM)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"messages\":[]}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    void authenticationErrorKeepsItsStatusForEventStreamClients() throws Exception {
        var mvc = MockMvcBuilders.standaloneSetup(new AuthenticationFailure())
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        mvc.perform(get("/authentication-failure").accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @RestController
    static class AuthenticationFailure {
        @GetMapping("/authentication-failure")
        void fail() {
            throw new InvalidCredentialsException("请重新登录");
        }
    }
}
