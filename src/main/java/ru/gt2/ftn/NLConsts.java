package ru.gt2.ftn;

import java.nio.charset.Charset;

public class NLConsts {
    public static final Charset CP_866 = Charset.forName("CP866");
    public static final String END_OF_FILE = "\u001A";
    public static final String CRLF = "\r\n";

    private NLConsts() {
    }
}
