package ru.gt2.ftn;

import com.google.common.io.Files;

import java.io.*;
import java.nio.charset.Charset;

public class NodeDiffApplier {

    private final Charset charset;

    public NodeDiffApplier(Charset charset) {
        this.charset = charset;
    }

    /** Применить nodediff через потоки */
    public void apply(InputStream oldInputStream, InputStream diffInputStream, OutputStream newOutputStream) throws IOException {
        try (BufferedReader oldReader = new BufferedReader(new InputStreamReader(oldInputStream, charset));
                BufferedReader diffReader = new BufferedReader(new InputStreamReader(diffInputStream, charset));
                BufferedWriter outputWriter = new BufferedWriter(new OutputStreamWriter(newOutputStream, charset))) {

            DiffProcessor context = new DiffProcessor(oldReader, diffReader, outputWriter, charset);
            context.checkHeaders();
            context.processDiffCycle();
            context.checkCRC();
        }
    }

    public static void main(String[] args) throws Exception {
        if ((args.length < 2) || (args.length > 3)) {
            System.err.println("Usage: java NodeDiffApplier <oldlist> <nodediff> (<newlist>)");
            System.exit(1);
        }

        String oldList = args[0];
        String diff = args[1];
        String newList;
        if (args.length == 2) {
            newList = Files.getNameWithoutExtension(oldList) + '.' + Files.getFileExtension(diff);
        } else {
            newList = args[2];
        }
        process(oldList, diff, newList);
        System.out.printf("NODEDIFF applied successfully, %s + %s = %s%n", oldList, diff, newList);
    }

    private static void process(String initialNodeList, String diff, String newNodeList) throws IOException {
        try (InputStream oldListIn = new FileInputStream(initialNodeList);
             InputStream diffFileIn = new FileInputStream(diff);
             OutputStream newListOut = new FileOutputStream(newNodeList)) {
            new NodeDiffApplier(NLConsts.CP_866).apply(oldListIn, diffFileIn, newListOut);
        }
    }
}