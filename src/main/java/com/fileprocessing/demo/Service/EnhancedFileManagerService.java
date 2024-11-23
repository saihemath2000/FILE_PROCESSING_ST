package com.fileprocessing.demo.Service;

import org.springframework.stereotype.Service;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class EnhancedFileManagerService {

    private static final String VERSION_DIR = "versions/";

    public EnhancedFileManagerService() {
        // Create the versions directory if it doesn't exist
        File versionDir = new File(VERSION_DIR);
        if (!versionDir.exists()) {
            versionDir.mkdir();
        }
    }

    // Save a version of the file
    public void saveFileVersion(String filePath) throws IOException {
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
