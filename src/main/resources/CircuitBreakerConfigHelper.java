package com.nttdata.transaction.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.github.resilience4j.reactor.timelimiter.TimeLimiterOperator;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.function.Function;

/**
 * Clase utilitaria para obtener operadores de Resilience4j con Circuit Breaker y TimeLimiter.
 */
public class CircuitBreakerConfigHelper {

    private static final CircuitBreakerRegistry circuitBreakerRegistry;
    private static final TimeLimiterRegistry timeLimiterRegistry;

    static {
        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .permittedNumberOfCallsInHalfOpenState(3)
                .slidingWindowSize(10)
                .build();

        TimeLimiterConfig timeLimiterConfig = TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofSeconds(2)) // ⏱ Timeout de 2 segundos
                .build();

        circuitBreakerRegistry = CircuitBreakerRegistry.of(circuitBreakerConfig);
        timeLimiterRegistry = TimeLimiterRegistry.of(timeLimiterConfig);
    }

    public static CircuitBreaker getCircuitBreaker(String name) {
        return circuitBreakerRegistry.circuitBreaker(name);
    }

    public static TimeLimiter getTimeLimiter(String name) {
        return timeLimiterRegistry.timeLimiter(name);
    }

    /**
     * Devuelve un operador combinado de Circuit Breaker y TimeLimiter para un flujo Reactor.
     */
    public static <T> Function<Mono<T>, Publisher<T>> applyResilience(String name) {
        CircuitBreaker cb = getCircuitBreaker(name);
        TimeLimiter tl = getTimeLimiter(name);

        return mono -> mono
                .transformDeferred(CircuitBreakerOperator.of(cb))
                .transformDeferred(TimeLimiterOperator.of(tl));
    }
}
