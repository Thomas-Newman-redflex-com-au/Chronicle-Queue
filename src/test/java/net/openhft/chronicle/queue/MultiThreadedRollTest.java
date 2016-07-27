package net.openhft.chronicle.queue;

import net.openhft.chronicle.core.time.SetTimeProvider;
import net.openhft.chronicle.queue.impl.RollingChronicleQueue;
import net.openhft.chronicle.queue.impl.single.SingleChronicleQueueBuilder;
import net.openhft.chronicle.threads.NamedThreadFactory;
import net.openhft.chronicle.wire.DocumentContext;
import org.junit.Test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static net.openhft.chronicle.queue.ChronicleQueueTestBase.getTmpDir;
import static net.openhft.chronicle.queue.RollCycles.TEST_SECONDLY;

/**
 * @author Rob Austin.
 */
public class MultiThreadedRollTest {

    @Test(timeout = 1000)
    public void test() throws ExecutionException, InterruptedException {

        ExecutorService reader = Executors.newSingleThreadExecutor(new NamedThreadFactory("reader"));
        final SetTimeProvider timeProvider = new SetTimeProvider();
        timeProvider.currentTimeMillis(1000);
        final String path = getTmpDir() + "/backRoll.q";

        final RollingChronicleQueue wqueue = SingleChronicleQueueBuilder.binary(path)
                .timeProvider(timeProvider)
                .rollCycle(TEST_SECONDLY)
                .build();

        wqueue.acquireAppender().writeText("hello world");

        final RollingChronicleQueue rqueue = SingleChronicleQueueBuilder.binary(path)
                .timeProvider(timeProvider)
                .rollCycle(TEST_SECONDLY)
                .build();

        ExcerptTailer tailer = rqueue.createTailer();

        Future f = reader.submit(() -> {
            long index = 0;
            do {

                try (DocumentContext documentContext = tailer.readingDocument()) {
                    index = documentContext.index();
                    System.out.println("documentContext.isPresent=" + documentContext.isPresent()
                            + ",index="
                            + Long.toHexString(index));
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            } while (index != 0x200000000L);

        });

        timeProvider.currentTimeMillis(2000);
        wqueue.acquireAppender().writeText("hello world");
        f.get();
    }
}
