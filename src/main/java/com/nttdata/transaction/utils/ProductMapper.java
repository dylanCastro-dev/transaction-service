package com.nttdata.transaction.utils;

import java.util.List;
import java.util.stream.Collectors;

public class ProductMapper {

    public static List<org.openapitools.model.BankProductDTO> toOpenApiList
    (List<com.nttdata.transaction.model.Dto.BankProductDTO> sourceList) {
        return sourceList.stream()
                .map(ProductMapper::toOpenApi)
                .collect(Collectors.toList());
    }

    public static org.openapitools.model.BankProductDTO toOpenApi
    (com.nttdata.transaction.model.Dto.BankProductDTO dto) {
        org.openapitools.model.BankProductDTO result = new org.openapitools.model.BankProductDTO();
        result.setId(dto.getId());
        result.setType(dto.getType().name());
        result.setBalance(dto.getBalance());
        result.setStatus(dto.getStatus().name());
        return result;
    }
}
