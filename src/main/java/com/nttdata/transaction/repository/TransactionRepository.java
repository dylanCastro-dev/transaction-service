package com.nttdata.transaction.repository;

import com.nttdata.transaction.model.Transaction;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;

@Repository
public interface TransactionRepository extends ReactiveMongoRepository<Transaction, String> {

    Flux<Transaction> findByProductId(String productId);
    Flux<Transaction> findByProductIdAndDateTimeBetween(String productId, LocalDateTime start, LocalDateTime end);
}
