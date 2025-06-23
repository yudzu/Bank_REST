package com.example.bankcards.service;

import com.example.bankcards.dto.CardDto;
import com.example.bankcards.dto.TransferRequest;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.CardNotFoundException;
import com.example.bankcards.exception.TransferException;
import com.example.bankcards.exception.UserNotFoundException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.util.CardEncryptionUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class CardService {
    private final CardRepository cardRepository;
    private final CardEncryptionUtil cardEncryptionUtil;
    private final UserRepository userRepository;

    public Page<CardDto> getAllCards(Pageable pageable) {
        return cardRepository.findAll(pageable)
                .map(card -> toDto(card, false));
    }

    public Page<CardDto> getUserCards(String email, Pageable pageable) {
        User user = getUserByEmail(email);
        return cardRepository.findAllByUser(user, pageable)
                .map(card -> toDto(card, true));
    }

    public CardDto getCard(String email, Long id) {
        Card card = cardRepository.findById(id)
                .orElseThrow(() -> new CardNotFoundException("Card not found with id: " + id));
        User user = getUserByEmail(email);
        if (isOwner(card, email)) {
            return toDto(card, true);
        } else if (user.getRoles().stream()
                .anyMatch(role -> role.getName().name().equals("ADMIN"))
        ) {
            return toDto(card, false);
        } else {
            throw new AccessDeniedException("You don't have permission to access this card");
        }
    }

    @Transactional
    public void createCard(Long id) {
        String newCardNumber = generateUniqueCardNumber();
        String encryptedNumber = cardEncryptionUtil.encrypt(newCardNumber);

        Card card = Card.builder()
                .user(userRepository.findById(id).orElseThrow(() -> new UserNotFoundException("User not found!")))
                .encryptedNumber(encryptedNumber)
                .balance(BigDecimal.ZERO)
                .status(CardStatus.ACTIVE)
                .expirationDate(LocalDate.now().plusYears(5))
                .build();

        cardRepository.save(card);
    }

    @Transactional
    public void deleteCard(Long id) {
        cardRepository.deleteById(id);
    }

    @Transactional
    public void blockCard(Long id) {
        updateCardStatus(id, CardStatus.BLOCKED);
    }

    @Transactional
    public void activateCard(Long id) {
        updateCardStatus(id, CardStatus.ACTIVE);
    }

    @Transactional
    public void requestCardBlock(String email, Long id) {
        Card card = cardRepository.findById(id)
                .orElseThrow(() -> new CardNotFoundException("Card not found with id: " + id));
        if (isOwner(card, email)) {
            card.setStatus(CardStatus.BLOCKED);
            cardRepository.save(card);
        } else {
            throw new AccessDeniedException("You don't have permission to access this card");
        }
    }

    @Transactional
    public void transferBetweenCards(TransferRequest transferRequest) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Long fromCardId = transferRequest.getFromCardId();
        Long toCardId = transferRequest.getToCardId();
        BigDecimal amount = transferRequest.getAmount();

        if (fromCardId.equals(toCardId)) {
            throw new TransferException("The transfer cards must be different");
        }

        Card fromCard = cardRepository.findById(fromCardId)
                .orElseThrow(() -> new CardNotFoundException("Sender card not found with id: " + fromCardId));
        Card toCard = cardRepository.findById(toCardId)
                .orElseThrow(() -> new CardNotFoundException("Receiver card not found with id: " + toCardId));

        if (!isOwner(fromCard, email) || !isOwner(toCard, email)) {
            throw new TransferException("Cannot transfer between cards that you don't own");
        }
        if (fromCard.getStatus() != CardStatus.ACTIVE || toCard.getStatus() != CardStatus.ACTIVE) {
            throw new TransferException("Both cards must be active");
        }
        if (fromCard.getBalance().compareTo(amount) < 0) {
            throw new TransferException("Insufficient balance");
        }

        fromCard.setBalance(fromCard.getBalance().subtract(amount));
        toCard.setBalance(toCard.getBalance().add(amount));

        cardRepository.save(fromCard);
        cardRepository.save(toCard);
    }

    // ===================================================================================================================
    // = Implementation
    // ===================================================================================================================

    private CardDto toDto(Card card, boolean isOwner) {
        String decryptedNumber = cardEncryptionUtil.decrypt(card.getEncryptedNumber());
        String visibleNumber = isOwner ? decryptedNumber : cardEncryptionUtil.mask(decryptedNumber);

        return CardDto.builder()
                .cardId(card.getId())
                .number(visibleNumber)
                .ownerEmail(card.getUser().getEmail())
                .balance(card.getBalance())
                .expirationDate(card.getExpirationDate())
                .status(card.getStatus())
                .build();
    }

    private String generateUniqueCardNumber() {
        String number = generateRandomCardNumber();
        while (cardRepository.existsByEncryptedNumber(cardEncryptionUtil.encrypt(number))) {
            number = generateRandomCardNumber();
        }
        return number;
    }

    private String generateRandomCardNumber() {
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 16; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }

    private void updateCardStatus(Long cardId, CardStatus status) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new CardNotFoundException("Card not found with id: " + cardId));
        card.setStatus(status);
        cardRepository.save(card);
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found!"));
    }

    private boolean isOwner(Card card, String email){
        return card.getUser().getEmail().equals(email);
    }
}
