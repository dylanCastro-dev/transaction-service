package com.nttdata.transaction.service.impl;

import com.nttdata.transaction.model.Details.CreditProduct;
import com.nttdata.transaction.model.Dto.AvailableBalanceDTO;
import com.nttdata.transaction.model.Dto.BankProductDTO;
import com.nttdata.transaction.model.Type.ProductType;
import com.nttdata.transaction.repository.TransactionRepository;
import com.nttdata.transaction.service.ProductService;
import com.nttdata.transaction.service.ReportingService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class ReportingServiceImpl implements ReportingService {
    private static final Logger log = LoggerFactory.getLogger(ReportingServiceImpl.class);
    private final TransactionRepository repository;
    private final ProductService productService;

    @Override
    public Mono<AvailableBalanceDTO> generateReportAvailableBalance(String productId) {
        return  productService.fetchProductById(productId)
                .map(response -> {
                    BankProductDTO product = response.getProducts().get(0);
                    BigDecimal balance = product.getBalance() != null ? product.getBalance() : BigDecimal.ZERO;

                    if (isCreditCard(product.getType())) {
                        CreditProduct detailsCreditProduct = (CreditProduct) product.getDetails();
                        BigDecimal available = detailsCreditProduct.getCreditLimit().subtract(balance);
                        return new AvailableBalanceDTO(product.getId(), available, product.getType().name());
                    } else {
                        return new AvailableBalanceDTO(product.getId(), balance, product.getType().name());
                    }
                });
    }

    private boolean isCreditCard(ProductType type) {
        //Funcion que valida si es una tarjeta de credito
        return type == ProductType.CREDIT;
    }
}
