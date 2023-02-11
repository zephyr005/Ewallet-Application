package com.example.project;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class WalletService {

    @Autowired
    KafkaTemplate<String,String> kafkaTemplate;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    WalletRepository walletRepository;

    @KafkaListener(topics = "create_wallet",groupId = "test1234")
    public void createWallet(String message){
        Wallet wallet = Wallet.builder().userName(message).balance(100).build();
        walletRepository.save(wallet);
    }


    @KafkaListener(topics = "update_wallet",groupId = "test1234")
    public void updateWallet(String message) throws JsonProcessingException{

    }
}
