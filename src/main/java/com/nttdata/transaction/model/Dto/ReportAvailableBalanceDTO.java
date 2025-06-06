package com.nttdata.transaction.model.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReportAvailableBalanceDTO {

    private String productId;

    private BigDecimal availableBalance;

    private String productCategory;
}
