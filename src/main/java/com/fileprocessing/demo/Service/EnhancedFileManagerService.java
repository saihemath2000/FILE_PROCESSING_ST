package com.fileprocessing.demo.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class EnhancedFileManagerService {

    private static final String VERSION_DIR = "versions/";
    private final ObjectMapper objectMapper = new ObjectMapper();

    public EnhancedFileManagerService() {
        // Create the versions directory if it doesn't exist
        File versionDir = new File(VERSION_DIR);
        if (!versionDir.exists()) {
            versionDir.mkdir();
        }
    }
    public void addContentAndCreateVersionText(String filePath, String content) throws IOException {
        File file = new File(filePath);
        if (!file.exists()) {
            throw new FileNotFoundException("File not found: " + filePath);
        }

        // Create a version file with a unique name
        String versionedFileName = VERSION_DIR + file.getName() + "_v" + System.currentTimeMillis();
        File versionedFile = new File(versionedFileName);

        try (InputStream is = new FileInputStream(file);
             OutputStream os = new FileOutputStream(versionedFile)) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
        }
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(versionedFile, true))) {
            writer.write(content);  // Write the new content to the versioned file
            writer.newLine();       // Add a newline after the content
        }
    }


    // Save a version of the file
    public void saveFileVersionJson(String filePath, List<Map<String, Object>> dataList) throws IOException {
        File file = new File(filePath);
        if (!file.exists()) {
            throw new FileNotFoundException("File not found: " + filePath);
        }

        // Create a version file with a unique name
        String versionedFileName = VERSION_DIR + file.getName() + "_v" + System.currentTimeMillis();
        File versionedFile = new File(versionedFileName);
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(versionedFile, dataList);
    }

    public List<String> listFileVersions(String originalFileName) {
        File versionDir = new File(VERSION_DIR);
        String regex = "^" + Pattern.quote(originalFileName) + "_v[0-9]+$"; // Matches originalFileName followed by "_v" and digits
        System.out.println("Regex: " + regex);

        File[] files = versionDir.listFiles((dir, name) -> {
            System.out.println("Checking: " + name); // Debugging statement
            return name.matches(regex);
        });

        List<String> versionFiles = new ArrayList<>();

        if (files != null) {
            for (File file : files) {
                versionFiles.add(file.getName());
            }
        }

        System.out.println("Version Files: " + versionFiles); // Debugging output
        return versionFiles;
    }
}
