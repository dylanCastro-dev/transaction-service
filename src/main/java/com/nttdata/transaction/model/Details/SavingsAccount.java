package com.nttdata.transaction.model.Details;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.TypeAlias;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TypeAlias("savingsAccount")
public class SavingsAccount implements ProductDetails {
    private Double maintenanceFee;
    private Integer monthlyLimit;
    private Double requiredMonthlyAverageBalance;
}