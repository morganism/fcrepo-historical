package fedora.server.journal.helpers;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import fedora.server.journal.JournalConstants;
import fedora.server.journal.JournalException;
import fedora.server.utilities.StreamUtility;

/**
 * 
 * <p>
 * <b>Title:</b> JournalHelper.java
 * </p>
 * <p>
 * <b>Description:</b> A collection of utility methods for use in the Journal
 * classes.
 * </p>
 * 
 * @author jblake@cs.cornell.edu
 * @version $Id$
 */
public class JournalHelper implements JournalConstants {

    private JournalHelper() {
        // no need to instantiate this class - all methods are static.
    }

    /**
     * Copy an input stream to a temporary file, so we can hand an input stream
     * to the delegate and have another input stream for the journal.
     */
    public static File copyToTempFile(InputStream serialization)
            throws IOException, FileNotFoundException {
        File tempFile = createTempFile();
        StreamUtility.pipeStream(serialization, new FileOutputStream(
                tempFile), 4096);
        return tempFile;
    }

    /**
     * Create a temporary file. The "File"
     * object that we return is really an instance of "JournalTempFile", so we
     * can detect it later in isTempFile().
     */
    public static File createTempFile() throws IOException {
        File rawTempFile = File.createTempFile("fedora-journal-temp", ".xml");
        return new JournalTempFile(rawTempFile);
    }
    
    /**
     * Is this file one that we created as a temp file?
     */
    public static boolean isTempFile(File file) {
        return (file instanceof JournalTempFile);
    }

    /**
     * Capture the full stack trace of an Exception, and return it in a String.
     */
    public static String captureStackTrace(Throwable e) {
        StringWriter buffer = new StringWriter();
        e.printStackTrace(new PrintWriter(buffer));
        return buffer.toString();
    }

    /**
     * Look in the system parameters and create an instance of the named class.
     * 
     * @param parameterName
     *            The name of the system parameter that contains the classname
     * @param argClasses
     *            What types of arguments are required by the constructor?
     * @param args
     *            Arguments to provide to the instance constructor.
     * @param parameters
     *            The system parameters
     * @return the new instance created
     */
    public static Object createInstanceAccordingToParameter(
            String parameterName, Class[] argClasses, Object[] args,
            Map parameters) throws JournalException {
        String className = (String) parameters.get(parameterName);
        if (className == null) {
            throw new JournalException("No parameter '" + parameterName + "'");
        }

        try {
            Class clazz = Class.forName(className);
            Constructor constructor = clazz.getConstructor(argClasses);
            return constructor.newInstance(args);
        } catch (Exception e) {
            throw new JournalException(e);
        }
    }

    /**
     * Format a date for the journal or the log.
     */
    public static String formatDate(Date date) {
        SimpleDateFormat formatter = new SimpleDateFormat(TIMESTAMP_FORMAT);
        return formatter.format(date);
    }

    /**
     * Parse a date from the journal.
     */
    public static Date parseDate(String date) throws JournalException {
        try {
            SimpleDateFormat parser = new SimpleDateFormat(TIMESTAMP_FORMAT);
            return parser.parse(date);
        } catch (ParseException e) {
            throw new JournalException(e);
        }
    }
}
