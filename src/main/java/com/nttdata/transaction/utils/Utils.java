package com.nttdata.transaction.utils;

import com.nttdata.transaction.model.Type.TransactionType;
import org.openapitools.model.TransactionBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.util.function.Supplier;

public class Utils {
    private static final Logger log = LoggerFactory.getLogger(Utils.class);

    private static Supplier<WebClient> productService =
            () -> WebClient.builder()
                    .baseUrl("http://localhost:8081") // URL del customer-service
                    .build();

    public static void setProductService(Supplier<WebClient> supplier) {
        productService = supplier;
    }

    public static Supplier<WebClient> getProductService() {
        return productService;
    }

    public static void validateTransactionBody(TransactionBody body) {
        if (body == null) {
            throw new IllegalArgumentException("El cuerpo de la transacción no puede ser nulo.");
        }

        if (body.getProductId() == null || body.getProductId().trim().isEmpty()) {
            throw new IllegalArgumentException("El ID del producto es obligatorio.");
        }

        if (body.getType() == null || body.getType().trim().isEmpty()) {
            throw new IllegalArgumentException("El tipo de transacción es obligatorio.");
        }

        try {
            TransactionType.valueOf(body.getType());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("El tipo de transacción debe ser DEPOSIT, WITHDRAWAL o PAYMENT.");
        }

        if (body.getAmount() == null) {
            throw new IllegalArgumentException("El monto de la transacción es obligatorio.");
        }

        if (body.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto de la transacción debe ser mayor a 0.");
        }
    }
}
