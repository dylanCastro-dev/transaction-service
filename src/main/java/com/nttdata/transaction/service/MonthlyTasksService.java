package com.nttdata.transaction.service;

import reactor.core.publisher.Mono;

public interface MonthlyTasksService {
    /**
     * Simula una ejecución mensual para aplicar la comisión de mantenimiento
     * a todos los productos que tienen un monto de mantenimiento mayor a 0.
     * Registra una transacción por la comisión y actualiza el saldo del producto.
     */
    public Mono<Void> applyMonthlyTasks();
}
