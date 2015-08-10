package com.cmc.testing.filetester;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import java.io.IOException;
import java.util.Collections;

@Controller
@RequestMapping("/")
public class FileTesterController {

    private final FileTransformer fileTransformer;

    @Autowired
    public FileTesterController(FileTransformer fileTransformer) {
        this.fileTransformer = fileTransformer;
    }

    @RequestMapping("/")
    public String home() {
        return "home";
    }

    @RequestMapping(value="/upload", method= RequestMethod.POST)
    public ResponseEntity<byte[]> handleFileUpload(@RequestParam("file") MultipartFile file) throws IOException {

        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=output.txt");

        return new ResponseEntity<byte[]>(fileTransformer.transform(file.getBytes()), headers, HttpStatus.OK);
    }

    @ExceptionHandler(FileTransformException.class)
    private ModelAndView fileTransformExceptionHandler(FileTransformException fte) {
        return new ModelAndView("home", "errors", fte.getErrors());
    }

    @ExceptionHandler(Exception.class)
    private ModelAndView fileTransformExceptionHandler(Exception e) {
        return new ModelAndView("home", "errors", Collections.singletonList(e.getMessage()));
    }
}
