package com.nttdata.transaction.service.impl;

import com.nttdata.transaction.model.Dto.BankProductDTO;
import com.nttdata.transaction.model.Dto.BankProductResponse;
import com.nttdata.transaction.model.Dto.CardProductResponse;
import com.nttdata.transaction.service.ProductService;
import com.nttdata.transaction.utils.Constants;
import com.nttdata.transaction.utils.exceptions.EmptyResultException;
import com.nttdata.transaction.utils.Utils;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {
    private static final Logger log = LoggerFactory.getLogger(ProductServiceImpl.class);

    @Override
    public Mono<CardProductResponse> getCardById(String cardId) {
        return Utils.getProductService().get().get()
                .uri("/debit-cards/{id}", cardId)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response -> {
                    if (response.statusCode() == HttpStatus.NOT_FOUND) {
                        return Mono.error(new EmptyResultException(Constants.ERROR_FIND_PRODUCT));
                    }
                    return Mono.error(new RuntimeException("Error en la solicitud: " + response.statusCode()));
                })
                .bodyToMono(CardProductResponse.class);
    }

    @Override
    public Mono<BankProductResponse> getProductById(String productId) {
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
    public Mono<BankProductResponse> getProductByCustomerId(String customerId) {
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
    public Mono<BankProductResponse> getAllBankProducts() {
        return Utils.getProductService().get().get()
                .uri("/products")
                .retrieve()
                .bodyToMono(BankProductResponse.class);
    }
}
