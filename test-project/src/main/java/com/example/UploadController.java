package com.example;

import framework.UploadedFile;
import framework.annotations.Controller;
import framework.annotations.Post;

import java.io.IOException;
import java.nio.file.Path;

@Controller("/files")
public class UploadController {

    @Post("/upload")
    public String upload(UploadedFile file) throws IOException {
        Path storedFile = file.save(Path.of(System.getProperty("java.io.tmpdir"), "framework-uploads"));
        return "<h1>Fichier reçu</h1>"
                + "<p>Nom : " + file.getFileName() + "</p>"
                + "<p>Taille : " + file.getSize() + " octets</p>"
                + "<p>Stocké dans : " + storedFile.getFileName() + "</p>";
    }
}
