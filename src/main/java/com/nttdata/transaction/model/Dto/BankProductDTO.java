package com.nttdata.transaction.model.Dto;

import com.nttdata.transaction.model.Type.ProductType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class BankProductDTO {
    private String id;
    private String customerId;
    private ProductType type;
    private String name;
    private BigDecimal balance;
    private Double maintenanceFee;
    private Integer monthlyLimit;
    private BigDecimal creditLimit;
    private List<String> holders;
    private List<String> signers;
    private Integer allowedTransactionDay;
}