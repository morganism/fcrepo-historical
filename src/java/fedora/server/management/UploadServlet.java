package fedora.server.management;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;

import com.oreilly.servlet.multipart.FilePart;
import com.oreilly.servlet.multipart.MultipartParser;
import com.oreilly.servlet.multipart.Part;

import fedora.common.Constants;
import fedora.server.Context;
import fedora.server.Logging;
import fedora.server.ReadOnlyContext;
import fedora.server.Server;
import fedora.server.errors.InitializationException;
import fedora.server.errors.ServerException;
import fedora.server.errors.authorization.AuthzException;
import fedora.server.errors.servletExceptionExtensions.RootException;

/**
 * Accepts and HTTP Multipart POST of a file from an authorized user, and if 
 * successful, returns a status of "201 Created" and a text/plain 
 * response with a single line containing an opaque identifier that can be 
 * used to later submit to the appropriate API-M method. If it fails it 
 * will return a non-201 status code with a text/plain explanation.
 *
 * The submitted file must be named "file", must not be accompanied by any other
 * parameters.
 *
 * Note: This class relies on a patched version of cos.jar that provides
 * an alternate constructor for MultiPartParser, allowing for the upload of 
 * files over 2GB in size.
 *
 * @author cwilper@cs.cornell.edu
 * @version $Id$
 */
public class UploadServlet 
        extends HttpServlet implements Logging {

    /** Content type for all responses. */
    private static final String CONTENT_TYPE_TEXT = "text/plain";

    /** Instance of the Fedora server. */
    private static Server s_server = null;

    /** Instance of Management subsystem (for storing uploaded files). */
    private static Management s_management = null;
    
    public static final String ACTION_LABEL = "Upload";

    /**
     * The servlet entry point.  http://host:port/fedora/management/upload
     */
    public void doPost(HttpServletRequest request, 
            HttpServletResponse response) 
            throws ServletException, IOException {
        String actionLabel = "uploading a file";    	
        Context context = ReadOnlyContext.getContext(Constants.HTTP_REQUEST.REST.uri, request);
		try {
            MultipartParser parser=new MultipartParser(request, 
                Long.MAX_VALUE, true, null);
			Part part=parser.readNextPart();
			if (part!=null && part.isFile()) {
			    if (part.getName().equals("file")) {
			    	String temp = saveAndGetId(context, (FilePart) part);
                    sendResponse(HttpServletResponse.SC_CREATED, temp, response);
				} else {
                    sendResponse(HttpServletResponse.SC_BAD_REQUEST,
                            "Content must be named \"file\"", response);
				}
			} else {
			    if (part==null) {
			        sendResponse(HttpServletResponse.SC_BAD_REQUEST, 
			                "No data sent.", response);
				} else {
			        sendResponse(HttpServletResponse.SC_BAD_REQUEST, 
			                "No extra parameters allowed", response);
				}
			}
    	} catch (AuthzException ae) {            
            throw RootException.getServletException (ae, request, ACTION_LABEL, new String[0]);	 				
		} catch (Exception e) {
                    e.printStackTrace();
		    sendResponse(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    e.getClass().getName() + ": " + e.getMessage(), response);
		}
    }

    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
		sendResponse(HttpServletResponse.SC_OK, 
		        "Client must use HTTP Multipart POST", response);
	}

    public void sendResponse(int status, String message, 
            HttpServletResponse response) {
        try {
            if (status==HttpServletResponse.SC_CREATED) {
                logFine("Successful upload, id=" + message);
            } else {
                logWarning("Failed upload: " + message);
            }
            response.setStatus(status);
            response.setContentType(CONTENT_TYPE_TEXT);
            PrintWriter w=response.getWriter();
            w.println(message);
        } catch (Exception e) {
            logSevere("Could not send a response: " + e.getClass().getName()
                    + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String saveAndGetId(Context context, FilePart filePart)
	        throws ServerException, IOException { 
		return s_management.putTempStream(context, filePart.getInputStream()); 
	}

    /**
     * Initialize servlet.  Gets a reference to the fedora Server object.
     *
     * @throws ServletException If the servet cannot be initialized.
     */
    public void init() throws ServletException {
        try {
            s_server=Server.getInstance(new File(System.getProperty("fedora.home")), false);
			s_management=(Management) s_server.getModule("fedora.server.management.Management");
			if (s_management==null) {
			    throw new ServletException("Unable to get Management module from server.");
			}
        } catch (InitializationException ie) {
            throw new ServletException("Unable to get Fedora Server instance."
                + ie.getMessage());
        }
    }

    public final Server getServer() {
        return s_server;
    }

  /**
   * Logs a SEVERE message, indicating that the server is inoperable or
   * unable to start.
   *
   * @param message The message.
   */
  public final void logSevere(String message) {
      StringBuffer m=new StringBuffer();
      m.append(getClass().getName());
      m.append(": ");
      m.append(message);
      getServer().logSevere(m.toString());
  }

  public final boolean loggingSevere() {
      return getServer().loggingSevere();
  }

  /**
   * Logs a WARNING message, indicating that an undesired (but non-fatal)
   * condition occured.
   *
   * @param message The message.
   */
  public final void logWarning(String message) {
      StringBuffer m=new StringBuffer();
      m.append(getClass().getName());
      m.append(": ");
      m.append(message);
      getServer().logWarning(m.toString());
  }

  public final boolean loggingWarning() {
      return getServer().loggingWarning();
  }

  /**
   * Logs an INFO message, indicating that something relatively uncommon and
   * interesting happened, like server or module startup or shutdown, or
   * a periodic job.
   *
   * @param message The message.
   */
  public final void logInfo(String message) {
      StringBuffer m=new StringBuffer();
      m.append(getClass().getName());
      m.append(": ");
      m.append(message);
      getServer().logInfo(m.toString());
  }

  public final boolean loggingInfo() {
      return getServer().loggingInfo();
  }

  /**
   * Logs a CONFIG message, indicating what occurred during the server's
   * (or a module's) configuration phase.
   *
   * @param message The message.
   */
  public final void logConfig(String message) {
      StringBuffer m=new StringBuffer();
      m.append(getClass().getName());
      m.append(": ");
      m.append(message);
      getServer().logConfig(m.toString());
  }

  public final boolean loggingConfig() {
      return getServer().loggingConfig();
  }

  /**
   * Logs a FINE message, indicating basic information about a request to
   * the server (like hostname, operation name, and success or failure).
   *
   * @param message The message.
   */
  public final void logFine(String message) {
      StringBuffer m=new StringBuffer();
      m.append(getClass().getName());
      m.append(": ");
      m.append(message);
      getServer().logFine(m.toString());
  }

  public final boolean loggingFine() {
      return getServer().loggingFine();
  }

  /**
   * Logs a FINER message, indicating detailed information about a request
   * to the server (like the full request, full response, and timing
   * information).
   *
   * @param message The message.
   */
  public final void logFiner(String message) {
      StringBuffer m=new StringBuffer();
      m.append(getClass().getName());
      m.append(": ");
      m.append(message);
      getServer().logFiner(m.toString());
  }

  public final boolean loggingFiner() {
      return getServer().loggingFiner();
  }

  /**
   * Logs a FINEST message, indicating method entry/exit or extremely
   * verbose information intended to aid in debugging.
   *
   * @param message The message.
   */
  public final void logFinest(String message) {
      StringBuffer m=new StringBuffer();
      m.append(getClass().getName());
      m.append(": ");
      m.append(message);
      getServer().logFinest(m.toString());
  }

  public final boolean loggingFinest() {
      return getServer().loggingFinest();
  }

}
