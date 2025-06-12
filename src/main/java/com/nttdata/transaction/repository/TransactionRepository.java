package com.nttdata.transaction.repository;

import com.nttdata.transaction.model.Transaction;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionRepository extends ReactiveMongoRepository<Transaction, String> {

    Flux<Transaction> findBySourceProductId(String sourceProductId);
    Flux<Transaction> findBySourceProductIdAndDateTimeBetween(String sourceProductId,
                                                              LocalDateTime start,
                                                              LocalDateTime end);

    Flux<Transaction> findByDateTimeBetween(LocalDateTime start, LocalDateTime end);

    Flux<Transaction> findByDateRangeAndProductIds(List<String> productIds,
                                                   LocalDateTime localDateTime,
                                                   LocalDateTime localDateTime1);
}
