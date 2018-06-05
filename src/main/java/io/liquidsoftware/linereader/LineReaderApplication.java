package io.liquidsoftware.linereader;

import io.liquidsoftware.linereader.config.Constants;
import io.liquidsoftware.linereader.domain.service.FileGeneratorService;
import io.liquidsoftware.linereader.domain.service.LineReaderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.env.Environment;

import java.io.File;
import java.nio.file.Path;

@SpringBootApplication
@ComponentScan(basePackages = "io.liquidsoftware")
public class LineReaderApplication {

    public static final Logger LOGGER = LoggerFactory.getLogger(LineReaderApplication.class);

    public static void main(String[] args) {
        // Spin up the application
        ApplicationContext ctx = SpringApplication.run(LineReaderApplication.class, args);

        // Generate a 100MB file
        File largeFile = new File(Constants.FILE_PATH);
        if (!largeFile.exists()) {
            LOGGER.info("Generating 100MB text file at " + Constants.FILE_PATH);
            FileGeneratorService fileGenerator = ctx.getBean(FileGeneratorService.class);
            Path path = fileGenerator.generateFile(104857600L);
            if (path.toFile().exists()) {
                LOGGER.info("Text file generation complete");
            } else {
                throw new IllegalStateException("Failed to generate file at: " + Constants.FILE_PATH);
            }
        } else {
            LOGGER.info("File generated at: " + Constants.FILE_PATH);
        }

        // For quick access to desired line
        LOGGER.info("Indexing file...");
        LineReaderService lineReader = ctx.getBean(LineReaderService.class);
        lineReader.initialize();
        LOGGER.info("Indexing complete");

        Environment env = ctx.getEnvironment();
        String protocol = "http";
        LOGGER.info("\n----------------------------------------------------------\n\t" +
                "Application '{}' is running! \n\tAccess URLs:\n\t" +
                "Local: \t\t{}://localhost:{}\n" +
                "----------------------------------------------------------",
            env.getProperty("spring.application.name"),
            protocol,
            env.getProperty("server.port"));

        LOGGER.info("Startup Complete.");
    }

}
