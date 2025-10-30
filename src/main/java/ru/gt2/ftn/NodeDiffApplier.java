package ru.gt2.ftn;

import java.io.*;
import java.nio.charset.Charset;

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

    // пример запуска
    public static void main(String[] args) throws Exception {
        if (args.length != 3) {
            System.err.println("Usage: java NodeDiffApplier <oldlist> <nodediff> <newlist>");
            System.exit(1);
        }
        try (
            InputStream oldListIn = new FileInputStream(args[0]);
            InputStream diffFileIn = new FileInputStream(args[1]);
            OutputStream newListOut = new FileOutputStream(args[2])
        ) {
            new NodeDiffApplier(NLConsts.CP_866).apply(oldListIn, diffFileIn, newListOut);
        }
        System.out.println("NODEDIFF applied successfully.");
    }
}