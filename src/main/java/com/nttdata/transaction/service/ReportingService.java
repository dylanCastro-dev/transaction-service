package com.nttdata.transaction.service;

import org.openapitools.model.AvailableBalanceResponseBalance;
import org.openapitools.model.BalanceSummaryResponseBalanceSummary;
import org.openapitools.model.CommissionReportResponseCommissionReport;
import org.openapitools.model.ProductConsolidatedSummaryResponseSummary;
import org.openapitools.model.ProductGeneralSummaryResponseSummary;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;

public interface ReportingService {

    Mono<ProductGeneralSummaryResponseSummary> generateGeneralProductSummary(
            String customerId, OffsetDateTime start, OffsetDateTime end);

    Mono<ProductConsolidatedSummaryResponseSummary> generateConsolidatedProductSummary(String customerId);

    Mono<AvailableBalanceResponseBalance> generateReportAvailableBalance(String productId);

    Flux<BalanceSummaryResponseBalanceSummary> generateMonthlyBalanceSummary(String customerId);

    Flux<CommissionReportResponseCommissionReport> generateCommissionReports(String customerId,
                                                                             OffsetDateTime start,
                                                                             OffsetDateTime end);
}
