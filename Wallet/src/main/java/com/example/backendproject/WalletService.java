package com.example.backendproject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class WalletService {

    @Autowired
    KafkaTemplate<String,String> kafkaTemplate;

    @Autowired
    WalletRepository walletRepository;

    @KafkaListener(topics = "create_wallet",groupId = "test1234")
    public void createWallet(String message){
        Wallet wallet = Wallet.builder().userName(message).balance(100).build();
        walletRepository.save(wallet);
    }
}
