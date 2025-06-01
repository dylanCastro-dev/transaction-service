package com.nttdata.transaction.model.Details;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.TypeAlias;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TypeAlias("creditProduct")
public class CreditProduct implements ProductDetails {
    private BigDecimal creditLimit;
}