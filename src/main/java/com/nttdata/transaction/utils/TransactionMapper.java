package com.nttdata.transaction.utils;

import com.nttdata.transaction.model.Dto.AvailableBalanceDTO;
import com.nttdata.transaction.model.Transaction;
import com.nttdata.transaction.model.Type.TransactionType;
import org.openapitools.model.AvailableBalanceResponse;
import org.openapitools.model.AvailableBalanceResponseBalance;
import org.openapitools.model.TemplateResponse;
import org.openapitools.model.TransactionBody;
import org.openapitools.model.TransactionResponse;


import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Clase utilitaria para mapear entre entidades Transaction, DTOs y respuestas.
 */
public class TransactionMapper {

    /**
     * Convierte un TransactionBody a una entidad Transaction.
     */
    public static Transaction toTransaction(TransactionBody body) {
        return Transaction.builder()
                .productId(body.getProductId())
                .type(TransactionType.valueOf(body.getType()))
                .amount(body.getAmount())
                .dateTime(body.getDateTime().toLocalDateTime())
                .build();
    }

    /**
     * Convierte una entidad Transaction a un TransactionBody.
     */
    public static TransactionResponse toTransactionResponse(Transaction transaction) {
        TransactionResponse body = new TransactionResponse();
        body.setId(transaction.getId());
        body.setProductId(transaction.getProductId());
        body.setType(transaction.getType().name());
        body.setAmount(transaction.getAmount());
        body.setDateTime(transaction.getDateTime().atOffset(ZoneOffset.UTC));
        return body;
    }

    /**
     * Construye una respuesta con una sola transacción.
     */
    public static TemplateResponse toResponse(Transaction transaction, int status, String message) {
        return new TemplateResponse()
                .status(status)
                .message(message)
                .addTransactionsItem(toTransactionResponse(transaction));
    }

    /**
     * Construye una respuesta con una lista de transacciones.
     */
    public static TemplateResponse toResponse(List<Transaction> transaction, int status, String message) {
        List<TransactionResponse> bodyList = transaction.stream()
                .map(TransactionMapper::toTransactionResponse)
                .collect(Collectors.toList());

        return new TemplateResponse()
                .status(status)
                .message(message)
                .transactions(bodyList);
    }

    /**
     * Construye una respuesta vacía (usado por ejemplo para delete o not found).
     */
    public static TemplateResponse toResponse(int status, String message) {
        return new TemplateResponse()
                .status(status)
                .message(message)
                .transactions(null);
    }

    public static AvailableBalanceResponse toResponseAvailableBalance(int status, String message) {
        return new AvailableBalanceResponse()
                .status(status)
                .message(message)
                .balance(null);
    }

    public static AvailableBalanceResponse toResponseAvailableBalance(AvailableBalanceDTO dto,
                                                                      int status,
                                                                      String message) {
        return new AvailableBalanceResponse()
                .status(status)
                .message(message)
                .balance(new AvailableBalanceResponseBalance()
                        .productId(dto.getProductId())
                        .availableBalance(dto.getAvailableBalance())
                        .productCategory(dto.getProductCategory()));
    }
}