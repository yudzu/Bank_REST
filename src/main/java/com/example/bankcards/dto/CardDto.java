package com.example.bankcards.dto;

import com.example.bankcards.entity.CardStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class CardDto {
    private Long cardId;
    private String number;
    private String ownerEmail;
    private BigDecimal balance;
    private LocalDate expirationDate;
    private CardStatus status;
}
