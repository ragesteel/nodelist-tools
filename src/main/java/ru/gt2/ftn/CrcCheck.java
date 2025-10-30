package ru.gt2.ftn;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

/**
 Проверка CRC нодлиста согласно FTS-5000.005 §5.2
 CRC16-CCITT (poly 0x1021, init 0x0000)
 Вычисляется со второй строки, включая все символы CR/LF.
 */
public class CrcCheck {
    private final Charset charset;

    public CrcCheck(Charset charset) {
        this.charset = charset;
    }

    /** Проверяет CRC и возвращает true, если совпадает */
    public CheckResult verifyCrc(InputStream inputStream) throws IOException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, charset))) {
            String header = br.readLine();
            if (header == null) {
                throw new IOException("Empty file");
            }
            int expectedCRC = Crc16.extractCRC(header);
            if (expectedCRC < 0) {
                throw new IOException("Cannot extract CRC from header");
            }

            Crc16 crc = new Crc16(charset);
            String line;
            while ((line = br.readLine()) != null) {
                if (line.equals(NLConsts.END_OF_FILE)) {
                    break;
                }
                crc.updateCrcLine(line + NLConsts.CRLF);
            }
            int calculatedCRC = crc.getCrc();
            System.out.printf("Expected CRC: %05d, Calculated CRC: %05d%n", expectedCRC, calculatedCRC);
            return new CheckResult(calculatedCRC == expectedCRC, header);
        }
    }

    // пример использования
    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.err.println("Usage: java NodelistCRCChecker <NODELIST.nnn>");
            System.exit(1);
        }
        boolean ok = new CrcCheck(NLConsts.CP_866).verifyCrc(new FileInputStream(args[0])).valid();
        System.out.println(ok ? "CRC OK" : "CRC mismatch!");
    }
}

