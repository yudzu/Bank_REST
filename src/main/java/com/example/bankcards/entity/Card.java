package com.example.bankcards.entity;


import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "cards")
@Data
public class Card implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "card_id_generator")
    @SequenceGenerator(name = "card_id_generator", sequenceName = "sq_card_id", allocationSize = 1)
    private Long id;

    private String encryptedNumber;

    @ManyToOne
    private User user;

    private LocalDate expirationDate;

    @Enumerated(value = EnumType.STRING)
    private CardStatus status;

    private BigDecimal balance;
}
