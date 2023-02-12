package com.example.project;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
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
        jsonObject.put("fromUser",transactionRequestDTO.getFromUser());
        jsonObject.put("toUser",transactionRequestDTO.getToUser());
        jsonObject.put("amount",transactionRequestDTO.getAmount());
        jsonObject.put("transactionId",transaction.getTransactionId());

        //Converted to string and send it via kafka to the wallet microservice
        String kafkaMessage = objectMapper.writeValueAsString(jsonObject);
        kafkaTemplate.send("update_wallet",kafkaMessage);
    }


    @KafkaListener(topics = "update_transaction",groupId = "friends_group")
    public String updateTransaction(String message) throws JsonProcessingException{
        //Decode the message
        JSONObject transactionRequest = objectMapper.readValue(message,JSONObject.class);

        String transactionStatus = (String)transactionRequest.get("status");
        String transactionId = (String) transactionRequest.get("transactionId");

        //Find transaction
        Transaction transaction = transactionRepository.findByTransactionId(transactionId);

        transaction.setTransactionStatus(TransactionStatus.valueOf(transactionStatus));

        transactionRepository.save(transaction);

        //Call Notification Service and Send Emails
        return callNotificationService(transaction);
    }


    private String callNotificationService(Transaction transaction) {
        String fromUserName = transaction.getFromUser();
        String toUserName = transaction.getToUser();
        String transactionId = transaction.getTransactionId();

        URI url = URI.create(("http://localhost:5551/user/findEmailNameDTO/" + fromUserName));
        HttpEntity httpEntity = new HttpEntity<>(new HttpHeaders());

        JSONObject fromUserObject = restTemplate.exchange(url, HttpMethod.GET, httpEntity, JSONObject.class).getBody();

        String senderName = (String) fromUserObject.get("name");
        String senderEmail = (String) fromUserObject.get("email");



        url = URI.create(("http://localhost:5551/user/findEmailNameDTO/" + toUserName));

        JSONObject toUserObject = restTemplate.exchange(url, HttpMethod.GET, httpEntity, JSONObject.class).getBody();

        String receiverName = (String) toUserObject.get("name");
        String receiverEmail = (String) toUserObject.get("email");

        //Send the email and message to notifications-service via kafka
        JSONObject emailRequest = new JSONObject();
        emailRequest.put("email",senderEmail);

        String senderMessageBody = String.format("Hi %s \n\n" +
                "The amount of Rs. %d with transactionId %s has been debited %s. \n\n\n\n\n" +
                        "Thank You \n"+
                        "Ewallet Team",
                senderName,transaction.getAmount(),transactionId,transaction.getTransactionStatus());

        emailRequest.put("message",senderMessageBody);

        String message = emailRequest.toString();

        //Send it to kafka
        kafkaTemplate.send("send_email",message);



        if(transaction.getTransactionStatus().equals("FAILED")){
            return "Transaction has been failed.";
        }

        //Send an email to the receiver also
        emailRequest.put("email",receiverEmail);

        String receiverMessageBody = String.format("Hi %s \n\n" +
                "You have received money %d from %s. \n\n\n\n\n" +
                        "Thank You \n"+
                        "Ewallet Team",
                receiverName,transaction.getAmount(),senderName);


        emailRequest.put("message",receiverMessageBody);

        message = emailRequest.toString();

        kafkaTemplate.send("send_email",message);
        return "Transaction has been successful.";
    }
}
