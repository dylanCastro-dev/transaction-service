package com.nttdata.transaction.service;

import com.nttdata.transaction.model.Transaction;
import reactor.core.publisher.Mono;

public interface TransactionCardService {
    Mono<Transaction> processCardTransaction(Transaction transaction);
}
