package com.nttdata.transaction.service.impl;

import com.nttdata.transaction.model.Details.CreditProduct;
import com.nttdata.transaction.model.Dto.BankProductDTO;
import com.nttdata.transaction.model.Transaction;
import com.nttdata.transaction.model.Type.ProductType;
import com.nttdata.transaction.model.Type.TransactionType;
import com.nttdata.transaction.repository.TransactionRepository;
import com.nttdata.transaction.service.ProductService;
import com.nttdata.transaction.service.ReportingService;
import com.nttdata.transaction.utils.ProductMapper;
import lombok.RequiredArgsConstructor;
import org.openapitools.model.AvailableBalanceResponseBalance;
import org.openapitools.model.BalanceSummaryResponseBalanceSummary;
import org.openapitools.model.CommissionReportResponseCommissionReport;
import org.openapitools.model.ProductBalanceSummary;
import org.openapitools.model.ProductConsolidatedSummaryResponseSummary;
import org.openapitools.model.ProductGeneralSummaryResponseSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportingServiceImpl implements ReportingService {
    private static final Logger log = LoggerFactory.getLogger(ReportingServiceImpl.class);
    private final TransactionRepository repository;
    private final ProductService productService;

    @Override
    public Mono<ProductGeneralSummaryResponseSummary> generateGeneralProductSummary(
            String customerId, OffsetDateTime start, OffsetDateTime end) {

        return productService.getProductByCustomerId(customerId)
                .flatMap(response -> {
                    List<BankProductDTO> allProducts = response.getProducts();
                    List<String> productIds = allProducts.stream()
                            .map(BankProductDTO::getId)
                            .collect(Collectors.toList());

                    return repository.findByDateRangeAndProductIds(
                                    productIds,
                                    start.toLocalDateTime(),
                                    end.toLocalDateTime()
                            )
                            .flatMap(tx -> Flux.just(tx.getSourceProductId(), tx.getTargetProductId()))
                            .distinct()
                            .collectList()
                            .map(activeProductIds -> {
                                List<BankProductDTO> filtered = allProducts.stream()
                                        .filter(p -> activeProductIds.contains(p.getId()))
                                        .collect(Collectors.toList());

                                ProductGeneralSummaryResponseSummary summary =
                                        new ProductGeneralSummaryResponseSummary();
                                summary.setCustomerId(customerId);
                                summary.setStart(start);
                                summary.setEnd(end);
                                summary.setProducts(ProductMapper.toOpenApiList(filtered));

                                return summary;
                            });
                });
    }


    @Override
    public Mono<ProductConsolidatedSummaryResponseSummary> generateConsolidatedProductSummary(String customerId) {
        return productService.getProductByCustomerId(customerId)
                .map(response -> {
                    ProductConsolidatedSummaryResponseSummary summary = new ProductConsolidatedSummaryResponseSummary();
                    summary.setCustomerId(customerId);
                    summary.setProducts(ProductMapper.toOpenApiList(response.getProducts()));
                    return summary;
                });
    }

    @Override
    public Mono<AvailableBalanceResponseBalance> generateReportAvailableBalance(String sourceProductId) {
        return  productService.getProductById(sourceProductId)
                .map(response -> {
                    BankProductDTO product = response.getProducts().get(0);
                    BigDecimal balance = product.getBalance() != null ? product.getBalance() : BigDecimal.ZERO;

                    if (isCreditCard(product.getType())) {
                        CreditProduct detailsCreditProduct = (CreditProduct) product.getDetails();
                        balance = detailsCreditProduct.getCreditLimit().subtract(balance);
                    }
                    AvailableBalanceResponseBalance availableBalanceResponseBalance =
                            new AvailableBalanceResponseBalance();
                    availableBalanceResponseBalance.setProductId(product.getId());
                    availableBalanceResponseBalance.setAvailableBalance(balance);
                    availableBalanceResponseBalance.setProductCategory(product.getType().name());
                    return availableBalanceResponseBalance;
                });
    }

    @Override
    public Flux<BalanceSummaryResponseBalanceSummary> generateMonthlyBalanceSummary(String customerId) {
        YearMonth currentMonth = YearMonth.now();
        LocalDateTime startDateTime = currentMonth.atDay(1).atStartOfDay();
        LocalDateTime endDateTime = currentMonth.atEndOfMonth().atTime(LocalTime.MAX);

        return productService.getProductByCustomerId(customerId) // Mono<TemplateResponse>
                .flatMapMany(templateResponse -> Flux.fromIterable(templateResponse.getProducts()))
                .flatMap(product -> calculateAverageBalance(product, startDateTime, endDateTime))
                .collectList()
                .map(summaries -> {
                    BalanceSummaryResponseBalanceSummary response = new BalanceSummaryResponseBalanceSummary();
                    response.setCustomerId(customerId);
                    response.setMonth(currentMonth.toString());
                    response.setProducts(summaries);
                    return response;
                })
                .flatMapMany(Flux::just);
    }

    @Override
    public Flux<CommissionReportResponseCommissionReport> generateCommissionReports(String customerId,
                                                                                    OffsetDateTime start,
                                                                                    OffsetDateTime end) {
        return productService.getProductByCustomerId(customerId)
                .flatMapMany(response ->
                        calculateCommissionsPerProduct(response.getProducts(),
                                start.toLocalDateTime(),
                                end.toLocalDateTime()));
    }


    private Flux<CommissionReportResponseCommissionReport> calculateCommissionsPerProduct(List<BankProductDTO> products,
                                                                                          LocalDateTime start,
                                                                                          LocalDateTime end) {

        Map<String, ProductType> productTypeMap = products.stream()
                .collect(Collectors.toMap(BankProductDTO::getId, BankProductDTO::getType));

        return repository.findByDateTimeBetween(start, end)
                .filter(tx -> tx.getTransactionFee() != null && tx.getTransactionFee() > 0)
                .filter(tx -> productTypeMap.containsKey(tx.getSourceProductId()))
                .groupBy(Transaction::getSourceProductId)
                .flatMap(grouped -> grouped.collectList().map(transactions -> {
                    String productId = grouped.key();
                    ProductType productType = productTypeMap.get(productId);

                    BigDecimal totalFee = transactions.stream()
                            .map(Transaction::getTransactionFee)
                            .map(BigDecimal::valueOf)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    CommissionReportResponseCommissionReport report = new CommissionReportResponseCommissionReport();
                    report.setProductId(productId);
                    report.setProductType(productType.name());
                    report.setTotalTransactions(transactions.size());
                    report.setTotalFee(totalFee);
                    report.setCurrency("PEN");

                    return report;
                }));
    }




    private Mono<ProductBalanceSummary> calculateAverageBalance(BankProductDTO sourceProductId,
                                                                LocalDateTime startDateTime,
                                                                LocalDateTime endDateTime) {

        return repository.findBySourceProductIdAndDateTimeBetween(sourceProductId.getId(), startDateTime, endDateTime)
                .collectList()
                .map(transactions -> {
                    Map<LocalDate, BigDecimal> dailyBalances = getDailyBalances(startDateTime,
                            endDateTime, transactions,
                            sourceProductId.getBalance());
                    BigDecimal average = dailyBalances.values().stream()
                            .reduce(BigDecimal.ZERO, BigDecimal::add)
                            .divide(BigDecimal.valueOf(dailyBalances.size()), 2, RoundingMode.HALF_UP);

                    ProductBalanceSummary response = new ProductBalanceSummary();
                    response.setProductId(sourceProductId.getId());
                    response.setProductType(sourceProductId.getType().name());
                    response.setAverageDailyBalance(average);
                    return response;
                });
    }

    private Map<LocalDate, BigDecimal> getDailyBalances(LocalDateTime start,
                                                        LocalDateTime end,
                                                        List<Transaction> txs,
                                                        BigDecimal initialBalance) {
        Map<LocalDate, BigDecimal> balances = new LinkedHashMap<>();
        BigDecimal balance = initialBalance;

        for (LocalDate date = start.toLocalDate(); !date.isAfter(end.toLocalDate()); date = date.plusDays(1)) {
            final LocalDate currentDate = date;

            BigDecimal delta = txs.stream()
                    .filter(t -> t.getDateTime().toLocalDate().equals(currentDate))
                    .map(t -> t.getType() == TransactionType.DEPOSIT
                            ? t.getAmount()
                            : t.getAmount().negate())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            balance = balance.add(delta);
            balances.put(currentDate, balance);
        }

        return balances;
    }

    private boolean isCreditCard(ProductType type) {
        //Funcion que valida si es una tarjeta de credito
        return type == ProductType.CREDIT;
    }


}
