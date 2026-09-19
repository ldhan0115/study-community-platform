package com.study.study_community_platform.controller.advice;

import com.study.study_community_platform.exception.BusinessRuleException;
import com.study.study_community_platform.exception.ForbiddenOperationException;
import com.study.study_community_platform.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class GlobalExceptionHandlerTest {

    MockMvc mockMvc;

    @BeforeEach
    void setup(){
        mockMvc = MockMvcBuilders
                .standaloneSetup(new ExceptionTestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void resourceNotFoundExceptionReturns404() throws Exception {

        mockMvc.perform(
                        get("/test/errors/not-found")
        )
                .andExpect(status().isNotFound())
                .andExpect(view().name("error/error"))
                .andExpect(model().attribute("status", 404));
    }

    @Test
    void forbiddenOperationExceptionReturns403() throws Exception {
        
        mockMvc.perform(
                get("/test/errors/forbidden")
        )
                .andExpect(status().isForbidden())
                .andExpect(view().name("error/error"))
                .andExpect(model().attribute("status", 403));
    }

    @Test
    void businessRuleExceptionReturns400() throws Exception {

        mockMvc.perform(
                get("/test/errors/business")
        )
                .andExpect(status().isBadRequest())
                .andExpect(view().name("error/error"))
                .andExpect(model().attribute("status", 400));
    }

    @Controller
    static class ExceptionTestController{

        @GetMapping("/test/errors/not-found")
        public String notFound(){
            throw new ResourceNotFoundException("존재하지 않는 리소스입니다.");
        }

        @GetMapping("/test/errors/forbidden")
        public String forbidden(){
            throw new ForbiddenOperationException("요청 권한이 없습니다.");
        }

        @GetMapping("/test/errors/business")
        public String business(){
            throw new BusinessRuleException("처리할 수 없는 요청입니다.");
        }
    }

}