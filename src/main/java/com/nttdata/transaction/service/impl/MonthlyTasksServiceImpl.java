package com.nttdata.transaction.service.impl;

import com.nttdata.transaction.model.Details.CurrentAccount;
import com.nttdata.transaction.model.Details.FixedTermAccount;
import com.nttdata.transaction.model.Details.ProductDetails;
import com.nttdata.transaction.model.Details.SavingsAccount;
import com.nttdata.transaction.model.Dto.BankProductDTO;
import com.nttdata.transaction.model.Transaction;
import com.nttdata.transaction.model.Type.ProductStatus;
import com.nttdata.transaction.model.Type.TransactionType;
import com.nttdata.transaction.repository.TransactionRepository;
import com.nttdata.transaction.service.MonthlyTasksService;
import com.nttdata.transaction.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MonthlyTasksServiceImpl implements MonthlyTasksService {
    private static final Logger log = LoggerFactory.getLogger(MonthlyTasksServiceImpl.class);
    private final TransactionRepository repository;
    private final ProductService productService;

    @Override
    public Mono<Void> applyMonthlyTasks() {
        return productService.getAllBankProducts()
                .flatMapMany(response -> Flux.fromIterable(response.getProducts()))
                .flatMap(product -> Mono.when(
                        applyMaintenanceFeeIfNeeded(product),
                        evaluateAverageBalanceRequirement(product)
                ))
                .then();
    }


    private Mono<Void> evaluateAverageBalanceRequirement(BankProductDTO product) {
        if (!(product.getDetails() instanceof SavingsAccount)) {
            return Mono.empty();
        }

        SavingsAccount savings = (SavingsAccount) product.getDetails();
        Double requiredAvg = savings.getRequiredMonthlyAverageBalance();

        if (requiredAvg == null || requiredAvg <= 0) {
            return Mono.empty();
        }

        YearMonth currentMonth = YearMonth.now();
        LocalDateTime start = currentMonth.atDay(1).atStartOfDay();
        LocalDateTime end = currentMonth.atEndOfMonth().atTime(LocalTime.MAX);

        return repository.findBySourceProductIdAndDateTimeBetween(product.getId(), start, end)
                .groupBy(tx -> tx.getDateTime().toLocalDate()) // Agrupa por día
                .flatMap(grouped -> grouped
                        .map(Transaction::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)) //Suma el monto total por dia
                .collectList()
                .map(dailyTotals -> {
                    //Suma de todos los montos diarios
                    //Divide entre los dias del mes
                    BigDecimal total = dailyTotals.stream()
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal average = total.divide(BigDecimal.valueOf(
                            currentMonth.lengthOfMonth()), RoundingMode.HALF_UP);
                    return average.doubleValue();
                })
                .flatMap(average -> {
                    if (average < requiredAvg) {
                        log.info("Producto {} será bloqueado: " +
                                        "promedio diario {:.2f} < requerido {:.2f}",
                                product.getId(), average, requiredAvg);
                        product.setStatus(ProductStatus.BLOCKED_AVG_BALANCE); // se bloqueará el producto
                        return productService.updateProduct(product).then();
                    }
                    return Mono.empty();
                })
                .onErrorResume(e -> {
                    log.error("No se pudo evaluar saldo promedio para producto {}: {}",
                            product.getId(), e.getMessage());
                    return Mono.empty();
                });
    }

    private Mono<Void> applyMaintenanceFeeIfNeeded(BankProductDTO product) {
        return hasValidMaintenanceFee(product)
                .flatMap(valid -> valid ? applyFeeIfBalanceSufficient(product) : Mono.empty());
    }

    private Mono<Boolean> hasValidMaintenanceFee(BankProductDTO product) {
        ProductDetails details = product.getDetails();
        Double fee = null;

        if (details instanceof CurrentAccount) {
            fee = ((CurrentAccount) details).getMaintenanceFee();
        } else if (details instanceof SavingsAccount) {
            fee = ((SavingsAccount) details).getMaintenanceFee();
        } else if (details instanceof FixedTermAccount) {
            fee = ((FixedTermAccount) details).getMaintenanceFee();
        }

        return Mono.just(fee != null && fee > 0);
    }

    private Mono<Void> applyFeeIfBalanceSufficient(BankProductDTO product) {
        ProductDetails details = product.getDetails();
        Double feeValue = 0.0;

        if (details instanceof CurrentAccount) {
            feeValue = ((CurrentAccount) details).getMaintenanceFee();
        } else if (details instanceof SavingsAccount) {
            feeValue = ((SavingsAccount) details).getMaintenanceFee();
        } else if (details instanceof FixedTermAccount) {
            feeValue = ((FixedTermAccount) details).getMaintenanceFee();
        }

        BigDecimal fee = BigDecimal.valueOf(feeValue);
        BigDecimal balance = Optional.ofNullable(product.getBalance()).orElse(BigDecimal.ZERO);

        if (balance.compareTo(fee) < 0) {
            return Mono.empty(); // Sin saldo suficiente
        }

        product.setBalance(balance.subtract(fee));

        Transaction tx = Transaction.builder()
                .sourceProductId(product.getId())
                .amount(fee)
                .type(TransactionType.MAINTENANCE)
                .dateTime(LocalDateTime.now())
                .build();

        return  productService.updateProduct(product)
                .flatMap(r -> repository.save(tx))
                .then();
    }
}
