package com.nttdata.transaction.service;

import com.nttdata.transaction.model.Dto.BankProductDTO;
import com.nttdata.transaction.model.Dto.BankProductResponse;
import com.nttdata.transaction.model.Dto.CardProductResponse;
import reactor.core.publisher.Mono;

public interface ProductService {
    /**
     * Busca un producto por Id.
     *
     * @return Mono con el producto obtenido
     */
    public Mono<BankProductResponse> getProductById(String productId);


    /**
     * Busca un producto por CustomerId.
     *
     * @return Mono con el producto obtenido
     */
    Mono<BankProductResponse> getProductByCustomerId(String customerId);


    /**
     * Actualiza un producto.
     *
     * @return Mono con el producto actualizado
     */
    Mono<BankProductResponse> updateProduct(BankProductDTO product);


    /**
     * Obtiene todos los productos.
     *
     * @return Mono con la lista de productos obtenidos
     */
    Mono<BankProductResponse> getAllBankProducts();


    /**
     * Obtiene todos los productos.
     *
     * @return Mono con la lista de productos obtenidos
     */
    Mono<CardProductResponse> getCardById(String cardId);
}
