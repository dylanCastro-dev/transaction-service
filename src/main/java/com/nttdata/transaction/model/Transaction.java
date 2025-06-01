package com.nttdata.transaction.model;

import com.nttdata.transaction.model.Type.TransactionType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Document(collection = "transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Entidad que representa una transacción bancaria")
public class Transaction {

    @Id
    @Schema(description = "ID único de la transacción", example = "663a8a9d1", accessMode = Schema.AccessMode.READ_ONLY)
    private String id;

    @Schema(description = "ID del producto sobre el que se realiza la transacción", example = "prod12", required = true)
    private String productId;

    @Schema(description = "Tipo de transacción", example = "DEPOSIT", required = true)
    private TransactionType type;

    @Schema(description = "Monto de la transacción", example = "250.0", required = true)
    private BigDecimal amount;

    @Schema(description = "Fecha y hora de la transacción", example = "2024-05-20T10:30:00")
    private LocalDateTime dateTime;
}
