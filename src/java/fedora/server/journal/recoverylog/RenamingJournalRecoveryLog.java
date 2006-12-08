package fedora.server.journal.recoverylog;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.Map;

import org.apache.log4j.Logger;

import fedora.server.errors.ModuleInitializationException;
import fedora.server.journal.ServerInterface;
import fedora.server.journal.helpers.JournalHelper;
import fedora.server.journal.readerwriter.multifile.MultiFileJournalHelper;

/**
 * A production-oriented implementation of {@link JournalRecoveryLog}. 
 * 
 * The name of the log file contains a time-stamp, so we can be sure that it 
 * won't over-write an existing log file. Also, the name is prefixed with an 
 * '_' (underscore) while the recovery is in progress, so we can see when it 
 * finishes.
 * 
 * @author jblake@cs.cornell.edu
 * @version $Id$
 */

public class RenamingJournalRecoveryLog extends JournalRecoveryLog {

    /** Logger for this class. */
    private static final Logger LOG = Logger.getLogger(
            RenamingJournalRecoveryLog.class.getName());

    /** The name of the log file, when it is complete. */
    String fileName;

    /** The name of the log file, while it is running. */
    String tempFileName;

    private final File logFile;

    private final FileWriter writer;

    private boolean open = true;

    /**
     * @param parameters
     * @param role
     * @param server
     * @throws ModuleInitializationException
     */
    public RenamingJournalRecoveryLog(Map parameters, String role,
            ServerInterface server) throws ModuleInitializationException {
        super(parameters, role, server);

        try {
            if (!parameters.containsKey(PARAMETER_RECOVERY_LOG_FILENAME)) {
                throw new ModuleInitializationException("Parameter '"
                        + PARAMETER_RECOVERY_LOG_FILENAME + "' is not set.",
                        role);
            }
            String fileNameTemplate = (String) parameters
                    .get(PARAMETER_RECOVERY_LOG_FILENAME);
            this.fileName = MultiFileJournalHelper.createTimestampedFilename(
                    fileNameTemplate, new Date());
            this.tempFileName = insertHyphenBeforeFilename(this.fileName);
            this.logFile = new File(this.tempFileName);
            this.writer = new FileWriter(this.logFile);

            super.logHeaderInfo(parameters);
        } catch (IOException e) {
            throw new ModuleInitializationException(
                    "Problem writing to the recovery log", role, e);
        }
    }

    private String insertHyphenBeforeFilename(String name) {
        int lastSlash = name.lastIndexOf(File.separatorChar);
        if (lastSlash == -1) {
            return '_' + name;
        } else {
            return name.substring(0, lastSlash + 1) + '_'
                    + name.substring(lastSlash + 1);
        }
    }

    /**
     * A request to log a message just writes it to the log file and flushes it
     * (if shutdown has not been called).
     */
    public synchronized void log(String message) {
        try {
            if (open) {
                super.log(message, writer);
                writer.flush();
            }
        } catch (IOException e) {
            LOG.error("Unable to write journal log message", e);
        }
    }

    /**
     * On the first call to this method, close the log file and rename it. Set
     * the flag so no more logging calls will be accepted.
     */
    public synchronized void shutdown() {
        try {
            if (open) {
                open = false;
                writer.close();
                logFile.renameTo(new File(this.fileName));
            }
        } catch (IOException e) {
            LOG.error("Error shutting down", e);
        }
    }

    public String toString() {
        return super.toString() + ", logFile='" + this.logFile.getPath() + "'";
    }

}
