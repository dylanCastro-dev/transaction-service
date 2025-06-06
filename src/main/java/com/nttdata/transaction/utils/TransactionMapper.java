package com.nttdata.transaction.utils;

import com.nttdata.transaction.model.Transaction;
import com.nttdata.transaction.model.Type.TransactionType;
import org.openapitools.model.AvailableBalanceResponse;
import org.openapitools.model.AvailableBalanceResponseBalance;
import org.openapitools.model.BalanceSummaryResponse;
import org.openapitools.model.BalanceSummaryResponseBalanceSummary;
import org.openapitools.model.CommissionReportResponse;
import org.openapitools.model.CommissionReportResponseCommissionReport;
import org.openapitools.model.TemplateResponse;
import org.openapitools.model.TransactionBody;
import org.openapitools.model.TransactionResponse;

import java.math.BigDecimal;
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
                .sourceProductId(body.getSourceProductId())
                .targetProductId(body.getTargetProductId())
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
        body.setSourceProductId(transaction.getSourceProductId());
        body.setTargetProductId(transaction.getTargetProductId());
        body.setType(transaction.getType().name());
        body.setAmount(transaction.getAmount());
        body.setTransactionFee(BigDecimal.valueOf(transaction.getTransactionFee()));
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

    public static AvailableBalanceResponse toResponseAvailableBalance(AvailableBalanceResponseBalance dto,
                                                                      int status,
                                                                      String message) {
        return new AvailableBalanceResponse()
                .status(status)
                .message(message)
                .balance(dto);
    }

    public static BalanceSummaryResponse toResponseBalanceSummary(int status, String message) {
        return new BalanceSummaryResponse()
                .status(status)
                .message(message)
                .balanceSummary(null);
    }

    public static BalanceSummaryResponse toResponseBalanceSummary(BalanceSummaryResponseBalanceSummary dto,
                                                                  int status,
                                                                  String message) {
        return new BalanceSummaryResponse()
                .status(status)
                .message(message)
                .balanceSummary(dto);
    }

    public static CommissionReportResponse toResponseCommissionReport(int status, String message) {
        return new CommissionReportResponse()
                .status(status)
                .message(message)
                .commissionReport(null);
    }

    public static CommissionReportResponse toResponseCommissionReport(CommissionReportResponseCommissionReport dto,
                                                                      int status,
                                                                      String message) {
        return new CommissionReportResponse()
                .status(status)
                .message(message)
                .commissionReport(dto);
    }
}