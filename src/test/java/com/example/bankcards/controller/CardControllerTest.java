package com.example.bankcards.controller;

import com.example.bankcards.dto.CardDto;
import com.example.bankcards.dto.TransferRequest;
import com.example.bankcards.security.JwtAuthFilter;
import com.example.bankcards.security.JwtProvider;
import com.example.bankcards.service.CardService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Collections;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@WebMvcTest(CardController.class)
@AutoConfigureMockMvc(addFilters = false)
public class CardControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockitoBean
    private CardService cardService;
    @MockitoBean
    private JwtProvider jwtProvider;
    @MockitoBean
    private JwtAuthFilter jwtAuthFilter;

    @Test
    void getAllCards_ShouldReturnEmptyPage() throws Exception {
        Page<CardDto> emptyPage = new PageImpl<>(Collections.emptyList());
        when(cardService.getAllCards(PageRequest.of(0, 10))).thenReturn(emptyPage);

        mockMvc.perform(get("/api/cards/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").isEmpty());
    }

    @Test
    void getUserCards_ShouldReturnEmptyPage() throws Exception {
        Authentication auth = new TestingAuthenticationToken("user@example.com", null, "ROLE_USER");
        Page<CardDto> emptyPage = new PageImpl<>(Collections.emptyList());
        when(cardService.getUserCards(eq("user@example.com"), any())).thenReturn(emptyPage);

        mockMvc.perform(get("/api/cards/my-cards").principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").isEmpty());
    }

    @Test
    void getCard_ShouldReturnCardDto() throws Exception {
        Authentication auth = new TestingAuthenticationToken("user@example.com", null, "ROLE_USER");
        CardDto cardDto = CardDto.builder()
                .cardId(1L)
                .number("1234567890123456")
                .build();

        when(cardService.getCard("user@example.com", 1L)).thenReturn(cardDto);

        mockMvc.perform(get("/api/cards/1").principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cardId").value(1))
                .andExpect(jsonPath("$.number").value("1234567890123456"));
    }

    @Test
    void requestCardBlock_ShouldReturnOk() throws Exception {
        Authentication auth = new TestingAuthenticationToken("user@example.com", null, "ROLE_USER");
        mockMvc.perform(post("/api/cards/1/request-block").principal(auth))
                .andExpect(status().isOk());

        verify(cardService).requestCardBlock("user@example.com", 1L);
    }

    @Test
    void blockCardByAdmin_ShouldReturnOk() throws Exception {
        mockMvc.perform(post("/api/cards/1/block"))
                .andExpect(status().isOk());

        verify(cardService).blockCard(1L);
    }

    @Test
    void activateCardByAdmin_ShouldReturnOk() throws Exception {
        mockMvc.perform(post("/api/cards/1/activate"))
                .andExpect(status().isOk());

        verify(cardService).activateCard(1L);
    }

    @Test
    void createCard_ShouldReturnSuccessMessage() throws Exception {
        mockMvc.perform(post("/api/cards/create")
                        .param("userId", "33"))
                .andExpect(status().isOk())
                .andExpect(content().string("Card created successfully!"));

        verify(cardService).createCard(33L);
    }

    @Test
    void deleteCard_ShouldReturnOk() throws Exception {
        mockMvc.perform(delete("/api/cards/1"))
                .andExpect(status().isOk());

        verify(cardService).deleteCard(1L);
    }

    @Test
    void transferBetweenCards_ShouldReturnOk() throws Exception {
        TransferRequest transferRequest = new TransferRequest();
        transferRequest.setFromCardId(1L);
        transferRequest.setToCardId(2L);
        transferRequest.setAmount(BigDecimal.valueOf(100.0));

        mockMvc.perform(post("/api/cards/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transferRequest)))
                .andExpect(status().isOk());

        verify(cardService).transferBetweenCards(any(TransferRequest.class));
    }
}
