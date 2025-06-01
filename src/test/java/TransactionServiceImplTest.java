
import com.nttdata.transaction.model.Details.SavingsAccount;
import com.nttdata.transaction.model.Dto.BankProductDTO;
import com.nttdata.transaction.model.Dto.BankProductResponse;
import com.nttdata.transaction.model.Transaction;
import com.nttdata.transaction.model.Type.ProductType;
import com.nttdata.transaction.model.Type.TransactionType;
import com.nttdata.transaction.repository.TransactionRepository;
import com.nttdata.transaction.service.impl.TransactionServiceImpl;
import com.nttdata.transaction.utils.Utils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;


public class TransactionServiceImplTest {

    @Mock
    private TransactionRepository repository;

    @InjectMocks
    private TransactionServiceImpl service;

    @BeforeEach
    void setup() {
        repository = mock(TransactionRepository.class);
        service = new TransactionServiceImpl(repository);

        // Mock del WebClient y sus componentes
        WebClient.RequestHeadersUriSpec uriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec headersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);
        WebClient.RequestBodyUriSpec bodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec bodySpec = mock(WebClient.RequestBodySpec.class);
        WebClient client = mock(WebClient.class);

        // Objeto de producto simulado
        BankProductDTO product = BankProductDTO.builder()
                .id("prod-1")
                .customerId("cust-1")
                .type(ProductType.SAVINGS)
                .name("Cuenta de Ahorro")
                .balance(BigDecimal.valueOf(1000))
                .details(new SavingsAccount(10.0, 10)) // ✅ aquí colocas los campos específicos
                .holders(List.of("holder1"))
                .signers(List.of("signer1"))
                .build();


        // Respuesta simulada
        BankProductResponse mockResponse = mock(BankProductResponse.class);
        Mockito.when(mockResponse.getProducts()).thenReturn(List.of(product));

        // Comportamiento del WebClient GET
        Mockito.when(client.get()).thenReturn(uriSpec);
        Mockito.when(uriSpec.uri(anyString(), (Object[]) any())).thenReturn(headersSpec);
        Mockito.when(headersSpec.retrieve()).thenReturn(responseSpec);
        Mockito.when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        Mockito.when(responseSpec.bodyToMono(eq(BankProductResponse.class))).thenReturn(Mono.just(mockResponse));

        // Comportamiento del WebClient PUT
        Mockito.when(client.put()).thenReturn(bodyUriSpec);
        Mockito.when(uriSpec.uri(anyString(), (Object[]) any())).thenReturn(bodySpec);
        Mockito.when(bodySpec.bodyValue(any())).thenReturn(headersSpec);
        Mockito.when(responseSpec.bodyToMono(eq(BankProductResponse.class))).thenReturn(Mono.just(mockResponse));

        // Inyectar mock en Utils
        Utils.setProductService(() -> client);
    }


    @Test
    void testGetAll() {
        Transaction transaction = new Transaction();
        Mockito.when(repository.findAll()).thenReturn(Flux.just(transaction));
        StepVerifier.create(service.getAll()).expectNext(transaction).verifyComplete();
    }

    @Test
    void testGetById() {
        Transaction transaction = new Transaction();
        Mockito.when(repository.findById("123")).thenReturn(Mono.just(transaction));
        StepVerifier.create(service.getById("123")).expectNext(transaction).verifyComplete();
    }

    @Test
    void testGetByProductId() {
        Transaction transaction = new Transaction();
        Mockito.when(repository.findByProductId("prod-1")).thenReturn(Flux.just(transaction));
        StepVerifier.create(service.getByProductId("prod-1")).expectNext(transaction).verifyComplete();
    }

    @Test
    void testDelete() {
        Mockito.when(repository.deleteById("123")).thenReturn(Mono.empty());
        StepVerifier.create(service.delete("123")).verifyComplete();
    }

    @Test
    void testCreate_withDepositTransaction_shouldSucceed() {
        // Transacción simulada
        Transaction tx = Transaction.builder()
                .id("tx-1")
                .productId("prod-1")
                .type(TransactionType.DEPOSIT)
                .amount(BigDecimal.valueOf(500))
                .build();

        // Producto simulado (tipo bancario)
        BankProductDTO product = BankProductDTO.builder()
                .id("prod-1")
                .customerId("cust-1")
                .type(ProductType.SAVINGS)
                .name("Cuenta de Ahorro")
                .balance(BigDecimal.valueOf(1000))
                .details(new SavingsAccount(10.0, 10)) // ✅ aquí colocas los campos específicos
                .holders(List.of("holder1"))
                .signers(List.of("signer1"))
                .build();

        BankProductResponse mockResponse = Mockito.mock(BankProductResponse.class);
        Mockito.when(mockResponse.getProducts()).thenReturn(List.of(product));

        // WebClient mocks
        WebClient.RequestHeadersUriSpec uriSpec = Mockito.mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec headersSpec = Mockito.mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = Mockito.mock(WebClient.ResponseSpec.class);
        WebClient.RequestBodyUriSpec putUriSpec = Mockito.mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec putBodySpec = Mockito.mock(WebClient.RequestBodySpec.class);
        WebClient.RequestHeadersSpec putHeadersSpec = Mockito.mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec putResponseSpec = Mockito.mock(WebClient.ResponseSpec.class);
        WebClient client = Mockito.mock(WebClient.class);

        // GET /products/{id}
        Mockito.when(client.get()).thenReturn(uriSpec);
        Mockito.when(uriSpec.uri(eq("/products/{id}"), eq("prod-1"))).thenReturn(headersSpec);
        Mockito.when(headersSpec.retrieve()).thenReturn(responseSpec);
        Mockito.when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        Mockito.when(responseSpec.bodyToMono(eq(BankProductResponse.class))).thenReturn(Mono.just(mockResponse));

        // PUT /products/{id}
        Mockito.when(client.put()).thenReturn(putUriSpec);
        Mockito.when(putUriSpec.uri(eq("/products/{id}"), eq("prod-1"))).thenReturn(putBodySpec);
        Mockito.when(putBodySpec.bodyValue(any())).thenReturn(putHeadersSpec);
        Mockito.when(putHeadersSpec.retrieve()).thenReturn(putResponseSpec);
        Mockito.when(putResponseSpec.bodyToMono(eq(BankProductResponse.class))).thenReturn(Mono.just(mockResponse));

        // Inyectar WebClient simulado
        Utils.setProductService(() -> client);

        // Mock de validación mensual y guardado
        Mockito.when(repository.findByProductIdAndDateTimeBetween(anyString(), any(), any()))
                .thenReturn(Flux.empty());
        Mockito.when(repository.save(any(Transaction.class))).thenReturn(Mono.just(tx));

        // Ejecutar y verificar
        StepVerifier.create(service.create(tx))
                .expectNextMatches(result ->
                        result.getProductId().equals("prod-1") &&
                                result.getType() == TransactionType.DEPOSIT &&
                                result.getAmount().compareTo(BigDecimal.valueOf(500)) == 0
                )
                .verifyComplete();
    }

    @Test
    void testUpdate() {
        Transaction original = Transaction.builder()
                .type(TransactionType.WITHDRAWAL)
                .amount(BigDecimal.valueOf(100))
                .productId("prod-1")
                .build();
        Mockito.when(repository.findById("tx-1")).thenReturn(Mono.just(original));
        Mockito.when(repository.save(any(Transaction.class))).thenReturn(Mono.just(original));

        StepVerifier.create(service.update("tx-1", original)).expectNextCount(1).verifyComplete();
    }

    @Test
    void testGetAvailableBalance() {
        // Simular producto con saldo bancario
        BankProductDTO product = BankProductDTO.builder()
                .id("prod-1")
                .type(ProductType.SAVINGS)
                .balance(BigDecimal.valueOf(850))
                .build();

        BankProductResponse mockResponse = Mockito.mock(BankProductResponse.class);
        Mockito.when(mockResponse.getProducts()).thenReturn(List.of(product));

        // Mocks del WebClient
        WebClient.RequestHeadersUriSpec uriSpec = Mockito.mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec headersSpec = Mockito.mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = Mockito.mock(WebClient.ResponseSpec.class);
        WebClient client = Mockito.mock(WebClient.class);

        // Comportamiento del WebClient GET /products/{id}
        Mockito.when(client.get()).thenReturn(uriSpec);
        Mockito.when(uriSpec.uri(eq("/products/{id}"), eq("prod-1"))).thenReturn(headersSpec);
        Mockito.when(headersSpec.retrieve()).thenReturn(responseSpec);
        Mockito.when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        Mockito.when(responseSpec.bodyToMono(eq(BankProductResponse.class))).thenReturn(Mono.just(mockResponse));

        // Inyectar el WebClient simulado
        Utils.setProductService(() -> client);

        // Ejecutar
        StepVerifier.create(service.getAvailableBalance("prod-1"))
                .expectNextMatches(dto ->
                        dto.getProductId().equals("prod-1") &&
                                dto.getAvailableBalance().compareTo(BigDecimal.valueOf(850)) == 0 &&
                                dto.getProductCategory().equals("BANK")
                )
                .verifyComplete();
    }


    @Test
    void testApplyMonthlyMaintenanceFee() {
        // Producto con saldo suficiente y comisión
        BankProductDTO product = BankProductDTO.builder()
                .id("prod-1")
                .customerId("cust-1")
                .type(ProductType.SAVINGS)
                .name("Cuenta de Ahorro")
                .balance(BigDecimal.valueOf(1000))
                .details(new SavingsAccount(10.0, 10)) // ✅ aquí colocas los campos específicos
                .holders(List.of("holder1"))
                .signers(List.of("signer1"))
                .build();

        // Simular respuesta GET /products
        BankProductResponse mockGetResponse = Mockito.mock(BankProductResponse.class);
        Mockito.when(mockGetResponse.getProducts()).thenReturn(List.of(product));

        // Simular respuesta PUT /products/{id}
        BankProductResponse mockPutResponse = Mockito.mock(BankProductResponse.class);
        Mockito.when(mockPutResponse.getProducts()).thenReturn(List.of(product));

        // WebClient mocks
        WebClient.RequestHeadersUriSpec getUriSpec = Mockito.mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec getHeadersSpec = Mockito.mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec getResponseSpec = Mockito.mock(WebClient.ResponseSpec.class);

        WebClient.RequestBodyUriSpec putUriSpec = Mockito.mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec putBodySpec = Mockito.mock(WebClient.RequestBodySpec.class);
        WebClient.RequestHeadersSpec putHeadersSpec = Mockito.mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec putResponseSpec = Mockito.mock(WebClient.ResponseSpec.class);

        WebClient webClient = Mockito.mock(WebClient.class);

        // Mock GET
        Mockito.when(webClient.get()).thenReturn(getUriSpec);
        Mockito.when(getUriSpec.uri(eq("/products"))).thenReturn(getHeadersSpec);
        Mockito.when(getHeadersSpec.retrieve()).thenReturn(getResponseSpec);
        Mockito.when(getResponseSpec.bodyToMono(eq(BankProductResponse.class))).thenReturn(Mono.just(mockGetResponse));

        // Mock PUT
        Mockito.when(webClient.put()).thenReturn(putUriSpec);
        Mockito.when(putUriSpec.uri(eq("/products/{id}"), eq("prod-1"))).thenReturn(putBodySpec);
        Mockito.when(putBodySpec.bodyValue(any())).thenReturn(putHeadersSpec);
        Mockito.when(putHeadersSpec.retrieve()).thenReturn(putResponseSpec);
        Mockito.when(putResponseSpec.bodyToMono(eq(BankProductResponse.class))).thenReturn(Mono.just(mockPutResponse));

        // Inyectar WebClient simulado
        Utils.setProductService(() -> webClient);

        // Simular guardado de la transacción
        Transaction tx = Transaction.builder()
                .productId("prod-1")
                .amount(BigDecimal.valueOf(10))
                .type(TransactionType.MAINTENANCE)
                .dateTime(LocalDateTime.now())
                .build();

        Mockito.when(repository.save(any(Transaction.class))).thenReturn(Mono.just(tx));

        // Ejecutar y verificar que se completa sin errores
        StepVerifier.create(service.applyMonthlyMaintenanceFee())
                .verifyComplete();
    }

}
