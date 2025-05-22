package com.nttdata.transaction.service;

import com.nttdata.transaction.model.*;
import com.nttdata.transaction.model.Dto.AvailableBalanceDTO;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


public interface TransactionService {

    /**
     * Obtiene todas las transacciones registradas en el sistema.
     *
     * @return Flux con la lista de transacciones
     */
    Flux<Transaction> getAll();

    /**
     * Busca una transacción por su identificador único.
     *
     * @param id Identificador de la transacción
     * @return Mono con la transacción encontrada o vacío si no existe
     */
    Mono<Transaction> getById(String id);

    /**
     * Obtiene todas las transacciones asociadas a un producto bancario específico.
     *
     * @param productId Identificador del producto bancario
     * @return Flux con las transacciones correspondientes al producto
     */
    Flux<Transaction> getByProductId(String productId);

    /**
     * Actualiza los datos de una transacción existente.
     *
     * @param id Identificador de la transacción a actualizar
     * @param transaction Datos nuevos de la transacción
     * @return Mono con la transacción actualizada
     */
    Mono<Transaction> update(String id, Transaction transaction);

    /**
     * Elimina una transacción por su identificador único.
     *
     * @param id Identificador de la transacción a eliminar
     * @return Mono vacío cuando la eliminación se completa
     */
    Mono<Void> delete(String id);

    /**
     * Obtiene el saldo disponible de un producto bancario.
     * Para productos de crédito, calcula el disponible como límite menos deuda actual.
     *
     * @param productId Identificador del producto bancario
     * @return Mono con el saldo disponible
     */
    Mono<AvailableBalanceDTO> getAvailableBalance(String productId);

    /**
     * Registra una nueva transacción en el sistema, aplicando las reglas de negocio correspondientes
     * según el tipo de producto y tipo de transacción.
     *
     * @param transaction Transacción a registrar
     * @return Mono con la transacción registrada
     */
    Mono<Transaction> create(Transaction transaction);

    /**
     * Simula una ejecución mensual para aplicar la comisión de mantenimiento
     * a todos los productos que tienen un monto de mantenimiento mayor a 0.
     * Registra una transacción por la comisión y actualiza el saldo del producto.
     */
    public Mono<Void> applyMonthlyMaintenanceFee();
}

