package com.nttdata.transaction.controller;

import com.nttdata.transaction.model.Dto.AvailableBalanceDTO;
import com.nttdata.transaction.model.Transaction;
import com.nttdata.transaction.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService service;

    @Operation(summary = "Listar todas las transacciones")
    @GetMapping
    public Flux<Transaction> getAll() {
        return service.getAll();
    }

    @Operation(summary = "Obtener transacción por ID")
    @GetMapping("/{id}")
    public Mono<Transaction> getById(
            @Parameter(description = "ID de la transacción", required = true)
            @PathVariable String id) {
        return service.getById(id);
    }

    @Operation(summary = "Consultar saldo disponible de un producto bancario o tarjeta de crédito")
    @GetMapping("/balance/{productId}")
    public Mono<AvailableBalanceDTO> getAvailableBalance(@PathVariable String productId) {
        return service.getAvailableBalance(productId);
    }

    @Operation(summary = "Listar todas las transacciones de un producto bancario")
    @GetMapping("/by-product/{productId}")
    public Flux<Transaction> getByProduct(@PathVariable String productId) {
        return service.getByProductId(productId);
    }



    @Operation(summary = "Registrar una nueva transacción")
    @PostMapping
    public Mono<Transaction> create(
            @Parameter(description = "Datos de la transacción", required = true)
            @RequestBody Transaction transaction) {
        return service.create(transaction);
    }

    @Operation(summary = "Actualizar transacción por ID")
    @PutMapping("/{id}")
    public Mono<Transaction> update(
            @PathVariable String id,
            @RequestBody Transaction transaction) {
        return service.update(id, transaction);
    }

    @Operation(summary = "Eliminar transacción por ID")
    @DeleteMapping("/{id}")
    public Mono<Void> delete(
            @Parameter(description = "ID de la transacción a eliminar", required = true)
            @PathVariable String id) {
        return service.delete(id);
    }

    @Operation(summary = "Listar transacciones por ID de producto")
    @GetMapping("/product/{productId}")
    public Flux<Transaction> getByProductId(
            @Parameter(description = "ID del producto relacionado") @PathVariable String productId) {
        return service.getByProductId(productId);
    }

    @Operation(summary = "Aplicar comisión mensual por mantenimiento a todos los productos con maintenanceFee > 0")
    @PostMapping("/apply-monthly-fee")
    public Mono<String> applyMonthlyFee() {
        return service.applyMonthlyMaintenanceFee()
                .thenReturn("Comisión mensual aplicada correctamente.");
    }
}
