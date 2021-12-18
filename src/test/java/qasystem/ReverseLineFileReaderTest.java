package qasystem;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class ReverseLineFileReaderTest {

    @Test
    public void testReadNormalFile() throws IOException {
        File testFile = createTestingFile();
        writeFile(testFile, Arrays.asList(
                "Give you up",
                "Let you down",
                "Run around",
                "Desert you"
        ));
        try (ReverseLineFileReader reverseLineFileReader = new ReverseLineFileReader(testFile)) {
            assertEquals(reverseLineFileReader.readLine(), "Desert you");
            assertEquals(reverseLineFileReader.readLine(), "Run around");
            assertEquals(reverseLineFileReader.readLine(), "Let you down");
            assertEquals(reverseLineFileReader.readLine(), "Give you up");
            assertNull(reverseLineFileReader.readLine());
        }
    }

    @Test
    public void testReadEmptyFile() throws IOException {
        File testFile = createTestingFile();
        try (ReverseLineFileReader reverseLineFileReader = new ReverseLineFileReader(testFile)) {
            assertNull(reverseLineFileReader.readLine());
        }
    }

    @Test
    public void testNotFoundFile() {
        File file = new File(randomString(12) + ".txt");
        try (ReverseLineFileReader reverseLineFileReader = new ReverseLineFileReader(file)) {
        } catch (FileNotFoundException e) {
            // should be thrown here
            return;
        } catch (IOException e) {
            fail();
        }
        fail();
    }

    @Test
    public void testReadInParallel() throws IOException, InterruptedException {
        File file = createTestingFile();
        Set<String> fileContent = randomStrings(100, 10);
        writeFile(file, fileContent);
        ReverseLineFileReader reverseLineFileReader = new ReverseLineFileReader(file);

        List<Callable<List<String>>> tasks =
                IntStream.range(0, 3)
                        .mapToObj(i -> (Callable<List<String>>) () -> {
                            List<String> readResult = readAllLineFrom(reverseLineFileReader);
                            reverseLineFileReader.close();
                            return readResult;
                        }).collect(toList());
        ExecutorService executorService = Executors.newCachedThreadPool();
        List<Future<List<String>>> futures = executorService.invokeAll(tasks);
        Set<String> combinedResult = new HashSet<>();
        for (Future<List<String>> future : futures) {
            try {
                combinedResult.addAll(future.get());
            } catch (Exception e) {
                fail();
            }
        }

        assertTrue(combinedResult.containsAll(fileContent));
        assertTrue(fileContent.containsAll(combinedResult));
    }

    private File createTestingFile() throws IOException {
        File file = new File(randomString(10) + ".txt");
        file.createNewFile();
        file.deleteOnExit();
        return file;
    }

    private Set<String> randomStrings(int quantity, int targetStringLength) {
        Set<String> randomStrings = new HashSet<>();
        for (int i = 0; i < quantity; i++) {
            randomStrings.add(randomString(targetStringLength));
        }
        return randomStrings;
    }

    private String randomString(int targetStringLength) {
        int leftLimit = 'a';
        int rightLimit = 'z';
        Random random = new Random();
        StringBuilder buffer = new StringBuilder(targetStringLength);
        for (int i = 0; i < targetStringLength; i++) {
            int randomLimitedInt = leftLimit + Math.round(random.nextFloat() * (rightLimit - leftLimit + 1));
            buffer.append((char) randomLimitedInt);
        }
        return buffer.toString();
    }

    private void writeFile(File file, Collection<String> content) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            for (String line : content) {
                writer.write(line);
                writer.newLine();
            }
        }
    }

    private List<String> readAllLineFrom(ReverseLineFileReader reverseLineFileReader) throws IOException {
        List<String> result = new ArrayList<>();
        String line;
        while ((line = reverseLineFileReader.readLine()) != null) {
            result.add(line);
        }
        return result;
    }
}
