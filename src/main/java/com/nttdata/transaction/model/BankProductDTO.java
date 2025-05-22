package com.nttdata.transaction.model;

import com.nttdata.transaction.model.ProductType;
import lombok.Data;

@Data
public class BankProductDTO {
    private String id;
    private String customerId;
    private ProductType type;
    private Double balance;
    private Double creditLimit;
}