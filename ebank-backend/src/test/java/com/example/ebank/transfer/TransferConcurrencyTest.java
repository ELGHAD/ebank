package com.example.ebank.transfer;

import com.example.ebank.IntegrationTestSupport;
import com.example.ebank.config.AppUserPrincipal;
import com.example.ebank.entities.Organization;
import com.example.ebank.exception.BusinessRuleException;
import com.example.ebank.transfer.TransferDtos.TransferRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Real concurrency against PostgreSQL: requests are released at the same instant from many threads.
 * These tests would fail without the row locks (lost updates / negative balances) or without a consistent
 * lock order (deadlocks between opposite transfers).
 */
class TransferConcurrencyTest extends IntegrationTestSupport {

    private static final int THREADS = 20;

    @Autowired TransferService transferService;

    Organization org;
    String agentToken;

    @BeforeEach
    void setUp() throws Exception {
        org = newOrganization();
        agentToken = agentToken(org);
    }

    private record Owner(AppUserPrincipal principal, AccountInfo account) {
    }

    private Owner newOwner(String deposit) throws Exception {
        OnboardedClient client = onboardClient(agentToken);
        AccountInfo account = newAccount(agentToken, client.id(), deposit);
        Long userId = userRepo.findByUsername(client.username()).orElseThrow().getId();
        return new Owner(new AppUserPrincipal(userId, client.username(), org.getId()), account);
    }

    /** Runs all tasks at the same instant and returns each outcome (result or exception). */
    private static List<Object> runConcurrently(List<Supplier<Object>> tasks) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(tasks.size());
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<Object>> futures = new ArrayList<>();
            for (Supplier<Object> task : tasks) {
                futures.add(pool.submit(() -> {
                    start.await();
                    try {
                        return task.get();
                    } catch (RuntimeException e) {
                        return e;
                    }
                }));
            }
            start.countDown();
            List<Object> outcomes = new ArrayList<>();
            for (Future<Object> f : futures) {
                outcomes.add(f.get(60, TimeUnit.SECONDS));
            }
            return outcomes;
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void concurrentDebitsNeverOverdrawTheAccount() throws Exception {
        Owner payer = newOwner("100.00");
        Owner payee = newOwner("0");

        // 20 simultaneous transfers of 10.00 from an account holding 100.00
        List<Supplier<Object>> tasks = new ArrayList<>();
        for (int i = 0; i < THREADS; i++) {
            tasks.add(() -> transferService.transfer(payer.principal(), UUID.randomUUID().toString(),
                    new TransferRequest(payer.account().id(), payee.account().rib(), new BigDecimal("10.00"), null)));
        }
        List<Object> outcomes = runConcurrently(tasks);

        long succeeded = outcomes.stream().filter(o -> o instanceof TransferService.TransferResult).count();
        List<Object> failures = outcomes.stream().filter(o -> !(o instanceof TransferService.TransferResult)).toList();

        assertThat(succeeded).isEqualTo(10);
        assertThat(failures).hasSize(10).allSatisfy(f -> assertThat(f)
                .isInstanceOfSatisfying(BusinessRuleException.class,
                        e -> assertThat(e.getCode()).isEqualTo("INSUFFICIENT_FUNDS")));
        assertThat(balanceOf(payer.account().id())).isEqualByComparingTo("0.00");
        assertThat(balanceOf(payee.account().id())).isEqualByComparingTo("100.00");
        assertBooksBalanced();
    }

    @Test
    void oppositeTransfersDoNotDeadlockAndConserveMoney() throws Exception {
        Owner a = newOwner("1000.00");
        Owner b = newOwner("1000.00");

        // A->B and B->A at the same time: without a global lock order this is the classic deadlock
        List<Supplier<Object>> tasks = new ArrayList<>();
        for (int i = 0; i < THREADS; i++) {
            Owner from = i % 2 == 0 ? a : b;
            Owner to = i % 2 == 0 ? b : a;
            tasks.add(() -> transferService.transfer(from.principal(), UUID.randomUUID().toString(),
                    new TransferRequest(from.account().id(), to.account().rib(), new BigDecimal("7.25"), null)));
        }
        List<Object> outcomes = runConcurrently(tasks);

        assertThat(outcomes).allSatisfy(o -> assertThat(o).isInstanceOf(TransferService.TransferResult.class));
        // Same number of transfers each way: both balances back where they started, total conserved
        assertThat(balanceOf(a.account().id())).isEqualByComparingTo("1000.00");
        assertThat(balanceOf(b.account().id())).isEqualByComparingTo("1000.00");
        assertBooksBalanced();
    }

    @Test
    void concurrentRetriesWithTheSameKeyExecuteOnce() throws Exception {
        Owner payer = newOwner("500.00");
        Owner payee = newOwner("0");
        String key = UUID.randomUUID().toString();
        var request = new TransferRequest(payer.account().id(), payee.account().rib(), new BigDecimal("50"), "Once");

        List<Supplier<Object>> tasks = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            tasks.add(() -> transferService.transfer(payer.principal(), key, request));
        }
        List<Object> outcomes = runConcurrently(tasks);

        assertThat(outcomes).allSatisfy(o -> assertThat(o).isInstanceOf(TransferService.TransferResult.class));
        List<TransferService.TransferResult> results = outcomes.stream()
                .map(TransferService.TransferResult.class::cast).toList();
        assertThat(results.stream().map(r -> r.transfer().id()).distinct()).hasSize(1);
        assertThat(results.stream().filter(r -> !r.replayed())).hasSize(1);
        assertThat(balanceOf(payer.account().id())).isEqualByComparingTo("450.00");
        assertThat(balanceOf(payee.account().id())).isEqualByComparingTo("50.00");
        assertBooksBalanced();
    }
}
