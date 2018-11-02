package rotators;

import config.ConfigManager;
import cronjobs.CronjobTask;
import cronjobs.CronjobTaskStatus;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Class used to implement file rotation. It can be applied to any file, but in most case used
 * to rotate log files.
 */
public class FileRotator extends CronjobTask implements IFileRotator {

    // Path to file which used as source for rotation
    private Path filePath = null;
    // Maximum number of archives in rotation
    private int maxArchives = 5;
    // Maximum size of source file, if file is bigger than this rotator will rotate it
    private long maxSourceFileSize = 1024L;
    // Should source file be removed after rotation
    private boolean removeSourceFileAfterRotation = false;
    // Should archives in rotation be compressed by zip
    private boolean compressArchives = false;
    // Unique name of this file rotator
    private String name = "";
    // Extension of archive file
    private String fileExt = ".zip";

    /**
     * Class constructor
     * @param name Name of rotator in configuration file
     */
    public FileRotator(String name) {
        this.configure(ConfigManager.getInstance().getConfigNode("rotators",name));
    }

    /**
     * Class constructor
     * @param config Configuration object for rotator
     */
    public FileRotator(HashMap<String,Object> config) {
        this.configure(config);
    }

    /**
     * Method used to apply configuration to object.
     * @param config: Configuration object
     */
    public void configure(HashMap<String,Object> config) {
        if (config == null || !config.containsKey("filePath") ) return;
        super.configure(config);
        name = config.getOrDefault("name",name).toString();
        filePath = Paths.get(config.getOrDefault("filePath",filePath).toString());
        maxArchives = Double.valueOf(config.getOrDefault("maxArchives",maxArchives).toString()).intValue();
        maxSourceFileSize = Double.valueOf(config.getOrDefault("maxSourceFileSize",maxSourceFileSize).toString()).longValue();
        removeSourceFileAfterRotation = Boolean.parseBoolean(
            config.getOrDefault("removeSourceFileAfterRotation",removeSourceFileAfterRotation).toString()
        );
        compressArchives = Boolean.parseBoolean(config.getOrDefault("compressArchives",compressArchives).toString());
        fileExt = compressArchives ? ".zip" : ".bak";
    }

    /**
     * Method, which Timer used to run this object as a Cronjob ("TimerTask" implementation)
     */
    public void run() {
        super.run();
        rotate();
        setTaskStatus(CronjobTaskStatus.IDLE);
        setLastExecTime(Instant.now().getEpochSecond());
    }

    /**
     * Main method, which begins rotation process
     */
    public void rotate() {
        try {
            if (Files.notExists(filePath) || Files.size(filePath) < maxSourceFileSize) return;
            rotateArchives();
            createArchive();
            if (removeSourceFileAfterRotation) Files.delete(filePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Method rotates all previous archives (removes oldest one and moves all archives back by one position)
     */
    void rotateArchives() throws IOException {
        List<Path> logFiles = getArchives();
        String numberFormat = "%0"+String.valueOf(maxArchives).length()+"d";
        if (logFiles.size()>=maxArchives) {
            logFiles.sort(Comparator.reverseOrder());
            for (int start=0;start<=logFiles.size()-maxArchives;start++)
                Files.deleteIfExists(logFiles.get(start));
            logFiles = getArchives();
        }
        if (logFiles.size()>0) {
            for (int start = logFiles.size(); start > 0; start--) {
                Files.move(logFiles.get(start-1),
                    Paths.get(filePath.getParent().toString()+"/"+
                    filePath.getFileName().toString() + "_" + String.format(numberFormat,start + 1) + fileExt),
                    StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    /**
     * Method used to get list of archives, created by this rotator
     * @return List of paths of archives
     */
    List<Path> getArchives() throws IOException {
        return Files.walk(filePath.getParent(), 1)
                .filter(path -> path.getFileName().toString().startsWith(filePath.getFileName().toString())
                        && path.toString().endsWith(fileExt))
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * Method used to create new archive from source file
     */
    void createArchive() throws IOException {
        String numberFormat = "%0"+String.valueOf(maxArchives).length()+"d";
        String archivePath = filePath.getParent().toString()+"/"+filePath.getFileName().toString()+
                "_"+String.format(numberFormat,1)+fileExt;
        if (compressArchives) createArchiveZip(archivePath);else createArchiveBak(archivePath);
    }

    /**
     * Method used to create compressed archive
     * @param archivePath
     */
    void createArchiveZip(String archivePath) throws IOException {
        ZipOutputStream out = new ZipOutputStream(new FileOutputStream(archivePath));
        out.putNextEntry(new ZipEntry(filePath.getFileName().toString()));
        byte[] buffer = new byte[1024];
        InputStream in = new FileInputStream(filePath.toString());
        int length;
        while ((length = in.read(buffer))>0) out.write(buffer,0,length);
        out.closeEntry();
        out.close();
    }

    /**
     * Method used to create uncompressed archive
     * @param archivePath
     */
    void createArchiveBak(String archivePath) throws IOException {
        Files.copy(filePath,Paths.get(archivePath), StandardCopyOption.REPLACE_EXISTING);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Object getLastRecord() {
        return null;
    }

    @Override
    public long getLastRecordTimestamp() {
        return 0L;
    }

}