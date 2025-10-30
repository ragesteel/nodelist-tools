package ru.gt2.ftn;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.EOFException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Objects;

/**
 Реализация FTS-5000.005 §6 — применение NODEDIFF к NODELIST.
 Формат NODEDIFF:
 ┌ первая строка — копия заголовка предыдущего NODELIST
 ├ команды: A<n>, D<n>, C<n>
 └ строки данных после A<n>

 Команды:
 A n — добавить следующие n строк в выходной файл
 D n — удалить n строк из входного файла
 C n — скопировать n строк из входного файла
 */
public class DiffProcessor {
    private final BufferedReader oldReader;
    private final BufferedReader diffReader;
    private final BufferedWriter outputWriter;
    private final Crc16 crc;
    private int expectedCRC = -1;
    private boolean firstAdded = true;
    private boolean srcDeleted = true;

    public DiffProcessor(BufferedReader oldReader, BufferedReader diffReader, BufferedWriter outputWriter, Charset charset) {
        this.oldReader = oldReader;
        this.diffReader = diffReader;
        this.outputWriter = outputWriter;
        this.crc = new Crc16(charset);
    }

    public void checkHeaders() throws IOException {
        String oldHeader = oldReader.readLine();
        String diffHeader = diffReader.readLine();
        if (!Objects.equals(oldHeader, diffHeader)) {
            throw new IOException("Header mismatch between NODELIST and NODEDIFF");
        }
    }

    public void processDiffCycle() throws IOException {
        String diffLine;
        while ((diffLine = diffReader.readLine()) != null) {
            if (diffLine.isEmpty()) {
                continue;
            }
            char cmd = diffLine.charAt(0);
            int count = tryParseInt(diffLine.substring(1));

            switch (cmd) {
                case 'A' -> handleAdd(count);
                case 'D' -> handleDelete(count);
                case 'C' -> handleCopy(count);
                case ';' -> {
                    // комментарий — игнорируем
                }
                default -> throw new IOException("Unknown diff command: " + diffLine);
            }
        }
        outputWriter.write(NLConsts.END_OF_FILE);
        outputWriter.flush();
    }

    public void checkCRC() throws IOException {
        int calculatedCrc = crc.getCrc();
        if (expectedCRC >= 0 && (calculatedCrc != expectedCRC)) {
            throw new IOException(String.format("CRC mismatch: expected %05d, got %05d", expectedCRC, calculatedCrc));
        }
    }

    private void handleAdd(int count) throws IOException {
        for (int i = 0; i < count; i++) {
            String added = diffReader.readLine();
            if (added == null) {
                throw new EOFException("Unexpected EOF in diff (ADD)");
            }
            added += NLConsts.CRLF;
            if (firstAdded) {
                firstAdded = false;
                expectedCRC = Crc16.extractCRC(added);
            } else {
                crc.updateCrcLine(added);
            }
            outputWriter.write(added);
        }
    }

    private void handleDelete(int count) throws IOException {
        for (int i = 0; i < count; i++) {
            if (srcDeleted) {
                srcDeleted = false;
                continue;
            }
            if (oldReader.readLine() == null) {
                throw new EOFException("Unexpected EOF in old list (DEL)");
            }
        }
    }

    private void handleCopy(int count) throws IOException {
        for (int i = 0; i < count; i++) {
            String oldLine = oldReader.readLine();
            if (oldLine == null) {
                throw new EOFException("Unexpected EOF in old list (COPY)");
            }
            oldLine += NLConsts.CRLF;
            outputWriter.write(oldLine);
            crc.updateCrcLine(oldLine);
        }
    }


    private static int tryParseInt(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
