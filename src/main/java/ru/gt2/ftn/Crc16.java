package ru.gt2.ftn;

import java.nio.charset.Charset;

/// CRC-16-CCITT (poly 0x1021, init 0x0000)
public class Crc16 {
    /// таблица CRC-16/CCITT
    private static final int[] CRC_TABLE = new int[256];

    private final Charset charset;
    private int crc;

    static {
        for (int i = 0; i < 256; i++) {
            int c = i << 8;
            for (int j = 0; j < 8; j++)
                c = ((c & 0x8000) != 0) ? (c << 1) ^ 0x1021 : (c << 1);
            CRC_TABLE[i] = c & 0xFFFF;
        }
    }

    public Crc16(Charset charset) {
        this.charset = charset;
    }

    /** Извлекает CRC из первой строки (после последнего ':') */
    static int extractCRC(String header) {
        int colon = header.lastIndexOf(':');
        if (colon < 0 || colon + 1 >= header.length()) return -1;
        try {
            return Integer.parseInt(header.substring(colon + 1).trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * обновление CRC-16 CCITT на одной строке (с CRLF)
     */
    public void updateCrcLine(String line) {
        for (byte b : line.getBytes(charset)) {
            updCrcByte(b);
        }
    }

    public int getCrc() {
        return crc & 0xFFFF;
    }

    /// CRC-16 CCITT, poly 0x1021
    private void updCrcByte(byte b) {
        int v = (b ^ (crc >>> 8)) & 0xFF;
        crc = ((crc << 8) ^ CRC_TABLE[v]) & 0xFFFF;
    }
}
