package com.nttdata.transaction.service;

import com.nttdata.transaction.model.*;
import com.nttdata.transaction.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository repository;

    private final WebClient webClient = WebClient.builder()
            .baseUrl("http://localhost:8081") // URL de product-service
            .build();

    public Flux<Transaction> getAll() {
        return repository.findAll();
    }

    public Mono<Transaction> getById(String id) {
        return repository.findById(id);
    }

    public Flux<Transaction> getByProductId(String productId) {
        return repository.findByProductId(productId);
    }

    public Mono<AvailableBalanceDTO> getAvailableBalance(String productId) {
        return webClient.get()
                .uri("/products/{id}", productId)
                .retrieve()
                .bodyToMono(BankProductDTO.class)
                .map(product -> {
                    double balance = product.getBalance() != null ? product.getBalance() : 0.0;

                    if (isCreditCard(product.getType()) && product.getCreditLimit() != null) {
                        double available = product.getCreditLimit() - balance;
                        return new AvailableBalanceDTO(product.getId(), available, "CREDIT");
                    } else {
                        return new AvailableBalanceDTO(product.getId(), balance, "BANK");
                    }
                });
    }

    public Mono<Transaction> create(Transaction transaction) {
        transaction.setDateTime(LocalDateTime.now());

        return webClient.get()
                .uri("/products/{id}", transaction.getProductId())
                .retrieve()
                .bodyToMono(BankProductDTO.class)
                .flatMap(product -> {
                    switch (transaction.getType()) {
                        case PAYMENT:
                            return handleCreditPayment(transaction, product);
                        case WITHDRAWAL:
                            return isCreditCard(product.getType())
                                    ? handleCreditCardWithdrawal(transaction, product)
                                    : handleBankTransaction(transaction, product);
                        case DEPOSIT:
                            return handleBankTransaction(transaction, product);
                        default:
                            return Mono.error(new IllegalArgumentException("Tipo de transacción no soportado."));
                    }
                });
    }

    private Mono<Transaction> handleCreditPayment(Transaction tx, BankProductDTO product) {
        if (!isProductoCredito(product.getType())) {
            return Mono.error(new IllegalArgumentException("Solo se pueden realizar pagos a productos de crédito."));
        }

        double balance = safeBalance(product);
        if (tx.getAmount() > balance) {
            return Mono.error(new IllegalArgumentException("El monto del pago excede la deuda actual."));
        }

        product.setBalance(balance - tx.getAmount());
        return updateProductAndSaveTransaction(product, tx);
    }

    private Mono<Transaction> handleCreditCardWithdrawal(Transaction tx, BankProductDTO product) {
        if (product.getCreditLimit() == null) {
            return Mono.error(new IllegalArgumentException("Producto sin límite de crédito."));
        }

        double balance = safeBalance(product);
        double newBalance = balance + tx.getAmount();

        if (newBalance > product.getCreditLimit()) {
            return Mono.error(new IllegalArgumentException("Monto excede límite de crédito."));
        }

        product.setBalance(newBalance);
        return updateProductAndSaveTransaction(product, tx);
    }

    private Mono<Transaction> handleBankTransaction(Transaction tx, BankProductDTO product) {
        if (!isCuentaBancaria(product.getType())) {
            return Mono.error(new IllegalArgumentException("Transacción no válida para este tipo de producto."));
        }

        double balance = safeBalance(product);
        double newBalance;

        if (tx.getType() == TransactionType.DEPOSIT) {
            newBalance = balance + tx.getAmount();
        } else if (tx.getType() == TransactionType.WITHDRAWAL) {
            if (tx.getAmount() > balance) {
                return Mono.error(new IllegalArgumentException("Fondos insuficientes para el retiro."));
            }
            newBalance = balance - tx.getAmount();
        } else {
            return Mono.error(new IllegalArgumentException("Tipo de transacción no válido para cuenta bancaria."));
        }

        product.setBalance(newBalance);
        return updateProductAndSaveTransaction(product, tx);
    }

    private Mono<Transaction> updateProductAndSaveTransaction(BankProductDTO product, Transaction tx) {
        return webClient.put()
                .uri("/products/{id}", product.getId())
                .bodyValue(product)
                .retrieve()
                .bodyToMono(BankProductDTO.class)
                .flatMap(updated -> repository.save(tx));
    }

    private double safeBalance(BankProductDTO product) {
        return product.getBalance() != null ? product.getBalance() : 0.0;
    }

    public Mono<Transaction> update(String id, Transaction transaction) {
        return repository.findById(id)
                .flatMap(existing -> {
                    existing.setType(transaction.getType());
                    existing.setAmount(transaction.getAmount());
                    existing.setProductId(transaction.getProductId());
                    return repository.save(existing);
                });
    }

    public Mono<Void> delete(String id) {
        return repository.deleteById(id);
    }

    public Flux<Transaction> findByProductId(String productId) {
        return repository.findByProductId(productId);
    }

    private boolean isCreditCard(ProductType type) {
        return type == ProductType.TARJETA_CREDITO;
    }

    private boolean isCuentaBancaria(ProductType type) {
        return type == ProductType.AHORRO ||
                type == ProductType.CORRIENTE ||
                type == ProductType.PLAZO_FIJO;
    }

    private boolean isProductoCredito(ProductType type) {
        return type == ProductType.CREDITO_PERSONAL
                || type == ProductType.CREDITO_EMPRESARIAL
                || type == ProductType.TARJETA_CREDITO;
    }

}
