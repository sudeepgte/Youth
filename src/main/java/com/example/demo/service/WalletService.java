package com.example.demo.service;

import com.example.demo.model.User;
import com.example.demo.model.WalletTransaction;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.WalletTransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WalletService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WalletTransactionRepository transactionRepository;

    @Transactional
    public synchronized boolean processTransaction(User user, Double amount, String currency, String type, String description, String referenceId) {
        user = userRepository.findById(user.getId()).orElseThrow();
        
        if ("COINS".equalsIgnoreCase(currency)) {
            Integer currentCoins = user.getCoins() != null ? user.getCoins() : 0;
            if ("DEBIT".equalsIgnoreCase(type)) {
                if (currentCoins < amount.intValue()) return false;
                user.setCoins(currentCoins - amount.intValue());
            } else if ("CREDIT".equalsIgnoreCase(type)) {
                user.setCoins(currentCoins + amount.intValue());
            } else {
                throw new IllegalArgumentException("Invalid transaction type");
            }
        } else if ("INR".equalsIgnoreCase(currency)) {
            Double currentBalance = user.getWalletBalance() != null ? user.getWalletBalance() : 0.0;
            if ("DEBIT".equalsIgnoreCase(type)) {
                if (currentBalance < amount) return false;
                user.setWalletBalance(currentBalance - amount);
            } else if ("CREDIT".equalsIgnoreCase(type)) {
                user.setWalletBalance(currentBalance + amount);
            } else {
                throw new IllegalArgumentException("Invalid transaction type");
            }
        } else {
            throw new IllegalArgumentException("Invalid currency");
        }

        userRepository.save(user);

        WalletTransaction tx = new WalletTransaction();
        tx.setUser(user);
        tx.setAmount(amount);
        tx.setCurrency(currency.toUpperCase());
        tx.setType(type.toUpperCase());
        tx.setDescription(description);
        tx.setReferenceId(referenceId);
        transactionRepository.save(tx);

        return true;
    }
}