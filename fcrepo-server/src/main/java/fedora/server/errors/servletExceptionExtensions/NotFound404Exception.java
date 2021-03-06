/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.errors.servletExceptionExtensions;

import javax.servlet.http.HttpServletRequest;

/**
 * Thrown to reach 404 Not Found error page.
 *
 * @author Chris Wilper
 */
public class NotFound404Exception
        extends RootException {

    private static final long serialVersionUID = 1L;

    public NotFound404Exception(HttpServletRequest request,
                                String action,
                                String detail,
                                String[] details) {
        super(request, action, detail, details);
    }

    public NotFound404Exception(String message,
                                HttpServletRequest request,
                                String action,
                                String detail,
                                String[] details) {
        super(message, request, action, detail, details);
    }

    public NotFound404Exception(String message,
                                Throwable cause,
                                HttpServletRequest request,
                                String action,
                                String detail,
                                String[] details) {
        super(message, cause, request, action, detail, details);
    }

}
