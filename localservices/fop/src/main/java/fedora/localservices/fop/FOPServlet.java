/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.localservices.fop;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.avalon.framework.logger.ConsoleLogger;
import org.apache.avalon.framework.logger.Logger;

import org.apache.fop.apps.Driver;
import org.apache.fop.messaging.MessageHandler;

import org.xml.sax.InputSource;

/**
 * Servlet for generating and serving a PDF, given the URL to an XSL-FO file.
 * 
 * Servlet param is:
 * <ul>
 * <li>source: the path to a formatting object file to render
 * </ul>
 * 
 * @author Chris Wilper
 */
public class FOPServlet
        extends HttpServlet {

    private static final long serialVersionUID = 1L;

    public static final String FO_REQUEST_PARAM = "source";

    Logger log = null;

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException {
        if (log == null) {
            log = new ConsoleLogger(ConsoleLogger.LEVEL_WARN);
            MessageHandler.setScreenLogger(log);
        }
        try {
            String foParam = request.getParameter(FO_REQUEST_PARAM);

            if (foParam != null) {
                renderFO(new InputSource(foParam), response);
            } else {
                PrintWriter out = response.getWriter();
                out.println("<html><head><title>Error</title></head>\n"
                        + "<body><h1>FOPServlet Error</h1><h3>No 'source' "
                        + "request param given.</body></html>");
            }
        } catch (ServletException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ServletException(ex);
        }
    }

    public void renderFO(InputSource foFile, HttpServletResponse response)
            throws ServletException {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            response.setContentType("application/pdf");

            Driver driver = new Driver(foFile, out);
            driver.setLogger(log);
            driver.setRenderer(Driver.RENDER_PDF);
            driver.run();

            byte[] content = out.toByteArray();
            response.setContentLength(content.length);
            response.getOutputStream().write(content);
            response.getOutputStream().flush();
        } catch (Exception ex) {
            throw new ServletException(ex);
        }
    }

}