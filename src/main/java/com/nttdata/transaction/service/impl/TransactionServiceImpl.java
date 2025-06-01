package com.nttdata.transaction.service.impl;

import com.nttdata.transaction.model.Details.CreditProduct;
import com.nttdata.transaction.model.Details.ProductDetails;
import com.nttdata.transaction.model.Details.SavingsAccount;
import com.nttdata.transaction.model.Details.FixedTermAccount;
import com.nttdata.transaction.model.Dto.BankProductDTO;
import com.nttdata.transaction.model.Transaction;
import com.nttdata.transaction.model.Type.ProductType;
import com.nttdata.transaction.model.Type.TransactionType;
import com.nttdata.transaction.repository.TransactionRepository;
import com.nttdata.transaction.service.ProductService;
import com.nttdata.transaction.service.TransactionService;
import com.nttdata.transaction.utils.Constants;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {
    private static final Logger log = LoggerFactory.getLogger(TransactionServiceImpl.class);
    private final TransactionRepository repository;
    private final ProductService productService;

    @Override
    public Flux<Transaction> getAll() {
        return repository.findAll();
    }

    @Override
    public Mono<Transaction> getById(String id) {
        return repository.findById(id);
    }

    @Override
    public Flux<Transaction> getByProductId(String productId) {
        return repository.findByProductId(productId);
    }

    @Override
    public Mono<Transaction> update(String id, Transaction transaction) {
        return repository.findById(id)
                .flatMap(existing -> {
                    existing.setType(transaction.getType());
                    existing.setAmount(transaction.getAmount());
                    existing.setProductId(transaction.getProductId());
                    return repository.save(existing);
                });
    }

    @Override
    public Mono<Void> delete(String id) {
        return repository.deleteById(id);
    }

    @Override
    public Mono<Transaction> create(Transaction transaction) {
        transaction.setDateTime(LocalDateTime.now());

        return  productService.fetchProductById(transaction.getProductId())
                .flatMap(response -> {
                    BankProductDTO product = response.getProducts().get(0);
                    switch (transaction.getType()) {
                        case PAYMENT:
                            // Procesa el pago de un producto de crédito, como préstamos o tarjetas de crédito.
                            return handleCreditPayment(transaction, product);
                        case WITHDRAWAL:
                            // Si el producto es una tarjeta de crédito:
                            // Aplica la lógica específica para retiros con crédito.
                            // En caso contrario, maneja el retiro como una transacción bancaria común.
                            return isCreditCard(product.getType())
                                    ? handleCreditCardWithdrawal(transaction, product)
                                    : handleBankTransaction(transaction, product);
                        case DEPOSIT:
                            // Aplica el depósito directamente en productos bancarios (c. de ahorro, corriente, etc.).
                            return handleBankTransaction(transaction, product);
                        default:
                            // Rechaza transacciones no soportadas por el sistema.
                            return Mono.error(
                                    new IllegalArgumentException(Constants.ERROR_UNSUPPORTED_TRANSACTION_TYPE));
                    }
                });
    }

    private Mono<Transaction> handleCreditPayment(Transaction tx, BankProductDTO product) {
        //Funcion que maneja el pago de dinero de tarjetas de credito
        if (!isCreditCard(product.getType())) {
            return Mono.error(new IllegalArgumentException(Constants.ERROR_PAYMENT_ONLY_FOR_CREDIT_PRODUCTS));
        }

        BigDecimal balance = safeBalance(product);
        BigDecimal amount = tx.getAmount();

        if (amount.compareTo(balance) > 0) {
            return Mono.error(new IllegalArgumentException(Constants.ERROR_PAYMENT_EXCEEDS_DEBT));
        }

        product.setBalance(balance.subtract(amount));
        return updateProductAndSaveTransaction(product, tx);
    }


    private Mono<Transaction> handleCreditCardWithdrawal(Transaction tx, BankProductDTO product) {
        //Funcion que maneja retiro de dinero de tarjetas de credito
        CreditProduct detailsCreditProduct = (CreditProduct) product.getDetails();
        if (detailsCreditProduct.getCreditLimit() == null) {
            return Mono.error(new IllegalArgumentException(Constants.ERROR_CREDIT_LIMIT_NOT_DEFINED));
        }

        BigDecimal balance = safeBalance(product);
        BigDecimal newBalance = balance.add(tx.getAmount());

        if (newBalance.compareTo(detailsCreditProduct.getCreditLimit()) > 0) {
            return Mono.error(new IllegalArgumentException(Constants.ERROR_AMOUNT_EXCEEDS_CREDIT_LIMIT));
        }

        product.setBalance(newBalance);
        return updateProductAndSaveTransaction(product, tx);
    }

    private Mono<Transaction> handleBankTransaction(Transaction tx, BankProductDTO product) {
        //Funcion que maneja transacciones de cuentas bancarias
        if (!isBankAccount(product.getType())) {
            return Mono.error(new IllegalArgumentException(Constants.ERROR_INVALID_TRANSACTION_FOR_PRODUCT));
        }

        // Validación: Si es cuenta de plazo fijo, solo permite transacción en un día específico
        if (product.getType() == ProductType.FIXED_TERM) {
            FixedTermAccount detailsFixedTermAccount = (FixedTermAccount) product.getDetails();
            int today = LocalDate.now().getDayOfMonth();
            if (today != detailsFixedTermAccount.getAllowedTransactionDay()) {
                return Mono.error(
                        new IllegalArgumentException(String.format(
                                Constants.ERROR_FIXED_TERM_WRONG_DAY,
                                detailsFixedTermAccount.getAllowedTransactionDay())));
            }
        }

        //Valida el limite de movimientos mensuales que tiene la cuenta
        return  canPerformTransaction(product)
                .flatMap(canProceed -> {
                    if (!canProceed) {
                        return Mono.error(new IllegalArgumentException(Constants.ERROR_MONTHLY_LIMIT_REACHED));
                    }

                    BigDecimal balance = safeBalance(product);
                    BigDecimal newBalance;

                    //Realiza los calculos por si es un deposito o un retiro para luego actualizar el producto
                    if (tx.getType() == TransactionType.DEPOSIT) {
                        newBalance = balance.add(tx.getAmount());
                    } else if (tx.getType() == TransactionType.WITHDRAWAL) {
                        if (tx.getAmount().compareTo(balance) > 0) {
                            return Mono.error(new IllegalArgumentException(Constants.ERROR_INSUFFICIENT_FUNDS));
                        }
                        newBalance = balance.subtract(tx.getAmount());
                    } else {
                        return Mono.error(
                                new IllegalArgumentException(Constants.ERROR_INVALID_TRANSACTION_FOR_BANK_ACCOUNT));
                    }

                    product.setBalance(newBalance);
                    return updateProductAndSaveTransaction(product, tx);
                });
    }

    private Mono<Transaction> updateProductAndSaveTransaction(BankProductDTO product, Transaction tx) {
        //Actualiza el producto y registra una nueva transaccion
            return productService.updateProduct(product)
                .flatMap(response -> {
                    return repository.save(tx);
                });
    }

    private BigDecimal safeBalance(BankProductDTO product) {
        //Funcion que trata de devolver un balance correcto
        return product.getBalance() != null ? product.getBalance() : BigDecimal.ZERO;
    }

    private boolean isCreditCard(ProductType type) {
        //Funcion que valida si es una tarjeta de credito
        return type == ProductType.CREDIT;
    }

    private Mono<Boolean> canPerformTransaction(BankProductDTO product) {
        //Funcion que valida cuantas transacciones le quedan al producto
        if (product.getType() == ProductType.CURRENT) {
            return Mono.just(true); // No hay límite de movimientos
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfMonth = now.withDayOfMonth(1).with(LocalTime.MIN);
        LocalDateTime endOfMonth = now.withDayOfMonth(now.toLocalDate().lengthOfMonth()).with(LocalTime.MAX);

        return repository.findByProductIdAndDateTimeBetween(product.getId(), startOfMonth, endOfMonth)
                .count()
                .map(count -> {
                    ProductDetails details = product.getDetails();
                    Integer limit = null;

                    if (details instanceof SavingsAccount) {
                        limit = ((SavingsAccount) details).getMonthlyLimit();
                    }

                    if (details instanceof FixedTermAccount) {
                        limit = ((FixedTermAccount) details).getMonthlyLimit();
                    }

                    return count < limit;
                });
    }

    private boolean isBankAccount(ProductType type) {
        //Funcion que valida si la cuenta es bancaria
        return type == ProductType.SAVINGS ||
                type == ProductType.CURRENT ||
                type == ProductType.FIXED_TERM;
    }

}
