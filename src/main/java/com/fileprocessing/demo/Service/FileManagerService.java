package com.fileprocessing.demo.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fileprocessing.demo.Model.MyData;
//import com.opencsv.CSVWriter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

//import static com.fileprocessing.demo.Service.EnhancedFileManagerService.VERSION_DIR;

@Service
public class FileManagerService {

    private static final String VERSION_DIR ="versions/";
    private final ObjectMapper objectMapper = new ObjectMapper();
    public final EnhancedFileManagerService enhancedFileManagerService = new EnhancedFileManagerService();

    // Read text file
    public String readTextFile(String filePath) {
        StringBuilder content = new StringBuilder();

        // Check if the file has a .txt extension
        if (!filePath.toLowerCase().endsWith(".txt")) {
            return "Invalid file type. Only .txt files are allowed.";
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
            return "Error reading file.";
        }

        return content.toString();
    }

    // Write to text file with versioning
    public void writeTextFile(String filePath, String content) throws IOException {


        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, true))) { // 'true' for append mode
            writer.write(content);
            writer.newLine(); // Add a newline after writing the content
        }
        // Create a version after writing
        enhancedFileManagerService.saveFileVersion(filePath);
    }

    // Read CSV file
    public List<List<String>> readCSV(String filePath) throws IOException {
        if (!filePath.endsWith(".csv")) {
            throw new IllegalArgumentException("Invalid file type. Only .csv files are allowed.");
        }
        List<List<String>> data = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] columns = line.split(",");
                List<String> row = new ArrayList<>();
                for (String column : columns) {
                    row.add(column.trim());
                }
                data.add(row);
            }
        }catch (FileNotFoundException e) {
            System.err.println("File not found: " + filePath);
            return Collections.singletonList(Collections.singletonList("File Not Found"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return data;
    }

    // Write CSV file with versioning
    public void writeCSV(String filePath, List<List<String>> data) throws IOException {
        // Create a version before writing
//        enhancedFileManagerService.saveFileVersion(filePath);

        try (FileWriter writer = new FileWriter(filePath, true)) { // 'true' for append mode
            for (List<String> row : data) {
                writer.write(String.join(",", row)); // Convert list to comma-separated string
                writer.write("\n"); // Add a new line for each row
            }
        }
    }

    // Read JSON file into a List of Maps
    public List<Map<String, Object>> readJsonFile(String filePath) {
        try {
            return objectMapper.readValue(new File(filePath), new TypeReference<List<Map<String, Object>>>() {});
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    // Write JSON file with versioning
    public void writeJsonFile(String filePath, List<Map<String, Object>> data) throws IOException {


        File file = new File(filePath);
        List<Map<String, Object>> dataList = new ArrayList<>();

        if (file.exists()) {
            // Read the existing data from the file
            try {
                dataList = objectMapper.readValue(file, new TypeReference<List<Map<String, Object>>>() {});
            } catch (IOException e) {
                // Handle error (file may not be in list format)
                Map<String, Object> existingData = objectMapper.readValue(file, Map.class);
                dataList.add(existingData); // Add the existing object to the list
            }
        }

        // Add the new data to the list
        dataList.addAll(data);

        // Write the updated list back to the file
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, dataList);
        // Create a version after writing
        enhancedFileManagerService.saveFileVersion(filePath);
    }

    // List all available versions for a file
    public List<String> listVersions(String filePath) {
        return enhancedFileManagerService.listFileVersions(filePath);
    }
    public String readVersionContent(String version) {
        // Ensure VERSION_DIR is correctly configured
        System.out.println("VERSION_DIR: " + VERSION_DIR);
        String filePath = VERSION_DIR + "/" + version;  // Example path format
        System.out.println("Requested File Path: " + filePath);

        File versionFile = new File(filePath);

        if (versionFile.exists()) {
            try {
                // Log the content being read
                String content = new String(Files.readAllBytes(versionFile.toPath()));
                System.out.println("File Content: " + content);
                return content;
            } catch (IOException e) {
                System.err.println("Error reading the file: " + e.getMessage());
                throw new RuntimeException("Error reading the file: " + e.getMessage());
            }
        } else {
            System.err.println("Versioned file not found: " + filePath);
            throw new RuntimeException("Versioned file not found");
        }
    }
    public byte[] downloadFileVersion(String version) throws IOException {

        System.out.println(version);
        Path filePath = Paths.get(VERSION_DIR,version);

        if (!Files.exists(filePath)) {
            throw new IOException("File version not found: " + version);
        }
        return Files.readAllBytes(filePath);  // Return the content of the file as bytes
    }

    // Method to delete a file version
    public static boolean deleteFileVersion(String version) throws IOException {
        // Construct the versioned file path
        Path filePath = Paths.get(VERSION_DIR,version);  // Assuming the files are .txt files, modify as necessary

        if (!Files.exists(filePath)) {
            return false;  // Version not found, nothing to delete
        }

        // Delete the version file
        Files.delete(filePath);
        return true;
    }
}
