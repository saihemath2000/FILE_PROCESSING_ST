package com.fileprocessing.demo.Service;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;

@Service
public class FileManagerService {
    private static final String[] ALLOWED_EXTENSIONS = {"txt", "json", "csv"};
    private static final String FILE_NAME_PATTERN = "^[a-zA-Z0-9_-]+(\\.[a-zA-Z0-9_-]+)?$";
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
        // Create a version after writing
        enhancedFileManagerService.addContentAndCreateVersionText(filePath,content);
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

    public void writeCSV(String filePath, List<List<String>> data) throws IOException {
        // Read the header of the CSV file to determine the number of columns
        List<String> header = null;
        File file = new File(filePath);

        if (file.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line = reader.readLine(); // Read the header line
                if (line != null) {
                    header = Arrays.asList(line.split(","));
                }
            }
        }

        if (header == null || header.isEmpty()) {
            throw new IllegalArgumentException("CSV file must have a header row.");
        }

        // Check if the data being added has the correct number of columns
        for (List<String> row : data) {
            if (row.size() > header.size()) {
                throw new IllegalArgumentException("Row has more columns than the header. Please provide data with " + header.size() + " columns.");
            }
        }

        // Write the data to the CSV file
        try (FileWriter writer = new FileWriter(filePath, true)) { // 'true' for append mode
            for (List<String> row : data) {
                // Ensure that the row has the correct number of columns by padding with empty strings if needed
                while (row.size() < header.size()) {
                    row.add(""); // Add empty values for missing columns
                }
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
        enhancedFileManagerService.saveFileVersionJson(filePath,dataList);
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
    public String renameFile(String filePath, String newFileName) {
        File originalFile = new File(filePath);

        // Check if the original file exists
        if (!originalFile.exists()) {
            return "Original file not found.";
        }

        // Extract the original file extension
        String originalExtension = getFileExtension(filePath);

        // Check for invalid file extension for the original file
        if (!isValidExtension(originalExtension)) {
            return "Invalid original file extension. Only .txt, .json, .csv are allowed.";
        }

        // Check if the new file name is valid (alphanumeric, dash, underscore)
        if (newFileName == null || newFileName.trim().isEmpty() || !newFileName.matches(FILE_NAME_PATTERN)) {
            return "Invalid file name. Name cannot be empty and should contain only alphanumeric characters, dashes, and underscores.";
        }

        // Check that the new file name has the same extension as the original file
        String newExtension = getFileExtension(newFileName);
        if (newExtension == null || !newExtension.equals(originalExtension)) {
            System.out.print(newExtension);
            return "The new file must have the same extension as the original file (." + originalExtension + ").Not Invalid";
        }

        // Check for name conflict (if a file already exists with the new name)
        File newFile = new File(originalFile.getParent(), newFileName);
        if (newFile.exists()) {
            return "A file with the new name already exists.";
        }

        // Try renaming the original file
        boolean isRenamed = originalFile.renameTo(newFile);
        if (!isRenamed) {
            return "Failed to rename the original file.";
        }

        // Rename corresponding version files
        File versionsDir = new File("versions/");
        String fileName = extractFileNameFromPath(filePath);
        String regex = "^" + Pattern.quote(fileName) + "_v[0-9]+$";  // Match version files

        // List the version files
        File[] files = versionsDir.listFiles((dir, name) -> name.matches(regex));
        if (files != null) {
            for (File versionFile : files) {
                String newVersionFileName = versionFile.getName().replace(originalFile.getName(), newFileName);
                File newVersionFile = new File(versionsDir, newVersionFileName);
                boolean versionRenamed = versionFile.renameTo(newVersionFile);
                if (!versionRenamed) {
                    return "Failed to rename version file: " + versionFile.getName();
                }
            }
        }

        return "File and versions renamed successfully.";
    }

    public boolean isValidExtension(String extension) {
        for (String ext : ALLOWED_EXTENSIONS) {
            if (ext.equalsIgnoreCase(extension)) {
                return true;
            }
        }
        return false;
    }

    public String getFileExtension(String filePath) {
        int lastDotIndex = filePath.lastIndexOf('.');
        return (lastDotIndex > 0) ? filePath.substring(lastDotIndex + 1).toLowerCase() : "";
    }

    private String extractFileNameFromPath(String filePath) {
        return Paths.get(filePath).getFileName().toString();
    }
}
