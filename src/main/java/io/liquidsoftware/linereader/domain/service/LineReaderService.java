package io.liquidsoftware.linereader.domain.service;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.liquidsoftware.linereader.config.Constants;
import io.vavr.collection.Stream;
import io.vavr.control.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Service
public class LineReaderService {

    private static final Logger LOGGER = LoggerFactory.getLogger(LineReaderService.class);

    private Map<Long, Long> offsetMap;

    public class MaxLineExceededException extends RuntimeException {}

    /**
     * Creates in-memory index of file
     *
     * @throws IOException
     */
    public void initialize() {
        Map<Long, Long> map = Maps.newHashMap();
        map.put(1L, 0L);
        try {
            offsetMap = Stream.ofAll(Files.lines(Paths.get(Constants.FILE_PATH)))
                .foldLeft(map, (m, l) -> {
                    long lineNum = m.size() + 1;
                    long offset = m.get((long)m.size()) + l.length() + 1;
                    m.put(lineNum, offset);
                    return m;
                });
            offsetMap.remove((long)offsetMap.size()); // edge condition
        } catch (IOException e) {
            // wrap checked exceptions
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the specified line in our large file
     *
     * @param lineNum
     * @return
     */
    @Cacheable("readLine")
    public String readLine(long lineNum) {
        if (lineNum > offsetMap.size()) {
            throw new MaxLineExceededException();
        }
        try (RandomAccessFile raf = new RandomAccessFile(Constants.FILE_PATH, "r")) {
            StopWatch sw = new StopWatch();
            sw.start();
            raf.seek(offsetMap.get(lineNum));
            sw.stop();
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("readLine time: " + sw.getTotalTimeMillis());
            }
            return raf.readLine();
        } catch (Exception e) {
            String msg = "Failed to access specified line: " + lineNum;
            LOGGER.error(msg, e);
            throw new IllegalStateException(msg);
        }
    }

    /**
     * Useful for quick and dirty performance testing.
     * <p>
     * Retrieves a specified number of randomly chosen lines from the file
     * concurrently using the specified number of threads.
     *
     * @param numLines
     * @param numThreads
     * @return
     */
    public Long timeToGetLines(int numLines, int numThreads) {
        try (RandomAccessFile raf = new RandomAccessFile(Constants.FILE_PATH, "r")) {
            Random r = new Random();
            List<Callable<String>> tasks = Lists.newArrayListWithCapacity(numLines);
            int max = (int) Math.min(Integer.MAX_VALUE, (raf.length() / Constants.MAX_LINE_LENGTH) - 5);
            for (int i = 0; i < numLines; i++) {
                tasks.add(() -> readLine(r.nextInt(max)));
            }

            StopWatch sw = new StopWatch();
            ExecutorService es = Executors.newFixedThreadPool(numThreads);
            sw.start();
            List<Future<String>> results = es.invokeAll(tasks);
            results.forEach(f -> Try.of(() -> f.get()).onFailure(e -> LOGGER.error("Failed to get line", e)));
            sw.stop();
            es.shutdown();

            return sw.getTotalTimeMillis();
        } catch (IOException | InterruptedException e) {
            // wrap checked exceptions
            throw new RuntimeException(e);
        }
    }

}
