package com.nttdata.transaction.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Información del saldo disponible de un producto bancario")
public class AvailableBalanceDTO {

    @Schema(description = "ID del producto", example = "663018e0ac82a12a8445a9b0")
    private String productId;

    @Schema(description = "Saldo disponible", example = "2450.75")
    private Double availableBalance;

    @Schema(description = "Categoría del producto (CREDIT o BANK)", example = "CREDIT")
    private String productCategory;
}
