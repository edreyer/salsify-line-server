package io.liquidsoftware.linereader.domain.service;

import io.liquidsoftware.linereader.config.Constants;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.Random;
import java.util.stream.LongStream;

@Service
public class FileGeneratorService {

    private Random rand = new SecureRandom();
    private final int asciiOffset = 65;
    private final int maxLineLength = Constants.MAX_LINE_LENGTH;

    /**
     * Generate a file random text up to `fileSize` bytes in size.
     *
     * @param fileSize
     */
    public Path generateFile(long fileSize) {
        long lines = fileSize / maxLineLength;
        try {
            return Files.write(Paths.get(Constants.FILE_PATH),
                (Iterable<String>) LongStream.range(0, lines).mapToObj(this::randomLine)::iterator);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to generate large text file", e);
        }
    }

    /**
     * Generates a random line up to max line length.
     * Note: This method is used as a lambda in `generateFiles`, thus the extraneous method parameter
     *
     * @param ignored
     * @return
     */
    protected String randomLine(long ignored) {
        StringBuffer sb = new StringBuffer();
        boolean allGood = true;
        while (allGood) {
            String word = randomWord();
            if (sb.length() + word.length() + 1 < maxLineLength) {
                sb.append(word).append(" ");
            } else {
                allGood = false;
            }
        }
        return sb.toString();
    }

    /**
     * Generates a random "word" between 3 and 8 characters long
     *
     * @return
     */
    protected String randomWord() {
        byte[] bytes = new byte[rand.nextInt(6) + 3]; // 3 to 8 char words (with space)
        for (int i = 0; i < bytes.length; i++) {
            byte asciiCode = (byte) (rand.nextInt(26) + asciiOffset);
            bytes[i] = asciiCode;
        }
        return new String(bytes);
    }

}
