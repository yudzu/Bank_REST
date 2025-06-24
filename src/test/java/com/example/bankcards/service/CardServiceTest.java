package com.example.bankcards.service;

import com.example.bankcards.dto.TransferRequest;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.RoleName;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.TransferException;
import com.example.bankcards.exception.UserNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.util.CardEncryptionUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
public class CardServiceTest {
    @Mock
    private CardRepository cardRepository;
    @Mock
    private CardEncryptionUtil cardEncryptionUtil;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CardService cardService;

    private User user;
    private Card card;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .email("user@example.com")
                .roles(Set.of(new Role(1L, RoleName.USER)))
                .build();

        card = Card.builder()
                .id(1L)
                .user(user)
                .encryptedNumber("encrypted")
                .balance(BigDecimal.valueOf(100))
                .status(CardStatus.ACTIVE)
                .expirationDate(LocalDate.now().plusYears(5))
                .build();
    }

    @Test
    void createCard_ShouldSaveCardSuccessfully() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(cardEncryptionUtil.encrypt(anyString())).thenReturn("encryptedCardNumber");
        when(cardRepository.existsByEncryptedNumber(anyString())).thenReturn(false);

        cardService.createCard(1L);

        verify(cardRepository).save(any(Card.class));
    }

    @Test
    void createCard_ShouldThrow_WhenUserNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> cardService.createCard(1L));
    }

    @Test
    void blockCard_ShouldUpdateCardStatusToBlocked() {
        when(cardRepository.findById(1L)).thenReturn(Optional.of(card));

        cardService.blockCard(1L);

        assertEquals(CardStatus.BLOCKED, card.getStatus());
        verify(cardRepository).save(card);
    }

    @Test
    void activateCard_ShouldUpdateCardStatusToActive() {
        card.setStatus(CardStatus.BLOCKED);
        when(cardRepository.findById(1L)).thenReturn(Optional.of(card));

        cardService.activateCard(1L);

        assertEquals(CardStatus.ACTIVE, card.getStatus());
        verify(cardRepository).save(card);
    }

    @Test
    void requestCardBlock_ShouldBlockCardIfOwner() {
        when(cardRepository.findById(1L)).thenReturn(Optional.of(card));

        cardService.requestCardBlock("user@example.com", 1L);

        assertEquals(CardStatus.BLOCKED, card.getStatus());
        verify(cardRepository).save(card);
    }

    @Test
    void requestCardBlock_ShouldThrowAccessDenied_WhenNotOwner() {
        when(cardRepository.findById(1L)).thenReturn(Optional.of(card));

        assertThrows(AccessDeniedException.class,
                () -> cardService.requestCardBlock("other@example.com", 1L));
    }

    @Test
    void transferBetweenCards_ShouldTransferBalanceSuccessfully() {
        Card fromCard = card.toBuilder().id(1L).balance(BigDecimal.valueOf(100)).build();
        Card toCard = card.toBuilder().id(2L).balance(BigDecimal.valueOf(10)).build();

        setSecurityContext("user@example.com");

        when(cardRepository.findById(1L)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findById(2L)).thenReturn(Optional.of(toCard));

        TransferRequest transferRequest = getTransferRequest(1L, 2L, BigDecimal.valueOf(50));

        cardService.transferBetweenCards(transferRequest);

        assertEquals(BigDecimal.valueOf(50), fromCard.getBalance());
        assertEquals(BigDecimal.valueOf(60), toCard.getBalance());
        verify(cardRepository).save(fromCard);
        verify(cardRepository).save(toCard);
    }

    @Test
    void transferBetweenCards_ShouldThrow_WhenSameCardIds() {
        setSecurityContext("user@example.com");

        TransferRequest request = getTransferRequest(1L, 1L, BigDecimal.TEN);

        assertThrows(TransferException.class, () -> cardService.transferBetweenCards(request));
    }

    @Test
    void transferBetweenCards_ShouldThrow_WhenBalanceInsufficient() {
        Card fromCard = card.toBuilder().balance(BigDecimal.valueOf(10)).build();
        Card toCard = card.toBuilder().id(2L).balance(BigDecimal.valueOf(0)).build();

        setSecurityContext("user@example.com");

        when(cardRepository.findById(1L)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findById(2L)).thenReturn(Optional.of(toCard));

        TransferRequest transferRequest = getTransferRequest(1L, 2L, BigDecimal.valueOf(100));

        assertThrows(TransferException.class, () -> cardService.transferBetweenCards(transferRequest));
    }

    @Test
    void transferBetweenCards_ShouldThrow_WhenNotOwner() {
        Card fromCard = card.toBuilder().balance(BigDecimal.valueOf(100)).build();
        Card toCard = card.toBuilder().id(2L).balance(BigDecimal.valueOf(0)).user(User.builder().email("other@example.com").build()).build();

        setSecurityContext("user@example.com");

        when(cardRepository.findById(1L)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findById(2L)).thenReturn(Optional.of(toCard));

        TransferRequest transferRequest = getTransferRequest(1L, 2L, BigDecimal.valueOf(10));

        assertThrows(TransferException.class, () -> cardService.transferBetweenCards(transferRequest));
    }

    // ===================================================================================================================
    // = Implementation
    // ===================================================================================================================

    private void setSecurityContext(String email) {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn(email);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    private TransferRequest getTransferRequest(Long fromCardId, Long toCardId, BigDecimal amount){
        TransferRequest transferRequest = new TransferRequest();
        transferRequest.setFromCardId(fromCardId);
        transferRequest.setToCardId(toCardId);
        transferRequest.setAmount(amount);
        return transferRequest;
    }
}
