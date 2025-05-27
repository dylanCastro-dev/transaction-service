package com.nttdata.transaction.model.Dto;

import lombok.Data;

import java.util.List;

@Data
public class BankProductResponse {
    private int status;
    private String message;
    private List<BankProductDTO> products;
}