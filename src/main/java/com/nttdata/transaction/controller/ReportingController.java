package com.nttdata.transaction.controller;

import com.nttdata.transaction.service.ReportingService;
import com.nttdata.transaction.utils.Constants;
import com.nttdata.transaction.utils.TransactionMapper;
import lombok.RequiredArgsConstructor;
import org.openapitools.api.ReportApi;
import org.openapitools.model.AvailableBalanceResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
public class ReportingController implements ReportApi {
    private final ReportingService reportingService;

    @Override
    public Mono<ResponseEntity<AvailableBalanceResponse>> generateReportAvailableBalance
            (String productId, ServerWebExchange exchange) {
        return reportingService.generateReportAvailableBalance(productId)
                .map(balance ->
                        TransactionMapper.toResponseAvailableBalance(balance,
                                200,
                                Constants.SUCCESS_GET_BALANCE))
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(TransactionMapper.toResponseAvailableBalance(404, Constants.ERROR_FIND_TRANSACTION)));
    }
}
