package archivers.processors;

import archivers.IDataArchiver;
import main.ISyslog;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;

/**
 * Archive processor, which sends source files to remote FTP server
 */
public class SendFtpArchiveProcessor extends ArchiveProcessor {

    // Link to FTP connection
    private FTPClient connection = new FTPClient();
    // FTP host
    private String host = "";
    // FTP port
    private int port = 21;
    // FTP login
    private String username = "";
    // FTP password
    private String password = "";
    // Should initiate passive connection
    private boolean passiveMode = true;
    // Path on FTP to which upload files (relative to user path on server)
    private String rootPath = "/";
    // FTP connection timeout in seconds
    private int connectionTimeout = 30;
    // FTP socket timeout in seconds (how much time to wait in case of IDLE or other stuck while uploading file)
    private int socketTimeout = 10;

    /// Failure which happened during file processing, which must interrupt stream and stop processing all
    /// files until the end of stream
    private boolean globalFailure = false;

    /**
     * Class constructor
     * @param archiver - Source Data archiver
     */
    SendFtpArchiveProcessor(IDataArchiver archiver) {
        super(archiver);
    }

    /**
     * Method used to set parameters of archiver from provided configuration object
     * @param config Configuration object
     */
    @Override
    public void configure(HashMap<String,Object> config) {
        super.configure(config);
        host = config.getOrDefault("host",host).toString();
        port = Double.valueOf(config.getOrDefault("port",port).toString()).intValue();
        username = config.getOrDefault("username",username).toString();
        password = config.getOrDefault("password",password).toString();
        rootPath = config.getOrDefault("rootPath",rootPath).toString();
        passiveMode = Boolean.parseBoolean(config.getOrDefault("passiveMode",passiveMode).toString());
        connectionTimeout = Double.valueOf(config.getOrDefault("connectionTimeout",connectionTimeout).toString()).intValue()*1000;
        socketTimeout =  Double.valueOf(config.getOrDefault("socketTimeout",socketTimeout).toString()).intValue()*1000;
    }

    /**
     * Method used to initialize archive before starting put files to it
     * @return True if archive initialized successfully or false otherwise
     */
    public boolean validateAndInitArchive() {
        String sourcePath = archiver.getSourcePath();
        globalFailure = false;
        if (!Files.exists(Paths.get(sourcePath))) return false;
        try {
            connection.setDataTimeout(socketTimeout);
            connection.connect(host,port);
            int reply = connection.getReplyCode();
            if (!FTPReply.isPositiveCompletion(reply)) {
               connection.disconnect();
               syslog.log(ISyslog.LogLevel.ERROR,"Could not initiate FTP connection. "+
                       "Error message: "+connection.getReplyString(),this.getClass().getName(),
                       "validateAndInitArchive");
               return false;
            }
            if (!connection.login(username,password)) {
               syslog.log(ISyslog.LogLevel.ERROR,"Could not initiate FTP connection. "+
                       "Invalid login or password.",this.getClass().getName(),"validateAndInitArchive");
               return false;
            }
            if (passiveMode) connection.enterLocalPassiveMode();
            if (!rootPath.equals("/")) {
               if (!connection.changeWorkingDirectory(rootPath)) {
                   syslog.log(ISyslog.LogLevel.ERROR, "Could not set FTP working directory '" + rootPath + "' ." +
                           "Error message '" + connection.getReplyString()+"'",
                           this.getClass().getName(), "validateAndInitArchive");
                   return false;
               }
            }
            connection.setFileType(FTP.BINARY_FILE_TYPE);
            connection.setConnectTimeout(connectionTimeout);
        } catch (IOException e) {
            syslog.log(ISyslog.LogLevel.ERROR,"Could not initiate FTP connection. "+
                    "Error message: "+e.getMessage(),this.getClass().getName(),"validateAndInitArchive");

        }
        return true;
    }

    /**
     * Method puts provided file to archive
     * @param sourceFile Path to source file to place to archive
     */
    @Override
    public void processFile(Path sourceFile) {
        if (globalFailure) return;
        try {
            connection.setSoTimeout(socketTimeout);
            if (connection.storeFile(sourceFile.getFileName().toString(),
                    new FileInputStream(sourceFile.toFile())))
                archiver.finishFileProcessing(sourceFile);
            else {
                syslog.log(ISyslog.LogLevel.ERROR, "Could not upload file by FTP: '" +
                                sourceFile.toString() + "'. Error message: "+connection.getReplyString(),
                        this.getClass().getName(), "processFile");
            }
        } catch (IOException e) {
            globalFailure = true;
            syslog.log(ISyslog.LogLevel.ERROR,"Could not upload file by FTP: '"+sourceFile.toString()+"'. "+
                    "Error message: "+e.getMessage(),this.getClass().getName(),"processFile");
        }
    }

    /**
     * Method called after last file placed to archive
     */
    @Override
    public void finish() {
        try {
            connection.disconnect();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
