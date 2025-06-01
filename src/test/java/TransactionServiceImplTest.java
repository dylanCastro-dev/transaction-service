
import com.nttdata.transaction.model.Details.SavingsAccount;
import com.nttdata.transaction.model.Dto.BankProductDTO;
import com.nttdata.transaction.model.Dto.BankProductResponse;
import com.nttdata.transaction.model.Transaction;
import com.nttdata.transaction.model.Type.ProductType;
import com.nttdata.transaction.model.Type.TransactionType;
import com.nttdata.transaction.repository.TransactionRepository;
import com.nttdata.transaction.service.ProductService;
import com.nttdata.transaction.service.impl.MonthlyTasksServiceImpl;
import com.nttdata.transaction.service.impl.ProductServiceImpl;
import com.nttdata.transaction.service.impl.ReportingServiceImpl;
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
    @Mock
    private ProductServiceImpl productService;

    @InjectMocks
    private TransactionServiceImpl transactionService;

    @InjectMocks
    private MonthlyTasksServiceImpl monthlyTasksService;

    @BeforeEach
    void setup() {
        repository = mock(TransactionRepository.class);
        productService = mock(ProductServiceImpl.class);
        transactionService = new TransactionServiceImpl(repository, productService);

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
                .details(new SavingsAccount(10.0, 10, 0.0, 0, 0.0))
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
        StepVerifier.create(transactionService.getAll()).expectNext(transaction).verifyComplete();
    }

    @Test
    void testGetById() {
        Transaction transaction = new Transaction();
        Mockito.when(repository.findById("123")).thenReturn(Mono.just(transaction));
        StepVerifier.create(transactionService.getById("123")).expectNext(transaction).verifyComplete();
    }

    @Test
    void testGetByProductId() {
        Transaction transaction = new Transaction();
        Mockito.when(repository.findByProductId("prod-1")).thenReturn(Flux.just(transaction));
        StepVerifier.create(transactionService.getByProductId("prod-1")).expectNext(transaction).verifyComplete();
    }

    @Test
    void testDelete() {
        Mockito.when(repository.deleteById("123")).thenReturn(Mono.empty());
        StepVerifier.create(transactionService.delete("123")).verifyComplete();
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
                .details(new SavingsAccount(10.0, 10, 0.0, 0, 0.0))
                .holders(List.of("holder1"))
                .signers(List.of("signer1"))
                .build();

        // Respuesta simulada del product-service
        BankProductResponse mockResponse = new BankProductResponse();
        mockResponse.setProducts(List.of(product));

        // Mocks de dependencias
        TransactionRepository repository = Mockito.mock(TransactionRepository.class);
        ProductService productService = Mockito.mock(ProductService.class);

        // Simulación de métodos del ProductService
        Mockito.when(productService.fetchProductById("prod-1")).thenReturn(Mono.just(mockResponse));
        Mockito.when(productService.updateProduct(any(BankProductDTO.class))).thenReturn(Mono.just(mockResponse));

        // Simulación del guardado de la transacción
        Mockito.when(repository.save(any(Transaction.class))).thenReturn(Mono.just(tx));
        Mockito.when(repository.findByProductIdAndDateTimeBetween(anyString(), any(), any())).thenReturn(Flux.empty());

        // Instanciar el servicio
        TransactionServiceImpl transactionService = new TransactionServiceImpl(repository, productService);

        // Ejecutar y verificar
        StepVerifier.create(transactionService.create(tx))
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

        StepVerifier.create(transactionService.update("tx-1", original)).expectNextCount(1).verifyComplete();
    }

    @Test
    void testGetAvailableBalance() {
        // Simular producto bancario (no tarjeta de crédito)
        BankProductDTO product = BankProductDTO.builder()
                .id("prod-1")
                .type(ProductType.SAVINGS)
                .balance(BigDecimal.valueOf(850))
                .build();

        BankProductResponse mockResponse = new BankProductResponse();
        mockResponse.setProducts(List.of(product));

        // Mock del ProductService
        ProductService productService = Mockito.mock(ProductService.class);
        Mockito.when(productService.fetchProductById("prod-1")).thenReturn(Mono.just(mockResponse));

        // Crear instancia del servicio que se está probando (TransactionServiceImpl)
        ReportingServiceImpl reportingService = new ReportingServiceImpl(repository, productService);

        // Verificar resultado
        StepVerifier.create(reportingService.generateReportAvailableBalance("prod-1"))
                .expectNextMatches(dto ->
                        dto.getProductId().equals("prod-1") &&
                                dto.getAvailableBalance().compareTo(BigDecimal.valueOf(850)) == 0 &&
                                dto.getProductCategory().equals("SAVINGS")
                )
                .verifyComplete();
    }

    @Test
    void testApplyMonthlyTasks() {
        // Simular producto con saldo suficiente y comisión de mantenimiento
        BankProductDTO product = BankProductDTO.builder()
                .id("prod-1")
                .customerId("cust-1")
                .type(ProductType.SAVINGS)
                .name("Cuenta de Ahorro")
                .balance(BigDecimal.valueOf(1000))
                .details(new SavingsAccount(10.0, 10, 0.0, 0, 0.0))
                .holders(List.of("holder1"))
                .signers(List.of("signer1"))
                .build();

        // Simular respuesta GET /products
        BankProductResponse mockGetResponse = Mockito.mock(BankProductResponse.class);
        Mockito.when(mockGetResponse.getProducts()).thenReturn(List.of(product));

        // Simular respuesta PUT /products/{id}
        BankProductResponse mockPutResponse = Mockito.mock(BankProductResponse.class);
        Mockito.when(mockPutResponse.getProducts()).thenReturn(List.of(product));

        // Simular transacción a guardar
        Transaction tx = Transaction.builder()
                .productId("prod-1")
                .amount(BigDecimal.valueOf(10))
                .type(TransactionType.MAINTENANCE)
                .dateTime(LocalDateTime.now())
                .build();

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

        // Inyectar WebClient simulado en Utils
        Utils.setProductService(() -> webClient);

        // Mock repository
        TransactionRepository repository = Mockito.mock(TransactionRepository.class);
        Mockito.when(repository.save(any(Transaction.class))).thenReturn(Mono.just(tx));
        Mockito.when(repository.findByProductIdAndDateTimeBetween(anyString(), any(), any()))
                .thenReturn(Flux.just()); // sin transacciones, no bloquea

        // Mock productService
        ProductService productService = Mockito.mock(ProductService.class);
        Mockito.when(productService.getAllBankProducts()).thenReturn(Flux.just(product));
        Mockito.when(productService.updateProduct(any(BankProductDTO.class)))
                .thenReturn(Mono.just(mockPutResponse));

        // Crear instancia del servicio con los mocks
        MonthlyTasksServiceImpl monthlyTasksService = new MonthlyTasksServiceImpl(repository, productService);

        // Ejecutar y verificar
        StepVerifier.create(monthlyTasksService.applyMonthlyTasks())
                .verifyComplete();
    }

}
