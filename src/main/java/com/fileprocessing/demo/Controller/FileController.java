package com.fileprocessing.demo.Controller;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fileprocessing.demo.Service.FileManagerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;
import java.util.*;

@Controller
@RequestMapping("/file")
public class FileController {
    @Autowired
    private FileManagerService fileService;
    @Autowired
    private FileManagerService fileManagerService;

    // Utility function for file extension validation
    private boolean hasExtension(String filePath, String extension) {
        return filePath.toLowerCase().endsWith("." + extension);
    }

    @GetMapping("/")
    public String getFileManagementPage() {
        return "FileManagement"; // Ensure this corresponds to file-management.html
    }

    @GetMapping("/read-text")
    public ResponseEntity<String> readTextFile(@RequestParam String filePath) {
        String content = fileManagerService.readTextFile(filePath);
        return ResponseEntity.ok(content); // Send the text content directly
    }


    @PostMapping("/write-text")
    public String writeTextFile(@RequestParam String filePath, @RequestParam String content, Model model) {
        try {
            if (!hasExtension(filePath, "txt")) {
                model.addAttribute("error", "Only .txt files are allowed.");
                return "FileManagement";
            }
            fileManagerService.writeTextFile(filePath, content);
            model.addAttribute("message", "File written successfully.");
        } catch (Exception e) {
            model.addAttribute("error", "Error writing to file: ");
        }
        return "FileManagement";
    }

    @GetMapping("/read-csv")
    public String readCsvFile(@RequestParam String filePath, Model model) {
        try {
            // Use the service to read the CSV file

            List<List<String>> csvData = fileService.readCSV(filePath);

            // Pass the csvData to the model
            model.addAttribute("csvData", csvData);
            System.out.println("CSV Data: " + csvData);  // Debugging line
        } catch (IOException e) {
            model.addAttribute("error", "Error reading CSV file: " + e.getMessage()); // Handle errors
            e.printStackTrace();  // Log the stack trace
        }
        return "FileManagement"; // Return the template name to display
    }


    @PostMapping("/write-csv")
    public String writeCsvFile(@RequestParam String filePath, @RequestParam String csvData, Model model) {
        try {
            if (!hasExtension(filePath, "csv")) {
                model.addAttribute("error", "Only .csv files are allowed.");
                return "FileManagement";
            }

            List<List<String>> data = csvData.lines()
                    .map(line -> Arrays.asList(line.split(",")))
                    .toList();

            // Call service method to write CSV and handle validation
            fileManagerService.writeCSV(filePath, data);

            model.addAttribute("message", "CSV file written successfully.");
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage()); // Display validation error
        } catch (Exception e) {
            model.addAttribute("error", "Error writing to CSV file: " + e.getMessage());
        }
        return "FileManagement";
    }


    @GetMapping("/read-json")
    public ResponseEntity<String> readJsonFile(@RequestParam String filePath) {
        try {
            if (!hasExtension(filePath, "json")) {
                return ResponseEntity.badRequest().body("Only .json files are allowed.");
            }
            List<Map<String, Object>> dataList = fileManagerService.readJsonFile(filePath);
            // Convert the list to JSON (you may already have a method for this)
            ObjectMapper mapper = new ObjectMapper();
            String jsonResponse = mapper.writeValueAsString(dataList);
            return ResponseEntity.ok(jsonResponse); // Send the content as JSON
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error reading JSON file: " + e.getMessage());
        }
    }


    @PostMapping("/write-json")
    public String writeJsonFile(@RequestParam String filePath, @RequestParam String jsonData, Model model) {
        try {
            if (!hasExtension(filePath, "json")) {
                model.addAttribute("error", "Only .json files are allowed.");
                return "FileManagement";
            }
            ObjectMapper objectMapper = new ObjectMapper();
            List<Map<String, Object>> data = objectMapper.readValue(jsonData, new TypeReference<>() {});
            fileManagerService.writeJsonFile(filePath, data);
            model.addAttribute("message", "JSON file written successfully.");
        } catch (IOException e) {
            model.addAttribute("error", "Error writing to JSON file:");
        }
        return "FileManagement";
    }

    @GetMapping("/versions")
    @ResponseBody  // This will return the response as JSON
    public ResponseEntity<List<String>> listVersions(@RequestParam String filePath) {
        try {
            List<String> versions = fileManagerService.listVersions(filePath);
            return ResponseEntity.ok(versions);  // Return the list as JSON response
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonList("Error: " + e.getMessage()));  // Return error message as JSON
        }
    }

    @GetMapping("/read-version/{version}")
    public ResponseEntity<String> getVersionContent(
            @PathVariable String version,
            @RequestParam("fileType") String fileType) {
        try {
            // Logic to read the versioned file content based on version
            String content = fileManagerService.readVersionContent(version);

            // Depending on file type, return the appropriate content directly
            if ("csv".equalsIgnoreCase(fileType)) {
                return ResponseEntity.ok(content);  // CSV content as string
            } else if ("text".equalsIgnoreCase(fileType)) {
                return ResponseEntity.ok(content);  // Text content
            } else if ("json".equalsIgnoreCase(fileType)) {
                return ResponseEntity.ok(content);  // Raw JSON string
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Unsupported file type");
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error reading version content: " + e.getMessage());
        }
    }
    // Download version of the file
    @GetMapping("/download-version")
    public ResponseEntity<byte[]> downloadVersion(
            @RequestParam String fileName,
            @RequestParam String version,
            @RequestParam String fileType) {
        try {
            // Download the file content using the version identifier
            byte[] fileContent = fileService.downloadFileVersion(version);

            // Set content type based on the file type
            String contentType = fileType.equals("json") ? "application/json" : "text/plain";
            return ResponseEntity.ok()
                    .header("Content-Type", contentType)
                    .header("Content-Disposition", "attachment; filename=\"" + version + "\"")
                    .body(fileContent);
        } catch (IOException e) {
            return ResponseEntity.status(500).body(null);  // Handle the error appropriately
        }
    }


    // Delete version of the file
    @DeleteMapping("/delete-version")
    public ResponseEntity<String> deleteVersion(
            @RequestParam String version) {
        try {
            boolean deleted = fileService.deleteFileVersion(version);
            if (deleted) {
                return ResponseEntity.ok("Version deleted successfully.");
            } else {
                return ResponseEntity.status(404).body("Version not found.");
            }
        } catch (IOException e) {
            return ResponseEntity.status(500).body("Error deleting version.");
        }
    }

    @PostMapping("/rename")
    public ResponseEntity<String> renameFile(@RequestParam String filePath, @RequestParam String newFileName) {
        // Call the service method and get the result
        String result = fileService.renameFile(filePath, newFileName);

        System.out.println(result);
        // Based on the result, return appropriate response
        if (result.contains("not found") || result.contains("Invalid") || result.contains("exists")) {
            System.out.println("Hi");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
        } else if (result.contains("Failed")) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        } else {
            return ResponseEntity.ok(result);
        }
    }
}


