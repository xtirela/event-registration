// package com.eventreg.integration.concurrency;
//
// import java.util.ArrayList;
// import java.util.List;
// import java.util.concurrent.*;
// import java.util.function.IntFunction;
// import org.springframework.mock.web.MockHttpServletRequest;
// import org.springframework.web.context.request.RequestContextHolder;
// import org.springframework.web.context.request.ServletRequestAttributes;
//
/// **
// * Test helper that launches a batch of tasks concurrently against the shared backend. Each task
// is
// * executed inside a per-thread servlet request context so that {@code @Idempotent} service
// methods
// * (whose aspect reads the current request to look up an {@code Idempotency-Key}) proceed
// normally.
// */
// public final class ConcurrencyTestSupport {
//
//  private ConcurrencyTestSupport() {}
//
//  /**
//   * Executes the given task {@code tasks} times, one per worker thread, releasing all workers
//   * simultaneously via a start gate so their work overlaps. Each invocation reports its result.
//   *
//   * @param tasks number of concurrent executions to launch
//   * @param task function mapping the worker index to a result (exceptions propagate to the
// caller)
//   * @return the result produced by each worker, in index order
//   */
//  public static <T> List<T> runConcurrently(int tasks, IntFunction<T> task) throws Exception {
//    ExecutorService pool = Executors.newFixedThreadPool(tasks);
//    try {
//      CountDownLatch startGate = new CountDownLatch(1);
//      List<Future<T>> futures = new ArrayList<>(tasks);
//      for (int i = 0; i < tasks; i++) {
//        int index = i;
//        futures.add(pool.submit(() -> {
//          startGate.await();
//          return withRequestContext(() -> task.apply(index));
//        }));
//      }
//      startGate.countDown();                       // release all workers together
//      List<T> results = new ArrayList<>(tasks);
//      for (Future<T> future : futures) {
//        results.add(future.get());               // wait for each to finish
//      }
//      return results;
//    } finally {
//      pool.shutdownNow();
//    }
//  }
//
//  private static <T> T withRequestContext(java.util.function.Supplier<T> supplier) {
//    MockHttpServletRequest request = new MockHttpServletRequest();
//    request.setRequestURI("/api/test");
//    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request), true);
//    try {
//      return supplier.get();
//    } finally {
//      RequestContextHolder.resetRequestAttributes();
//    }
//  }
// }
