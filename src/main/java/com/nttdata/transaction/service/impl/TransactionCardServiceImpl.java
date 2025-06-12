package com.nttdata.transaction.service.impl;

import com.nttdata.transaction.model.Dto.CardProductDTO;
import com.nttdata.transaction.model.Transaction;
import com.nttdata.transaction.service.ProductService;
import com.nttdata.transaction.service.TransactionCardService;
import com.nttdata.transaction.service.TransactionService;
import com.nttdata.transaction.utils.Constants;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TransactionCardServiceImpl implements TransactionCardService {
    private final ProductService productService;
    private final TransactionService transactionService;

    @Override
    public Mono<Transaction> processCardTransaction(Transaction transaction) {
        return productService.getCardById(transaction.getSourceProductId())
                .flatMap(responseSource -> {
                    CardProductDTO cardSource = responseSource.getProducts().get(0);
                    // Orden: principal + resto
                    List<String> orderedAccounts = new ArrayList<>();
                    orderedAccounts.add(cardSource.getPrimaryAccountId());
                    cardSource.getLinkedAccountIds().stream()
                            .filter(id -> !id.equals(cardSource.getPrimaryAccountId()))
                            .forEach(orderedAccounts::add);

                    return handleCardTransaction(orderedAccounts, transaction);
                });
    }

    private Mono<Transaction> handleCardTransaction( List<String> orderedAccounts, Transaction transaction){
        return Flux.fromIterable(orderedAccounts)
                .concatMap(productService::getProductById) // Consulta ordenada una por una
                .map(response -> response.getProducts().get(0))
                .filter(account -> {
                    BigDecimal balance = Optional.ofNullable(account.getBalance()).orElse(BigDecimal.ZERO);
                    return balance.compareTo(transaction.getAmount()) >= 0;
                })
                .next()
                .switchIfEmpty(Mono.error(new IllegalArgumentException(Constants.ERROR_INSUFFICIENT_FUNDS)))
                .flatMap(selectedAccount -> {
                    // Construir nueva transacción con la cuenta válida
                    Transaction newTx = new Transaction();
                    newTx.setType(transaction.getType());
                    newTx.setAmount(transaction.getAmount());
                    newTx.setSourceProductId(selectedAccount.getId());
                    newTx.setDateTime(transaction.getDateTime());
                    return transactionService.create(newTx);
                });
    }

}
