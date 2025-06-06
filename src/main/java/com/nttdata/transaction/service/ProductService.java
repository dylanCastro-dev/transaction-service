package com.nttdata.transaction.service;

import com.nttdata.transaction.model.Dto.BankProductDTO;
import com.nttdata.transaction.model.Dto.BankProductResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ProductService {
    /**
     * Busca un producto por Id.
     *
     * @return Mono con el producto obtenido
     */
    public Mono<BankProductResponse> fetchProductById(String productId);

    Mono<BankProductResponse> fetchProductByCustomerId(String customerId);

    /**
     * Actualiza un producto.
     *
     * @return Mono con el producto actualizado
     */
    Mono<BankProductResponse> updateProduct(BankProductDTO product);

    Flux<BankProductDTO> getAllBankProducts();
}
