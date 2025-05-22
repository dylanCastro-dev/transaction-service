package com.nttdata.transaction.service.impl;

import com.nttdata.transaction.model.Dto.AvailableBalanceDTO;
import com.nttdata.transaction.model.Dto.BankProductDTO;
import com.nttdata.transaction.model.Transaction;
import com.nttdata.transaction.model.Type.ProductType;
import com.nttdata.transaction.model.Type.TransactionType;
import com.nttdata.transaction.repository.TransactionRepository;
import com.nttdata.transaction.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {
    private final TransactionRepository repository;

    private final WebClient webClient = WebClient.builder()
            .baseUrl("http://localhost:8081") // URL de product-service
            .build();

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
    public Mono<AvailableBalanceDTO> getAvailableBalance(String productId) {
        return webClient.get()
                .uri("/products/{id}", productId)
                .retrieve()
                .bodyToMono(BankProductDTO.class)
                .map(product -> {
                    BigDecimal balance = product.getBalance() != null ? product.getBalance() : BigDecimal.ZERO;

                    if (isCreditCard(product.getType()) && product.getCreditLimit() != null) {
                        BigDecimal available = product.getCreditLimit().subtract(balance);
                        return new AvailableBalanceDTO(product.getId(), available, "CREDIT");
                    } else {
                        return new AvailableBalanceDTO(product.getId(), balance, "BANK");
                    }
                });
    }

    @Override
    public Mono<Transaction> create(Transaction transaction) {
        transaction.setDateTime(LocalDateTime.now());

        return webClient.get()
                .uri("/products/{id}", transaction.getProductId())
                .retrieve()
                .bodyToMono(BankProductDTO.class)
                .flatMap(product -> {
                    switch (transaction.getType()) {
                        case PAYMENT:
                            // Procesa el pago de un producto de crédito, como préstamos o tarjetas de crédito.
                            return handleCreditPayment(transaction, product);
                        case WITHDRAWAL:
                            // Si el producto es una tarjeta de crédito, aplica la lógica específica para retiros con crédito.
                            // En caso contrario, maneja el retiro como una transacción bancaria común.
                            return isCreditCard(product.getType())
                                    ? handleCreditCardWithdrawal(transaction, product)
                                    : handleBankTransaction(transaction, product);
                        case DEPOSIT:
                            // Aplica el depósito directamente en productos bancarios (cuentas de ahorro, corriente, etc.).
                            return handleBankTransaction(transaction, product);
                        default:
                            // Rechaza cualquier tipo de transacción no soportada por el sistema.
                            return Mono.error(new IllegalArgumentException("Tipo de transacción no soportado."));
                    }
                });
    }

    public Mono<Void> applyMonthlyMaintenanceFee() {
        // Simula una ejecución mensual para aplicar la comisión de mantenimiento
        return webClient.get()
                .uri("/products")
                .retrieve()
                .bodyToFlux(BankProductDTO.class)
                .filter(product -> {
                    Double fee = product.getMaintenanceFee();
                    return fee != null && fee > 0;
                })
                .flatMap(product -> {
                    BigDecimal fee = BigDecimal.valueOf(product.getMaintenanceFee());
                    BigDecimal balance = Optional.ofNullable(product.getBalance()).orElse(BigDecimal.ZERO);

                    if (balance.compareTo(fee) < 0) {
                        return Mono.empty(); // Sin saldo suficiente
                    }

                    product.setBalance(balance.subtract(fee));

                    Transaction tx = Transaction.builder()
                            .productId(product.getId())
                            .amount(fee)
                            .type(TransactionType.MAINTENANCE)
                            .dateTime(LocalDateTime.now())
                            .build();

                    return webClient.put()
                            .uri("/products/{id}", product.getId())
                            .bodyValue(product)
                            .retrieve()
                            .bodyToMono(BankProductDTO.class)
                            .then(repository.save(tx))
                            .then();
                })
                .then();
    }

    private Mono<Transaction> handleCreditPayment(Transaction tx, BankProductDTO product) {
        //Funcion que maneja el pago de dinero de tarjetas de credito
        if (!isProductoCredito(product.getType())) {
            return Mono.error(new IllegalArgumentException("Solo se pueden realizar pagos a productos de crédito."));
        }

        BigDecimal balance = safeBalance(product);
        BigDecimal amount = tx.getAmount();

        if (amount.compareTo(balance) > 0) {
            return Mono.error(new IllegalArgumentException("El monto del pago excede la deuda actual."));
        }

        product.setBalance(balance.subtract(amount));
        return updateProductAndSaveTransaction(product, tx);
    }


    private Mono<Transaction> handleCreditCardWithdrawal(Transaction tx, BankProductDTO product) {
        //Funcion que maneja retiro de dinero de tarjetas de credito
        if (product.getCreditLimit() == null) {
            return Mono.error(new IllegalArgumentException("Producto sin límite de crédito."));
        }

        BigDecimal balance = safeBalance(product);
        BigDecimal newBalance = balance.add(tx.getAmount());

        if (newBalance.compareTo(product.getCreditLimit()) > 0) {
            return Mono.error(new IllegalArgumentException("Monto excede límite de crédito."));
        }

        product.setBalance(newBalance);
        return updateProductAndSaveTransaction(product, tx);
    }

    private Mono<Transaction> handleBankTransaction(Transaction tx, BankProductDTO product) {
        //Funcion que maneja transacciones de cuentas bancarias
        if (!isCuentaBancaria(product.getType())) {
            return Mono.error(new IllegalArgumentException("Transacción no válida para este tipo de producto."));
        }

        // Validación: Si es cuenta de plazo fijo, solo permite transacción en un día específico
        if (product.getType() == ProductType.PLAZO_FIJO && product.getAllowedTransactionDay() != null) {
            int today = LocalDate.now().getDayOfMonth();
            if (today != product.getAllowedTransactionDay()) {
                return Mono.error(new IllegalArgumentException(
                        "Solo se permiten transacciones el día " + product.getAllowedTransactionDay() + " de cada mes para cuentas a plazo fijo."
                ));
            }
        }

        //Valida el limite de movimientos mensuales que tiene la cuenta
        return  canPerformTransaction(product)
                .flatMap(canProceed -> {
                    if (!canProceed) {
                        return Mono.error(new IllegalArgumentException("Límite mensual de movimientos alcanzado para este producto."));
                    }

                    BigDecimal balance = safeBalance(product);
                    BigDecimal newBalance;

                    //Realiza los calculos por si es un deposito o un retiro para luego actualizar el producto
                    if (tx.getType() == TransactionType.DEPOSIT) {
                        newBalance = balance.add(tx.getAmount());
                    } else if (tx.getType() == TransactionType.WITHDRAWAL) {
                        double maintenanceFee = 0.0;
                        if (product.getType() == ProductType.CORRIENTE) {
                            //Aplica el costo de mantenimiento
                            maintenanceFee = product.getMaintenanceFee() != null ? product.getMaintenanceFee() : 0.0;
                        }
                        BigDecimal totalDebit = tx.getAmount().add(BigDecimal.valueOf(maintenanceFee));
                        if (totalDebit.compareTo(balance) > 0) {
                            return Mono.error(new IllegalArgumentException("Fondos insuficientes para el retiro."));
                        }
                        newBalance = balance.subtract(totalDebit);
                    } else {
                        return Mono.error(new IllegalArgumentException("Tipo de transacción no válido para cuenta bancaria."));
                    }

                    product.setBalance(newBalance);
                    return updateProductAndSaveTransaction(product, tx);
                });
    }

    private Mono<Transaction> updateProductAndSaveTransaction(BankProductDTO product, Transaction tx) {
        //Actualiza el producto y registra una nueva transaccion
        return webClient.put()
                .uri("/products/{id}", product.getId())
                .bodyValue(product)
                .retrieve()
                .bodyToMono(BankProductDTO.class)
                .flatMap(updated -> repository.save(tx));
    }

    private BigDecimal safeBalance(BankProductDTO product) {
        //Funcion que trata de devolver un balance correcto
        return product.getBalance() != null ? product.getBalance() : BigDecimal.ZERO;
    }

    private boolean isCreditCard(ProductType type) {
        //Funcion que valida si es una tarjeta de credito
        return type == ProductType.TARJETA_CREDITO;
    }

    private Mono<Boolean> canPerformTransaction(BankProductDTO product) {
        //Funcion que valida cuantas transacciones le quedan al producto
        if (product.getType() != ProductType.AHORRO) {
            return Mono.just(true); // No hay límite de movimientos
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfMonth = now.withDayOfMonth(1).with(LocalTime.MIN);
        LocalDateTime endOfMonth = now.withDayOfMonth(now.toLocalDate().lengthOfMonth()).with(LocalTime.MAX);

        return repository.findByProductIdAndDateTimeBetween(product.getId(), startOfMonth, endOfMonth)
                .count()
                .map(count -> count < product.getMonthlyLimit());
    }

    private boolean isCuentaBancaria(ProductType type) {
        //Funcion que valida si la cuenta es bancaria
        return type == ProductType.AHORRO ||
                type == ProductType.CORRIENTE ||
                type == ProductType.PLAZO_FIJO;
    }

    private boolean isProductoCredito(ProductType type) {
        //Funcion que valida si el producto es credtio
        return type == ProductType.CREDITO_PERSONAL
                || type == ProductType.CREDITO_EMPRESARIAL
                || type == ProductType.TARJETA_CREDITO;
    }

}
