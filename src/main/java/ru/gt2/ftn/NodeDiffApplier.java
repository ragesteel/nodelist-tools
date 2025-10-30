package ru.gt2.ftn;

import java.io.*;
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

 CRC-16-CCITT (poly 0x1021, init 0x0000) считается с 2-й строки нового NODELIST включительно (т.е. без заголовка).
 */
public class NodeDiffApplier {
    private static final Charset CHARSET = Charset.forName("CP866");

    public static final String END_OF_FILE = "\u001A";
    public static final String CRLF = "\r\n";

    /** Применить nodediff */
    public static void apply(File oldList, File diffFile, File newList) throws IOException {
        try (BufferedReader oldIn = new BufferedReader(new FileReader(oldList, CHARSET));
             BufferedReader diffIn = new BufferedReader(new FileReader(diffFile, CHARSET));
             BufferedWriter newOut = new BufferedWriter(new FileWriter(newList, CHARSET))) {

            // 1. Проверка заголовка
            String oldHeader = oldIn.readLine();
            String diffHeader = diffIn.readLine();

            if (!Objects.equals(oldHeader, diffHeader)) {
                throw new IOException("Header mismatch between NODELIST and NODEDIFF");
            }

            // 2. Основной цикл
            Crc16 crc = new Crc16(CHARSET);

            int expectedCRC = -1;
            boolean firstAdded = true;
            boolean srcDeleted = true;
            String diffLine;
            while ((diffLine = diffIn.readLine()) != null) {
                if (diffLine.isEmpty()) {
                    continue;
                }
                char cmd = diffLine.charAt(0);
                int count = tryParseInt(diffLine.substring(1));

                switch (cmd) {
                    case 'A' -> {
                        // добавить count строк
                        for (int i = 0; i < count; i++) {
                            String added = diffIn.readLine();
                            if (added == null) {
                                throw new EOFException("Unexpected EOF in diff (ADD)");
                            }
                            added += CRLF;
                            if (firstAdded) {
                                firstAdded = false;
                                expectedCRC = extractCRC(added);
                            } else {
                                crc.updateCrcLine(added);
                            }

                            newOut.write(added);
                        }
                    }
                    case 'D' -> {
                        // пропустить count строк из старого списка
                        for (int i = 0; i < count; i++) {
                            if (srcDeleted) {
                                srcDeleted = false;
                                continue;
                            }
                            if (oldIn.readLine() == null) {
                                throw new EOFException("Unexpected EOF in old list (DEL)");
                            }
                        }
                    }
                    case 'C' -> {
                        // скопировать count строк из старого списка
                        for (int i = 0; i < count; i++) {
                            String oldLine = oldIn.readLine();
                            if (oldLine == null) {
                                throw new EOFException("Unexpected EOF in old list (COPY)");
                            }
                            oldLine += CRLF;
                            newOut.write(oldLine);
                            crc.updateCrcLine(oldLine);
                        }
                    }
                    case ';' -> {
                        // комментарий — игнорируем
                    }
                    default -> throw new IOException("Unknown diff command: " + diffLine);
                }
            }
            newOut.write(END_OF_FILE);

            // 3. Проверка CRC
            int calculatedCrc = crc.getCrc();
            if (expectedCRC >= 0 && (calculatedCrc != expectedCRC)) {
                throw new IOException(String.format("CRC mismatch: expected %05d, got %05d", expectedCRC, calculatedCrc));
            }
        }
    }

    /// Извлечь CRC контрольную сумму из первой добавленной строки
    private static int extractCRC(String header) {
        int colon = header.lastIndexOf(':');
        if (colon < 0 || colon + 1 >= header.length()) return -1;
        try {
            return Integer.parseInt(header.substring(colon + 1).trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static int tryParseInt(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    // пример запуска
    public static void main(String[] args) throws Exception {
        if (args.length != 3) {
            System.err.println("Usage: java NodeDiffApplier <oldlist> <nodediff> <newlist>");
            System.exit(1);
        }
        apply(new File(args[0]), new File(args[1]), new File(args[2]));
        System.out.println("NODEDIFF applied successfully.");
    }
}