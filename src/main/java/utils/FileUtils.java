package utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.function.BinaryOperator;
import java.util.function.Function;

/**
 * Class with various functions related to filesystem
 */
public class FileUtils {

    /**
     * Function returns size of provided file
     * @param path - Path to file
     * @return Size of file in bytes
     */
    private static Function<Path,Long> folderSizeFunction = (Path path) -> {
        try {
            return Files.size(path);
        } catch (Exception e) { return 0L;}
    };

    /**
     * Function which used to increment count of files by one
     */
    private static Function<Path,Long> folderCountFunction = (Path path) -> 1L;

    /**
     * Function used as operator in stream to sum values
     */
    private static BinaryOperator<Long> sumReducer = (sum,value) -> sum += value;

    /**
     * Function used to remove folder and its content recursively
     * @param path: Path to folder
     * @param ifEmpty : If true, then removes subfolders only if they are empty, otherwise delete everything
     */
    public static void removeFolder(Path path,boolean ifEmpty) {
        if (Files.notExists(path)) return;
        try {
            Files.walk(path).sorted(Comparator.comparingInt(Path::getNameCount).reversed()).forEach(file -> {
                if (Files.isDirectory(file)) {
                    long count = getFolderFilesCount(file);
                    if (count == 0) {
                        try { Files.delete(file); } catch (IOException e) {e.printStackTrace();}
                    }
                } else if (!ifEmpty) {
                    try { Files.delete(file); } catch (IOException e) {e.printStackTrace();}
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Function returns number of entries in specified folder (including files and subfolders)
     * @param path - Path to folder
     * @return number of records
     */
    public static long getFolderFilesCount(Path path) {
        return getFolderMetric(path, folderCountFunction, sumReducer)-1;
    }

    /**
     * Function returns summary size of all files inside folder
     * @param path - Path to folder
     * @return Size of files in folder in bytes
     */
    public static long getFolderFilesSize(Path path) {
        return getFolderMetric(path, folderSizeFunction, sumReducer);
    }

    /**
     * Base function used to calculate something related to content in folder
     * @param path Path to folder
     * @param func Function which gets value related to each file to calculate (map)
     * @param operator Function used to calculate value related to all files (reduce)
     * @return Calculated value
     */
    private static Long getFolderMetric(Path path, Function<Path,Long> func, BinaryOperator<Long> operator) {
        if (Files.notExists(path)) return 0L;
        try {
            return Files.walk(path).map(func).reduce(operator).orElse(0L);
        } catch (IOException e) { e.printStackTrace();}
        return 0L;
    }
}