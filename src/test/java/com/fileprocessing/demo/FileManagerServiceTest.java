package com.fileprocessing.demo;

import org.junit.jupiter.api.BeforeEach;
import com.fileprocessing.demo.Service.EnhancedFileManagerService;
import com.fileprocessing.demo.Service.FileManagerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
public class FileManagerServiceTest {

    @InjectMocks
    private FileManagerService fileManagerService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private EnhancedFileManagerService enhancedFileManagerService;
    private File mockFile;


    @BeforeEach
    void setUp() throws NoSuchFieldException, IllegalAccessException {
        // Mock EnhancedFileManagerService
        enhancedFileManagerService = mock(EnhancedFileManagerService.class);

        // Create FileManagerService instance
        fileManagerService = new FileManagerService();

        // Use reflection to inject the mock into the private field
        Field field = FileManagerService.class.getDeclaredField("enhancedFileManagerService");
        field.setAccessible(true); // Make the private field accessible
        field.set(fileManagerService, enhancedFileManagerService);

        // Spy on fileManagerService to allow further method mocking
        fileManagerService = spy(fileManagerService);

        // Mock File class for file handling
        mockFile = mock(File.class);
    }


    // DU Path 1: Valid file with content
    // The function readTextFile is tested by passing a valid file path "testFile.txt"
    // which contains some text content. The function should read the content and return it.
    // The variable "filePath" is defined here, and it is used as a parameter to the function.
    @Test
    public void testReadTextFile_ValidFile() throws IOException {
        // Create a temporary text file with some content
        String filePath = "testFile.txt";
        Files.write(Paths.get(filePath), "Hello, World!\nThis is a test file.".getBytes());

        String content = fileManagerService.readTextFile(filePath);
        assertEquals("Hello, World!\nThis is a test file.\n", content);

        // Clean up
        Files.deleteIfExists(Paths.get(filePath));
    }

    // DU Path 2: Invalid file extension
    // The function readTextFile is tested with an invalid file extension (.csv).
    // The function is expected to return an error message indicating that only .txt files are allowed.
    // The variable "filePath" is defined here, and it is used as a parameter to the function.
    @Test
    public void testReadTextFile_InvalidExtension() {
        String filePath = "testFile.csv";
        String result = fileManagerService.readTextFile(filePath);
        assertEquals("Invalid file type. Only .txt files are allowed.", result);
    }

    // DU Path 3: File not found
    // The function readTextFile is tested with a non-existent file path ("nonExistentFile.txt").
    // The function should return a generic error message indicating that the file could not be read.
    // The variable "filePath" is defined here and is used as the input to the function.

    @Test
    public void testReadTextFile_FileNotFound() {
        String filePath = "nonExistentFile.txt";
        String result = fileManagerService.readTextFile(filePath);
        assertEquals("Error reading file.", result);
    }

    // DU Path 4: Append to file
    // The function writeTextFile is tested with a valid file path ("testWrite.txt").
    // The function appends new content ("Appended Content") to an existing file containing "Initial Content".
    // The variable "filePath" is defined here and is used as the input to the function.

    @Test
    public void testWriteTextFile_AppendContent() throws IOException {
        // Create a temporary file
        String filePath = "testWrite.txt";
        Files.write(Paths.get(filePath), "Initial Content\n".getBytes());

        // Write new content
        fileManagerService.writeTextFile(filePath, "Appended Content");

        // Verify content
        String content = new String(Files.readAllBytes(Paths.get(filePath)));
        assertTrue(content.contains("Appended Content"));

        // Clean up
        Files.deleteIfExists(Paths.get(filePath));
    }

    // DU Path 5: Valid CSV file
    // The function readCSV is tested with a valid CSV file ("testFile.csv") containing data.
    // The file contains a header row followed by a single data row.
    // The function should correctly parse the CSV and return a list of lists, where each inner list corresponds to a row of the CSV.
    // The variable "filePath" is defined here and is used as input to the readCSV method.
    @Test
    public void testReadCSV_ValidFile() throws IOException {
        // Create a temporary CSV file
        String filePath = "testFile.csv";
        Files.write(Paths.get(filePath), "name,age,city\nJohn,30,New York".getBytes());

        List<List<String>> result = fileManagerService.readCSV(filePath);
        assertEquals(2, result.size()); // Header + 1 row
        assertEquals("John", result.get(1).get(0));

        // Clean up
        Files.deleteIfExists(Paths.get(filePath));
    }

    // DU Path 6: File does not exist
    // The function readCSV is tested with a file path to a non-existent file.
    // The function should return a list containing a single list with the message "File Not Found".
    // The variable "filePath" is defined here and is used as input to the readCSV method.

    @Test
    public void testReadCSV_FileNotFound() throws IOException {
        String filePath = "/home/hemanth/Desktop/iiitb/ST/fileprocess_project/FILES/nonExistentFile.csv";  // File does not exist
        List<List<String>> result = fileManagerService.readCSV(filePath);
        assertEquals(Collections.singletonList(Collections.singletonList("File Not Found")), result);
    }

    // DU Path 7: Function call and file writing
    // The writeJsonFile function is invoked with the data and filePath.
    // This path tests the writing of data to a JSON file and checks if the function handles file creation properly.
    // The "data" variable is passed to the function, which is the key data input for the function.
    @Test
    public void testWriteJsonFile_ValidData() throws IOException {
        // Prepare data (List of Maps)
        String filePath = "testJson.json";
        List<Map<String, Object>> data = new ArrayList<>();
        Map<String, Object> dataMap1 = new HashMap<>();
        dataMap1.put("name", "John");
        dataMap1.put("age", 22);
        data.add(dataMap1);

        fileManagerService.writeJsonFile(filePath, data);

        // Verify the file exists and check its content
        File file = new File(filePath);
        assertTrue(file.exists());

        // Optionally, read the file back and verify its content
        String jsonContent = new String(Files.readAllBytes(file.toPath()));
        assertTrue(jsonContent.contains("John"));
        assertTrue(jsonContent.contains("22"));

        // Clean up
        Files.deleteIfExists(file.toPath());
    }

    // DU Path 8: Data preparation and writing to file
    // The function writeJsonFile is first tested to create a valid JSON file that can be read.
    // This prepares the test by writing a sample JSON file containing data that will be read by readJsonFile.
    // The variable "filePath" is used to specify where the JSON file will be written.
    // The variable "data" is a list of maps that contains the data to be written.

    @Test
    public void testReadJsonFile_ValidData() throws IOException {
        // Prepare a valid JSON file (List of Maps)
        String filePath = "testReadJson.json";
        List<Map<String, Object>> data = new ArrayList<>();
        Map<String, Object> dataMap1 = new HashMap<>();
        dataMap1.put("name", "John");
        dataMap1.put("age", 22);
        data.add(dataMap1);

        // Write the data to the file
        fileManagerService.writeJsonFile(filePath, data);

       //DU Path 9: Reading file providing filePath
        List<Map<String, Object>> result = fileManagerService.readJsonFile(filePath);
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("John", result.get(0).get("name"));
        assertEquals(22, result.get(0).get("age"));

        // Clean up
        Files.deleteIfExists(Paths.get(filePath));
    }
    // DU Path 10: Calling the readJsonFile function with an invalid file extension
    @Test
    public void testReadJsonFile_InvalidExtension() {
        String filePath = "testFile.txt";  // Invalid extension for a JSON file
        List<Map<String, Object>> result = fileManagerService.readJsonFile(filePath);
        assertNull(result);  // Assuming readJsonFile returns null on invalid file type
    }

    // DU Path 11: Calling the readJsonFile function with an invalid filePath
    @Test
    public void testReadJsonFile_InvalidFilePath() {
        String filePath = "nonExistentFile.json";
        List<Map<String, Object>> result = fileManagerService.readJsonFile(filePath);
        assertNull(result);
    }

    // DU Path 12: Calling the readCSV function with an invalid file extension
    @Test
    public void testReadCSV_InvalidExtension() {
        String filePath = "/home/hemanth/Desktop/iiitb/ST/fileprocess_project/FILES/text_file.txt";  // Invalid extension for a CSV file
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            fileManagerService.readCSV(filePath);
        });

        String expectedMessage = "Invalid file type. Only .csv files are allowed.";
        assertEquals(expectedMessage, exception.getMessage());
    }



    @Test
    void testWriteCSV() throws IOException {
        String filePath = "test.csv";
        List<List<String>> data = Arrays.asList(
                Arrays.asList("Col1", "Col2"),
                Arrays.asList("Data1", "Data2")
        );
        // DU Path 13: Mocking the writeCSV method passing filePath and data variables
        doNothing().when(fileManagerService).writeCSV(filePath, data);

        // DU Path 14: writeCSV method with filePath,data
        fileManagerService.writeCSV(filePath, data);
    }


    // Test for listVersions method
    @Test
    void testListVersions() throws IOException {
        String filePath = "test.txt";
        List<String> versions = Arrays.asList("test_v1", "test_v2");
        // DU Path 15: Mocking the listFileVersions  method passing the defined filePath and versions variables
        when(enhancedFileManagerService.listFileVersions(filePath)).thenReturn(versions);

        // DU Path 16: listVersions method with filePath
        List<String> result = fileManagerService.listVersions(filePath);
        assert(result.equals(versions));
    }

    @Test
    void testDeleteFileVersion() throws IOException {
        try (MockedStatic<FileManagerService> mocked = Mockito.mockStatic(FileManagerService.class)) {
            String version = "test_v1";
            // DU Path 17: Mocking the deleteFileVersion method with defined version
            mocked.when(() -> FileManagerService.deleteFileVersion(version)).thenReturn(true);

            // DU Path 18: deleteFileVersion method with version
            boolean result = FileManagerService.deleteFileVersion(version);
            assertTrue(result);
        }
    }
    @Test
    void testDownloadFileVersion() {
        String version = "test_v1";  // The file name to test
        Path versionsDir = Paths.get("/home/hemanth/Desktop/iiitb/ST/fileprocess_project/versions");

        try {
            // DU Path 19: Defined and used versionsDir and version to get the filePath
            Path filePath = versionsDir.resolve(version);

            // DU Path 20: Defined and used filePath for checking file existence
            if (!Files.exists(filePath)) {
                Files.createFile(filePath);
            }

            // DU Path 21: Defined and used mockData for writing to file
            String mockData = "Mocked file content for download.";
            Files.write(filePath, mockData.getBytes());

            // Mock the expected byte data returned by the download method
            byte[] expectedData = mockData.getBytes();

            // Call the downloadFileVersion method
            byte[] result = fileManagerService.downloadFileVersion(version);

            // DU Path 22: for expectedData and result
            assertArrayEquals(result, expectedData, "Downloaded content should match");

            // DU Path 23: For filePath
            Files.delete(filePath);

        } catch (IOException e) {
            fail("IOException occurred: " + e.getMessage());
        }
    }

}
