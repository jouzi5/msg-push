package com.ycc.netty;

import io.netty.util.concurrent.FastThreadLocal;
import io.netty.util.concurrent.FastThreadLocalThread;

/**
 * FastThreadLocal 使用示例
 *
 * FastThreadLocal 是 Netty 提供的高性能线程本地变量实现，
 * 相比 JDK 的 ThreadLocal，它通过数组索引直接访问，避免了哈希冲突，性能更高。
 *
 * 使用要求：线程必须是 FastThreadLocalThread（或其子类），否则会退化为普通 ThreadLocal。
 */
public class Demo2_FastThreadLocal {

    // 1. 定义 FastThreadLocal 变量
    private static final FastThreadLocal<String> USER_CONTEXT = new FastThreadLocal<>();
    private static final FastThreadLocal<Long> REQUEST_ID = new FastThreadLocal<>();

    public static void main(String[] args) throws InterruptedException {
        // 2. 普通 Thread 中使用 FastThreadLocal（退化为普通 ThreadLocal）
        System.out.println("=== 普通 Thread 中使用 FastThreadLocal ===");
        Thread normalThread = new Thread(() -> {
            USER_CONTEXT.set("normal-thread-user");
            REQUEST_ID.set(1001L);
            System.out.println("[普通Thread] USER_CONTEXT = " + USER_CONTEXT.get());
            System.out.println("[普通Thread] REQUEST_ID = " + REQUEST_ID.get());
        });
        normalThread.start();
        normalThread.join();

        // 3. FastThreadLocalThread 中使用 FastThreadLocal（高性能路径）
        System.out.println("\n=== FastThreadLocalThread 中使用 FastThreadLocal ===");
        FastThreadLocalThread fastThread = new FastThreadLocalThread(() -> {
            USER_CONTEXT.set("fast-thread-user");
            REQUEST_ID.set(2002L);
            System.out.println("[FastThread] USER_CONTEXT = " + USER_CONTEXT.get());
            System.out.println("[FastThread] REQUEST_ID = " + REQUEST_ID.get());
        }, "fast-worker-1");
        fastThread.start();
        fastThread.join();

        // 4. 清理 FastThreadLocal
        System.out.println("\n=== 清理 FastThreadLocal ===");
        FastThreadLocalThread cleanupThread = new FastThreadLocalThread(() -> {
            USER_CONTEXT.set("cleanup-user");
            System.out.println("[清理前] USER_CONTEXT = " + USER_CONTEXT.get());
            USER_CONTEXT.remove();
            System.out.println("[清理后] USER_CONTEXT = " + USER_CONTEXT.get());
        }, "cleanup-worker");
        cleanupThread.start();
        cleanupThread.join();

        // 5. 多线程并发使用
        System.out.println("\n=== 多线程并发使用 ===");
        for (int i = 0; i < 5; i++) {
            final int index = i;
            FastThreadLocalThread concurrentThread = new FastThreadLocalThread(() -> {
                USER_CONTEXT.set("user-" + index);
                REQUEST_ID.set((long) (3000 + index));
                System.out.println("[Thread-" + index + "] USER_CONTEXT = " + USER_CONTEXT.get()
                        + ", REQUEST_ID = " + REQUEST_ID.get());
            }, "concurrent-worker-" + index);
            concurrentThread.start();
            concurrentThread.join();
        }

        // 6. 与普通 ThreadLocal 性能对比示意
        System.out.println("\n=== 性能对比说明 ===");
        System.out.println("FastThreadLocal vs ThreadLocal:");
        System.out.println("1. FastThreadLocal 通过数组索引直接定位，O(1) 访问");
        System.out.println("2. ThreadLocal 通过哈希表，存在哈希冲突，性能较低");
        System.out.println("3. 使用 FastThreadLocal 必须配合 FastThreadLocalThread");
        System.out.println("4. Netty 的 NioEventLoop 默认就是 FastThreadLocalThread");

        // 7. FastThreadLocal 模拟实际应用场景：存储用户信息
        System.out.println("\n=== 实际应用场景：请求上下文传递 ===");
        for (int i = 0; i < 3; i++) {
            final int requestId = i;
            FastThreadLocalThread requestThread = new FastThreadLocalThread(() -> {
                // 模拟请求开始：设置上下文
                USER_CONTEXT.set("user-" + requestId);
                REQUEST_ID.set((long) requestId);

                // 模拟业务处理
                processRequest();

                // 模拟请求结束：清理上下文
                USER_CONTEXT.remove();
                REQUEST_ID.remove();
            }, "request-handler-" + requestId);
            requestThread.start();
            requestThread.join();
        }
    }

    private static void processRequest() {
        String user = USER_CONTEXT.get();
        Long requestId = REQUEST_ID.get();
        System.out.println("处理请求: user=" + user + ", requestId=" + requestId);

        // 模拟异步操作
        FastThreadLocalThread asyncThread = new FastThreadLocalThread(() -> {
            // 注意：子线程无法直接访问父线程的 FastThreadLocal
            // 需要显式传递
            System.out.println("异步线程中: user=" + USER_CONTEXT.get());
        }, "async-worker");
        asyncThread.start();
        try {
            asyncThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
