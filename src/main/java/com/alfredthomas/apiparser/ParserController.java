package com.alfredthomas.apiparser;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ParserController {
    private final AtomicLong counter = new AtomicLong();
    private final String defaultOutPath = "zips/";

    @RequestMapping(value = "/parse", method = RequestMethod.GET, produces = "application/zip")
    public ResponseEntity<InputStreamResource> parseURL(@RequestParam(value="url")String url,@RequestParam(value="name",defaultValue = "")String name)
    {
        String filename = Long.toString(counter.incrementAndGet());
        if(name.isEmpty())
            name="result";
        try{
            ClassParser.parseAllClasses(Util.getAllClassDocument(url),defaultOutPath+filename);
            File zipFile = new File(defaultOutPath + filename+".zip");

            return ResponseEntity.ok()
                    .contentLength(zipFile.length())
                    .contentType(MediaType.parseMediaType("application/zip"))
                    .header("Content-disposition", "attachment; filename="+ name+".zip")
                    .body(new CleanupInputStreamResource(zipFile));
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return ResponseEntity.badRequest().build();

        }
    }
    private class CleanupInputStreamResource extends InputStreamResource {
        private CleanupInputStreamResource(File file) throws FileNotFoundException {
            super(new FileInputStream(file) {
                @Override
                public void close() throws IOException {
                    super.close();
                    Files.delete(file.toPath());
                }
            });
        }
    }

}
