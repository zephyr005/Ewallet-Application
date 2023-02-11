package com.example.project;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Date;
import java.util.UUID;

@Service
public class TransactionService {

    @Autowired
    TransactionRepository transactionRepository;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    KafkaTemplate<String,String> kafkaTemplate;

    @Autowired
    RestTemplate restTemplate;

    public void createTransaction(TransactionRequestDTO transactionRequestDTO) throws JsonProcessingException {
        //First of all we will create  a transaction entity and put its status to PENDING
        Transaction transaction = Transaction.builder().fromUser(transactionRequestDTO.getFromUser())
                .toUser(transactionRequestDTO.getToUser()).transactionId(UUID.randomUUID().toString())
                .transactionDate(new Date()).transactionStatus(TransactionStatus.PENDING)
                .amount(transactionRequestDTO.getAmount()).purpose(transactionRequestDTO.getPurpose()).build();

        transactionRepository.save(transaction);

        //Create that JsonObject
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("fromUser",transactionRequest.getFromUser());
        jsonObject.put("toUser",transactionRequest.getToUser());
        jsonObject.put("amount",transactionRequest.getAmount());
        jsonObject.put("transactionId",transaction.getTransactionId());

        //Converted to string and send it via kafka to the wallet microservice
        String kafkaMessage = objectMapper.writeValueAsString(jsonObject);
        kafkaTemplate.send("update_wallet",kafkaMessage);

    }
}
