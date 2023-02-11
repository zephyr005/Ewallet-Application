package com.example.project;

import lombok.Data;

@Data
public class TransactionRequestDTO {
    private String fromUser;

    private String toUser;

    private int amount;

    private String purpose;
}
