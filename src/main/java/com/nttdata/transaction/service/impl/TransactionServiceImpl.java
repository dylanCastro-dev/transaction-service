package com.nttdata.transaction.service.impl;


import com.nttdata.transaction.model.Details.CreditProduct;
import com.nttdata.transaction.model.Details.FixedTermAccount;
import com.nttdata.transaction.model.Details.SavingsAccount;
import com.nttdata.transaction.model.Details.CurrentAccount;
import com.nttdata.transaction.model.Details.ProductDetails;
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
        return repository.findBySourceProductId(productId);
    }

    @Override
    public Mono<Transaction> update(String id, Transaction transaction) {
        return repository.findById(id)
                .flatMap(existing -> {
                    existing.setType(transaction.getType());
                    existing.setAmount(transaction.getAmount());
                    existing.setSourceProductId(transaction.getSourceProductId());
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

        return  productService.getProductById(transaction.getSourceProductId())
                .flatMap(responseSource -> {
                    BankProductDTO productSource = responseSource.getProducts().get(0);
                    switch (transaction.getType()) {
                        case PAYMENT:
                            // Procesa el pago de un producto de crédito, como préstamos o tarjetas de crédito.
                            return handleCreditPayment(transaction, productSource);
                        case WITHDRAWAL:
                            // Si el producto es una tarjeta de crédito:
                            // Aplica la lógica específica para retiros con crédito.
                            // En caso contrario, maneja el retiro como una transacción bancaria común.
                            return isCreditCard(productSource.getType())
                                    ? handleCreditCardWithdrawal(transaction, productSource)
                                    : handleBankTransaction(transaction, productSource, null);
                        case DEPOSIT:
                            // Aplica el depósito directamente en productos bancarios (c. de ahorro, corriente, etc.).
                            return handleBankTransaction(transaction, productSource, null);
                        case TRANSFER:
                            //Busca el producto destino
                            return productService.getProductById(transaction.getTargetProductId())
                                    .flatMap(responseTarget ->{
                                                BankProductDTO productTarget = responseTarget.getProducts().get(0);
                                                return handleBankTransaction(transaction, productSource, productTarget);
                                    });
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
        return updateProductAndSaveTransaction(product, null, tx);
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
        return updateProductAndSaveTransaction(product, null, tx);
    }

    /**
     * Maneja transacciones para productos bancarios (cuentas de ahorro, corriente, etc.).
     */
    private Mono<Transaction> handleBankTransaction(Transaction tx,
                                                    BankProductDTO productSource,
                                                    BankProductDTO productTarget) {
        return Mono.empty()
                //Valida que el producto sea de tipo bancario.
                .then(validateBankAccountType(productSource))
                //Valida que una cuenta a plazo fijo solo permita transacción en el día permitido.
                .then(validateFixedTermAccountRestrictions(productSource))
                //Valida que el número de transacciones mensuales permitidas no se haya superado.
                .then(validateMonthlyTransactionLimit(productSource))
                //Valida que la cuentas para las transferencias son validas.
                .then(validateTransferAccountType(tx, productSource, productTarget))
                // Se aplica la lógica para verificar y calcular la comisión si excede el límite
                .then(applyTransactionFeeIfExceeded(tx, productSource))
                //Realiza el cálculo del nuevo saldo y actualiza el producto, guardando la transacción.
                .flatMap(valid -> processTransactionAndUpdate(tx, productSource, productTarget));
    }

    private Mono<Void> validateTransferAccountType(Transaction tx,
                                                   BankProductDTO productSource,
                                                   BankProductDTO productTarget) {
        if(tx.getType() == TransactionType.TRANSFER){
            if(productSource == null){
                return Mono.error(new IllegalArgumentException(Constants.ERROR_INVALID_TRANSFER_FOR_SOURCE_ACCOUNT));
            }
            if(productTarget == null){
                return Mono.error(new IllegalArgumentException(Constants.ERROR_INVALID_TRANSFER_FOR_TARGET_ACCOUNT));
            }
            if (!productSource.getType().equals(ProductType.CURRENT)
                    && !productSource.getType().equals(ProductType.SAVINGS)){
                return Mono.error(new IllegalArgumentException(Constants.ERROR_INVALID_TRANSFER_FOR_SOURCE_ACCOUNT));
            }
            if(!productTarget.getType().equals(ProductType.CURRENT)
                    && !productTarget.getType().equals(ProductType.SAVINGS)){
                return Mono.error(new IllegalArgumentException(Constants.ERROR_INVALID_TRANSFER_FOR_TARGET_ACCOUNT));
            }
        }
        return Mono.empty();
    }

    private Mono<Void> validateBankAccountType(BankProductDTO product) {
        if (!isBankAccount(product.getType())) {
            return Mono.error(new IllegalArgumentException(Constants.ERROR_INVALID_TRANSACTION_FOR_PRODUCT));
        }
        return Mono.empty();
    }

    private Mono<Void> validateFixedTermAccountRestrictions(BankProductDTO product) {
        if (product.getType() != ProductType.FIXED_TERM) {
            return Mono.empty();
        }

        FixedTermAccount details = (FixedTermAccount) product.getDetails();
        int today = LocalDate.now().getDayOfMonth();

        if (today != details.getAllowedTransactionDay()) {
            return Mono.error(new IllegalArgumentException(
                    String.format(Constants.ERROR_FIXED_TERM_WRONG_DAY, details.getAllowedTransactionDay())));
        }

        return Mono.empty();
    }

    private Mono<Void> validateMonthlyTransactionLimit(BankProductDTO product) {
        return canPerformTransaction(product)
                .flatMap(canProceed -> {
                    if (!canProceed) {
                        return Mono.error(new IllegalArgumentException(Constants.ERROR_MONTHLY_LIMIT_REACHED));
                    }
                    return Mono.empty();
                });
    }

    private Mono<Transaction> processTransactionAndUpdate(Transaction tx,
                                                          BankProductDTO productSource,
                                                          BankProductDTO productTarget) {
        BigDecimal balance = safeBalance(productSource);
        BigDecimal fee = tx.getTransactionFee() != null ? BigDecimal.valueOf(tx.getTransactionFee()) : BigDecimal.ZERO;
        BigDecimal newBalance;

        if (tx.getType() == TransactionType.DEPOSIT) {
            // El depósito se reduce por la comisión cobrada
            BigDecimal netAmount = tx.getAmount().subtract(fee);
            newBalance = balance.add(netAmount);
            productSource.setBalance(newBalance);
        }
        else if(tx.getType() == TransactionType.TRANSFER){
            //Retiro del dinero en la cuenta origen
            BigDecimal totalWithdrawal = tx.getAmount().add(fee);
            if (totalWithdrawal.compareTo(balance) > 0) {
                return Mono.error(new IllegalArgumentException(Constants.ERROR_INSUFFICIENT_FUNDS));
            }
            newBalance = balance.subtract(totalWithdrawal);
            productSource.setBalance(newBalance);

            //Ingreso del dinero a la cuenta destino
            BigDecimal newBalanceProductTarget = productTarget.getBalance().add(tx.getAmount());
            productTarget.setBalance(newBalanceProductTarget);

        }
        else if (tx.getType() == TransactionType.WITHDRAWAL || tx.getType() == TransactionType.PURCHASE) {
            // El retiro se incrementa por la comisión
            BigDecimal totalWithdrawal = tx.getAmount().add(fee);
            if (totalWithdrawal.compareTo(balance) > 0) {
                return Mono.error(new IllegalArgumentException(Constants.ERROR_INSUFFICIENT_FUNDS));
            }
            newBalance = balance.subtract(totalWithdrawal);
            productSource.setBalance(newBalance);
        } else {
            return Mono.error(new IllegalArgumentException(Constants.ERROR_INVALID_TRANSACTION_FOR_BANK_ACCOUNT));
        }


        return updateProductAndSaveTransaction(productSource, productTarget, tx);
    }

    private Mono<Transaction> applyTransactionFeeIfExceeded(Transaction tx, BankProductDTO product) {
        if (!(product.getDetails() instanceof SavingsAccount || product.getDetails() instanceof CurrentAccount)) {
            return Mono.just(tx);
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start = now.withDayOfMonth(1).with(LocalTime.MIN);
        LocalDateTime end = now.withDayOfMonth(now.toLocalDate().lengthOfMonth()).with(LocalTime.MAX);

        return repository.findBySourceProductIdAndDateTimeBetween(product.getId(), start, end)
                .count()
                .map(count -> {
                    ProductDetails details = product.getDetails();

                    Integer freeLimit = null;
                    Double fee = null;

                    if (details instanceof SavingsAccount) {
                        SavingsAccount sa = (SavingsAccount) details;
                        freeLimit = sa.getFreeMonthlyTransactionLimit();
                        fee = sa.getTransactionFee();
                    } else if (details instanceof CurrentAccount) {
                        CurrentAccount ca = (CurrentAccount) details;
                        freeLimit = ca.getFreeMonthlyTransactionLimit();
                        fee = ca.getTransactionFee();
                    }

                    if (freeLimit != null && fee != null && fee > 0 && count >= freeLimit) {
                        tx.setTransactionFee(fee);
                    } else {
                        tx.setTransactionFee(0.0);
                    }

                    return tx;
                });
    }


    private Mono<Transaction> updateProductAndSaveTransaction(BankProductDTO productSource,
                                                              BankProductDTO productTarget,
                                                              Transaction tx) {
        Mono<Void> updateSource = productSource != null
                ? productService.updateProduct(productSource).then()
                : Mono.empty();

        Mono<Void> updateTarget = productTarget != null
                ? productService.updateProduct(productTarget).then()
                : Mono.empty();

        return updateSource
                .then(updateTarget)
                .then(repository.save(tx));
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

        return repository.findBySourceProductIdAndDateTimeBetween(product.getId(), startOfMonth, endOfMonth)
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
