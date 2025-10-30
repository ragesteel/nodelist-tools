package ru.gt2.ftn;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;

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
            int expectedCRC = Crc16.extractCRC(header);
            if (expectedCRC < 0) throw new IOException("Cannot extract CRC from header");

            Crc16 crc = new Crc16(CHARSET);
            String line;
            while ((line = br.readLine()) != null) {
                if (line.equals("\u001A")) {
                    break;
                }
                crc.updateCrcLine(line + "\r\n");
            }
            int calculatedCRC = crc.getCrc();
            System.out.printf("Expected CRC: %05d, Calculated CRC: %05d%n", expectedCRC, calculatedCRC);
            return calculatedCRC == expectedCRC;
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

