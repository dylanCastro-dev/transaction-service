package com.nttdata.transaction.model.Dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class BankProductResponse {
    private int status;
    private String message;
    private List<BankProductDTO> products;
}