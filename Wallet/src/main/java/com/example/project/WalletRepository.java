package com.example.project;

import org.springframework.data.jpa.repository.JpaRepository;

public interface WalletRepository extends JpaRepository<Wallet,Integer> {

    Wallet findWalletByUserName(String fromUser);
}
