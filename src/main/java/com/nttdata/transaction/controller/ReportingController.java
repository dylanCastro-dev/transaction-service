package com.nttdata.transaction.controller;

import com.nttdata.transaction.service.ReportingService;
import com.nttdata.transaction.utils.Constants;
import com.nttdata.transaction.utils.TransactionMapper;
import lombok.RequiredArgsConstructor;
import org.openapitools.api.ReportApi;
import org.openapitools.model.AvailableBalanceResponse;
import org.openapitools.model.BalanceSummaryResponse;
import org.openapitools.model.CommissionReportResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;

@RestController
@RequiredArgsConstructor
public class ReportingController implements ReportApi {
    private final ReportingService reportingService;

    @Override
    public Mono<ResponseEntity<AvailableBalanceResponse>> generateReportAvailableBalance
            (String productId, ServerWebExchange exchange) {
        return reportingService.generateReportAvailableBalance(productId)
                .map(report ->
                        TransactionMapper.toResponseAvailableBalance(report,
                                200,
                                Constants.SUCCESS_GET_BALANCE))
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(TransactionMapper.toResponseAvailableBalance(404, Constants.ERROR_FIND_TRANSACTION)));
    }

    @Override
    public Mono<ResponseEntity<Flux<BalanceSummaryResponse>>> generateMonthlyBalanceSummary(
            String customerId, ServerWebExchange exchange) {

        return reportingService.generateMonthlyBalanceSummary(customerId)
                .map(report -> TransactionMapper.toResponseBalanceSummary(
                        report, 200, Constants.SUCCESS_GET_BALANCE
                ))
                .collectList()
                .flatMap(list -> {
                    if (list.isEmpty()) {
                        return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .body(Flux.empty()));
                    }
                    return Mono.just(ResponseEntity.ok(Flux.fromIterable(list)));
                });
    }

    @Override
    public Mono<ResponseEntity<Flux<CommissionReportResponse>>> generateCommissionReports(
            String customerId,
            OffsetDateTime start,
            OffsetDateTime end,
            ServerWebExchange exchange) {

        return reportingService.generateCommissionReports(customerId, start, end)
                .map(report -> TransactionMapper.toResponseCommissionReport(
                        report, 200, Constants.SUCCESS_GET_BALANCE))
                .collectList()
                .flatMap(list -> {
                    if (list.isEmpty()) {
                        return Mono.just(ResponseEntity
                                .status(HttpStatus.NOT_FOUND)
                                .body(Flux.empty()));
                    }
                    return Mono.just(ResponseEntity.ok(Flux.fromIterable(list)));
                });
    }

}
