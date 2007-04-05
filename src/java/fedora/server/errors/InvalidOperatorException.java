/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://www.fedora.info/license/).
 */

package fedora.server.errors;

/**
 *
 * <p><b>Title:</b> InvalidOperatorException.java</p>
 * <p><b>Description:</b> Thrown when an operator is invalid.</p>
 *
 * @author cwilper@cs.cornell.edu
 * @version $Id$
 */
public class InvalidOperatorException
        extends ServerException {

	private static final long serialVersionUID = 1L;
	
    /**
     * Creates an InvalidOperatorException.
     *
     * @param message An informative message explaining what happened and
     *                (possibly) how to fix it.
     */
    public InvalidOperatorException(String message) {
        super(null, message, null, null, null);
    }

    public InvalidOperatorException(String bundleName, String code,
            String[] replacements, String[] details, Throwable cause) {
        super(bundleName, code, replacements, details, cause);
    }

}