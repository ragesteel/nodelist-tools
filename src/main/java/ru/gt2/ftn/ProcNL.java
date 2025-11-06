package ru.gt2.ftn;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/// Обработка нодлистов: применение Diff
public class ProcNL {
    private final Path nodelistPath;

    private final List<Path> diffPaths;

    public ProcNL(Path nodelistPath, List<Path> diffPaths) {
        this.nodelistPath = nodelistPath;
        this.diffPaths = diffPaths;
    }

    public void process() throws IOException {
        // Если nodelistPath - директория, обработать все файлы внутри неё (каждый как отдельный нодлист)
        // иначе - обработать только один файл (стандартная логика)
        Multimap<String, Integer> nodeListNames = MultimapBuilder.linkedHashKeys().treeSetValues().build();

        if (Files.isDirectory(nodelistPath)) {
            try (Stream<Path> files = Files.list(nodelistPath)) {
                files.filter(Files::isRegularFile)
                 .forEach(filePath -> {
                     String filePathString = filePath.toString();
                     String fileExtension = com.google.common.io.Files.getFileExtension(filePathString);
                     if (3 != fileExtension.length()) {
                         return;
                     }

                     String name = com.google.common.io.Files.getNameWithoutExtension(filePathString);

                     // Для каждого файла применить diff-ы (copy-paste логика ниже)
                    Path tmpOld = filePath;
                    try {
                        for (Path diffPath : diffPaths) {
                            Path tmpNew = Files.createTempFile("nodelist_new_", ".txt");
                            try (
                                InputStream nodelistIn = Files.newInputStream(tmpOld);
                                InputStream diffIn = Files.newInputStream(diffPath);
                                OutputStream resultOut = Files.newOutputStream(tmpNew)
                            ) {
                                new NodeDiffApplier(NLConsts.CP_866).apply(nodelistIn, diffIn, resultOut);
                            }
                            tmpOld = tmpNew;
                        }
                        Files.copy(
                            tmpOld,
                            filePath,
                            java.nio.file.StandardCopyOption.REPLACE_EXISTING
                        );
                        System.out.println("DIFFs successfully applied. Updated nodelist: " + filePath);
                    } catch (Exception ex) {
                        throw new RuntimeException("Failed to process nodelist file " + filePath + " with diffs: " + ex.getMessage(), ex);
                    }
                 });
            }
            // После обработки директории не продолжаем дальше
            return;
        }
        // Для этой функции: Применяем диффы к одному нодлисту по порядку

        Path currentNodelist = nodelistPath;
        try {
            // Читаем весь нодлист в память
            Path tmpOld = currentNodelist;
            for (Path diffPath : diffPaths) {
                // Создаём временный файл для результата применения diff
                Path tmpNew = Files.createTempFile("nodelist_new_", ".txt");
                try (
                    InputStream nodelistIn = Files.newInputStream(tmpOld);
                    InputStream diffIn = Files.newInputStream(diffPath);
                    OutputStream resultOut = Files.newOutputStream(tmpNew)
                ) {
                    new NodeDiffApplier(NLConsts.CP_866).apply(nodelistIn, diffIn, resultOut);
                }
                // Следующий проход будет брать свежий файл
                tmpOld = tmpNew;
            }

            // Финальный результат: tmpOld содержит новый нодлист
            // Сохраняем его обратно (перезаписываем исходный)
            Files.copy(
                tmpOld,
                nodelistPath,
                java.nio.file.StandardCopyOption.REPLACE_EXISTING
            );
            System.out.println("DIFFs successfully applied. Updated nodelist: " + nodelistPath);

        } catch (Exception ex) {
            throw new RuntimeException("Failed to process nodelist with diffs: " + ex.getMessage(), ex);
        }
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: java ProcNL <nodelistPath> <diffPath1> [<diffPath2> ...]");
            System.exit(1);
        }

        Path nodelistPath = Path.of(args[0]);
        List<Path> diffPaths = Arrays.stream(args)
                .skip(1)
                .map(Path::of)
                .toList();
        //new ProcNL(nodelistPath, diffPaths).process();
    }

}
