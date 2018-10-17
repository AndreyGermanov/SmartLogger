package archivers;

import java.util.HashMap;

/**
 * Data archiver which sends files to remote FTP server
 */
public class SendFtpDataArchiver extends FileCopyDataArchiver {

    /**
     * Class constructor
     *
     * @param config Configuration object
     */
    SendFtpDataArchiver(HashMap<String, Object> config) {
        super(config);
    }

    /**
     * Method used to set parameters of archiver from provided configuration object
     * @param config Configuration object
     */
    public void configure(HashMap<String,Object> config) {
        config.put("type","send_ftp");
        super.configure(config);
    }
}
