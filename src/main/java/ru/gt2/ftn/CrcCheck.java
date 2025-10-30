package ru.gt2.ftn;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 Проверка CRC нодлиста согласно FTS-5000.005 §5.2
 CRC16-CCITT (poly 0x1021, init 0x0000)
 Вычисляется со второй строки, включая все символы CR/LF.
 */
public class CrcCheck {
    private static final Charset CHARSET = Charset.forName("CP866");

    /** Проверяет CRC и возвращает true, если совпадает */
    public static boolean verifyCRC(File nodelist) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(nodelist, CHARSET))) {
            String header = br.readLine();
            if (header == null) throw new IOException("Empty file");
            int expectedCRC = extractCRC(header);
            if (expectedCRC < 0) throw new IOException("Cannot extract CRC from header");

            int crc = 0;
            String line;
            while ((line = br.readLine()) != null) {
                if (line.equals("\u001A")) {
                    break;
                }
                for (byte b : (line + "\r\n").getBytes(CHARSET)) {
                    crc = updateCRC(b, crc);
                }
            }

            System.out.printf("Expected CRC: %05d, Calculated CRC: %05d%n", expectedCRC, crc & 0xFFFF);
            return (crc & 0xFFFF) == expectedCRC;
        }
    }

    /** Извлекает CRC из первой строки (после последнего ':') */
    private static int extractCRC(String header) {
        int colon = header.lastIndexOf(':');
        if (colon < 0 || colon + 1 >= header.length()) return -1;
        try {
            return Integer.parseInt(header.substring(colon + 1).trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /** Обновление CRC16/CCITT */
    private static int updateCRC(byte b, int crc) {
        int v = (b ^ (crc >>> 8)) & 0xFF;
        crc = ((crc << 8) ^ CRC_TABLE[v]) & 0xFFFF;
        return crc;
    }

    /** Таблица CRC-16 CCITT */
    private static final int[] CRC_TABLE = new int[256];
    static {
        for (int i = 0; i < 256; i++) {
            int c = i << 8;
            for (int j = 0; j < 8; j++) {
                c = ((c & 0x8000) != 0) ? ((c << 1) ^ 0x1021) : (c << 1);
            }
            CRC_TABLE[i] = c & 0xFFFF;
        }
    }

    // пример использования
    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.err.println("Usage: java NodelistCRCChecker <NODELIST.nnn>");
            System.exit(1);
        }
        boolean ok = verifyCRC(new File(args[0]));
        System.out.println(ok ? "CRC OK" : "CRC mismatch!");
    }
}

