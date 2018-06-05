package io.liquidsoftware.linereader.web.rest;

import io.liquidsoftware.linereader.domain.service.LineReaderService;
import io.liquidsoftware.linereader.domain.service.LineReaderService.MaxLineExceededException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/lines")
public class LineResource {

    private LineReaderService lineReader;

    @Autowired
    public LineResource(LineReaderService lineReader) {
        this.lineReader = lineReader;
    }

    @ExceptionHandler({MaxLineExceededException.class})
    public void handleMaxExceeded(MaxLineExceededException e, HttpServletResponse resp) {
        resp.setStatus(HttpStatus.PAYLOAD_TOO_LARGE.value());
    }
    @ExceptionHandler({RuntimeException.class})
    public void handleRuntimeException(HttpServletResponse resp) {
        resp.setStatus(HttpStatus.BAD_REQUEST.value());
    }

    /**
     * Public API as specified by the assignment
     * @param index
     * @return
     */
    @GetMapping("/{index}")
    public String getLine(@PathVariable long index) {
        return lineReader.readLine(index);
    }

    /**
     * Test API for simple performance tests
     * @param numLines
     * @param numThreads
     * @return
     */
    @GetMapping("/getsome/{numLines}/{numThreads}")
    public String timeToGetLines(@PathVariable int numLines, @PathVariable int numThreads) {
        return "Time to read " + numLines + " lines with " + numThreads + " threads : " + lineReader.timeToGetLines(numLines, numThreads) + " ms";
    }

}
