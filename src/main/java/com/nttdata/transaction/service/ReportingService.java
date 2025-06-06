package com.nttdata.transaction.service;

import org.openapitools.model.AvailableBalanceResponseBalance;
import org.openapitools.model.BalanceSummaryResponseBalanceSummary;
import org.openapitools.model.CommissionReportResponseCommissionReport;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;

public interface ReportingService {
    /**
     * Obtiene el saldo disponible de un producto bancario.
     * Para productos de crédito, calcula el disponible como límite menos deuda actual.
     *
     * @param productId Identificador del producto bancario
     * @return Mono con el saldo disponible
     */
    Mono<AvailableBalanceResponseBalance> generateReportAvailableBalance(String productId);

    Flux<BalanceSummaryResponseBalanceSummary> generateMonthlyBalanceSummary(String customerId);

    Flux<CommissionReportResponseCommissionReport> generateCommissionReports(String customerId,
                                                                             OffsetDateTime start,
                                                                             OffsetDateTime end);
}
