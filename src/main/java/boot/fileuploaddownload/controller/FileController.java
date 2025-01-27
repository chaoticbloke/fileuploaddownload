package boot.fileuploaddownload.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/files")
public class FileController {

    //define  a location
    public static final String DIRECTORY = System.getProperty("user.home") + "/Downloads/uploads";
    private static final Logger log = LoggerFactory.getLogger(FileController.class);
   ///Users/aditya/Downloads
    //define a method to upload files

    @PostMapping("/upload")
    public ResponseEntity<List<String>> uploadFiles(@RequestParam("files") List<MultipartFile> files) throws IOException {
        List<String> fileNames = new ArrayList<>();
        for(MultipartFile file : files){
            String fileName = StringUtils.cleanPath(file.getOriginalFilename());
            Path pathStorage = Paths.get(DIRECTORY, fileName).toAbsolutePath().normalize();
            //save the files to location
            Files.copy(file.getInputStream(), pathStorage, StandardCopyOption.REPLACE_EXISTING);

            //get the file names to return
            fileNames.add(fileName);
        }
        return ResponseEntity.ok(fileNames);
    }


    @GetMapping("/download/{fileName}")
    public ResponseEntity<Resource> downloadFiles(@PathVariable("fileName") String fileName) throws IOException {
        // Locate the file in the DIRECTORY
        Optional<Path> filePathOptional = Files.list(Paths.get(DIRECTORY))
                .filter(file -> file.getFileName().toString().equalsIgnoreCase(fileName))
                .findFirst();

        if (!filePathOptional.isPresent()) {
            throw new FileNotFoundException("File " + fileName + " was not found on the server!");
        }

        Path filePath = filePathOptional.get();
        Resource resource = new UrlResource(filePath.toUri());

        if (!resource.exists() || !resource.isReadable()) {
            throw new FileNotFoundException("File " + fileName + " exists but cannot be read!");
        }

        // Determine content type
        String contentType = Files.probeContentType(filePath);
        if (contentType == null) {
            contentType = "application/octet-stream"; // Default content type if unable to determine
        }

        // Prepare HTTP headers
        HttpHeaders headers = new HttpHeaders();
        headers.add("File-Name", fileName);
        headers.add("Content-Type", contentType);
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"");

        // Return the file as a resource with headers
        return ResponseEntity.ok()
                .headers(headers)
                .contentLength(resource.contentLength())  // Setting content length to inform the client about file size
                .body(resource);
    }


    @GetMapping("")
    public ResponseEntity<?> getAllFiles() {
        LinkedHashSet<String> set = new LinkedHashSet<>();
        try(Stream<Path> files = Files.list(Paths.get(DIRECTORY))){
             set = files.map(file->file.getFileName().toAbsolutePath().normalize().toString()).collect(Collectors.toCollection(LinkedHashSet::new));
             return ResponseEntity.ok(set);
        } catch(IOException ex){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("IOException: " + ex.getMessage());
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Exception: " + ex.getMessage());
        }
    }
}
