package com.nttdata.transaction.service.impl;

import com.nttdata.transaction.model.Dto.BankProductDTO;
import com.nttdata.transaction.model.Dto.BankProductResponse;
import com.nttdata.transaction.service.ProductService;
import com.nttdata.transaction.utils.Constants;
import com.nttdata.transaction.utils.EmptyResultException;
import com.nttdata.transaction.utils.Utils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    @Override
    public Mono<BankProductResponse> fetchProductById(String productId) {
        return Utils.getProductService().get().get()
                .uri("/products/{id}", productId)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response -> {
                    if (response.statusCode() == HttpStatus.NOT_FOUND) {
                        return Mono.error(new EmptyResultException(Constants.ERROR_FIND_PRODUCT));
                    }
                    return Mono.error(new RuntimeException("Error en la solicitud: " + response.statusCode()));
                })
                .bodyToMono(BankProductResponse.class);
    }

    @Override
    public Mono<BankProductResponse> fetchProductByCustomerId(String customerId) {
        return Utils.getProductService().get().get()
                .uri("/products/customer/{id}", customerId)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response -> {
                    if (response.statusCode() == HttpStatus.NOT_FOUND) {
                        return Mono.error(new EmptyResultException(Constants.ERROR_FIND_PRODUCT));
                    }
                    return Mono.error(new RuntimeException("Error en la solicitud: " + response.statusCode()));
                })
                .bodyToMono(BankProductResponse.class);
    }

    @Override
    public Mono<BankProductResponse> updateProduct(BankProductDTO product) {
        return Utils.getProductService().get().put()
                .uri("/products/{id}", product.getId())
                .bodyValue(product)
                .retrieve()
                .bodyToMono(BankProductResponse.class);
    }

    @Override
    public Flux<BankProductDTO> getAllBankProducts() {
        return Utils.getProductService().get().get()
                .uri("/products")
                .retrieve()
                .bodyToMono(BankProductResponse.class)
                .flatMapMany(response -> Flux.fromIterable(response.getProducts()));
    }

}
