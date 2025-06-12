package com.nttdata.transaction.model.Dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CardProductResponse {
    private int status;
    private String message;
    private List<CardProductDTO> products;
}