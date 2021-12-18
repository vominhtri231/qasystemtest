package qasystem;

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

public class ReverseLineFileReader implements Closeable {
    private final RandomAccessFile accessFile;
    private long currentPos;
    private volatile boolean isClosed = false;

    public ReverseLineFileReader(File file) throws FileNotFoundException {
        accessFile = new RandomAccessFile(file, "r");
        currentPos = file.length();
    }

    public String readLine() throws IOException {
        StringBuilder lineBuilder = new StringBuilder();

        synchronized (this) {
            if (currentPos <= 0) {
                return null;
            }

            do {
                currentPos--;
                char currentChar = readCurrentPos();
                if (currentChar != '\n' && currentChar != '\r') {
                    lineBuilder.append(currentChar);
                }
            } while (!isStartOfLine());
        }

        return lineBuilder.reverse().toString();
    }

    private char readCurrentPos() throws IOException {
        accessFile.seek(currentPos);
        return (char) accessFile.read();
    }

    private boolean isStartOfLine() throws IOException {
        if (currentPos == 0) {
            return true;
        }

        accessFile.seek(currentPos - 1);
        return accessFile.read() == '\n';
    }

    @Override
    public void close() throws IOException {
        synchronized (this) {
            if (isClosed) {
                return;
            }
            isClosed = true;
        }

        accessFile.close();
    }
}



