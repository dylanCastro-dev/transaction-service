package com.nttdata.transaction.controller;

import com.nttdata.transaction.service.MonthlyTasksService;
import com.nttdata.transaction.service.TransactionService;
import com.nttdata.transaction.utils.Constants;
import com.nttdata.transaction.utils.TransactionMapper;
import com.nttdata.transaction.utils.Utils;
import lombok.RequiredArgsConstructor;
import org.openapitools.api.TransactionsApi;
import org.openapitools.model.TemplateResponse;
import org.openapitools.model.TransactionBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
public class TransactionController implements TransactionsApi {

    private static final Logger log = LoggerFactory.getLogger(TransactionController.class);

    private final TransactionService transactionService;
    private final MonthlyTasksService monthlyTasksService;

    @Override
    public Mono<ResponseEntity<TemplateResponse>> getAllTransactions(ServerWebExchange exchange) {
        return transactionService.getAll()
                .collectList()
                .map(transactions ->
                        TransactionMapper.toResponse(transactions,
                                200,
                                Constants.SUCCESS_FIND_LIST_TRANSACTION))
                .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<TemplateResponse>> getTransactionById(String id, ServerWebExchange exchange) {
        return transactionService.getById(id)
                .map(transaction ->
                        TransactionMapper.toResponse(transaction,
                                200,
                                Constants.SUCCESS_FIND_TRANSACTION))
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(TransactionMapper.toResponse(404, Constants.ERROR_FIND_TRANSACTION)));
    }

    @Override
    public Mono<ResponseEntity<TemplateResponse>> createTransaction(
            @RequestBody Mono<TransactionBody> request, ServerWebExchange exchange) {

        return request
                .doOnNext(req -> log.debug("Request recibido: {}", req))
                .doOnNext(Utils::validateTransactionBody)
                .map(TransactionMapper::toTransaction)
                .flatMap(transactionService::create)
                .map(created -> TransactionMapper.toResponse(created,
                        201,
                        Constants.SUCCESS_CREATE_TRANSACTION))
                .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<TemplateResponse>> updateTransaction(
            String id, @RequestBody Mono<TransactionBody> request, ServerWebExchange exchange) {

        return request
                .doOnNext(req -> log.debug("Request recibido: {}", req))
                .doOnNext(Utils::validateTransactionBody)
                .map(TransactionMapper::toTransaction)
                .flatMap(transaction -> transactionService.update(id, transaction))
                .map(updated -> TransactionMapper.toResponse(updated,
                        200,
                        Constants.SUCCESS_UPDATE_TRANSACTION))
                .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<TemplateResponse>> deleteTransaction(String id, ServerWebExchange exchange) {
        return transactionService.delete(id)
                .thenReturn(TransactionMapper.toResponse(200,
                        Constants.SUCCESS_DELETE_TRANSACTION))
                .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<TemplateResponse>> getTransactionsByProduct
            (String productId, ServerWebExchange exchange) {
        return transactionService.getByProductId(productId)
                .collectList()
                .map(transactions ->
                        TransactionMapper.toResponse(transactions,
                                200,
                                Constants.SUCCESS_FIND_LIST_TRANSACTION_BY_PRODUCT))
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(TransactionMapper.toResponse(404, Constants.ERROR_FIND_TRANSACTION)));
    }

    @Override
    public Mono<ResponseEntity<TemplateResponse>> applyMonthlyTasks(ServerWebExchange exchange) {
        return monthlyTasksService.applyMonthlyTasks()
                .map(transactions ->
                        TransactionMapper.toResponse(200,
                                Constants.SUCCESS_APPLY_MONTHLY_FEE))
                .map(ResponseEntity::ok);
    }
}