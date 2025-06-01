package com.nttdata.transaction.service;

import com.nttdata.transaction.model.Dto.AvailableBalanceDTO;
import reactor.core.publisher.Mono;

public interface ReportingService {
    /**
     * Obtiene el saldo disponible de un producto bancario.
     * Para productos de crédito, calcula el disponible como límite menos deuda actual.
     *
     * @param productId Identificador del producto bancario
     * @return Mono con el saldo disponible
     */
    Mono<AvailableBalanceDTO> generateReportAvailableBalance(String productId);
}
