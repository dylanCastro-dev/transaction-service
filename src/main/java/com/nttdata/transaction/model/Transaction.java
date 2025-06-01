package com.nttdata.transaction.model;

import com.nttdata.transaction.model.Type.TransactionType;
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
public class Transaction {

    @Id
    private String id;

    private String productId;

    private TransactionType type;

    private BigDecimal amount;

    private LocalDateTime dateTime;

    private Double transactionFee;
}
