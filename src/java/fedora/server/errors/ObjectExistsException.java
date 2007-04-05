/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://www.fedora.info/license/).
 */

package fedora.server.errors;

/**
 *
 * <p><b>Title:</b> ObjectExistsException.java</p>
 * <p><b>Description:</b> Signals that an object existed when it wasn't
 * expected to have existed.</p>
 *
 * @author cwilper@cs.cornell.edu
 * @version $Id$
 */
public class ObjectExistsException
        extends StorageException {

	private static final long serialVersionUID = 1L;
	
    /**
     * Creates an ObjectExistsException.
     *
     * @param message An informative message explaining what happened and
     *                (possibly) how to fix it.
     */
    public ObjectExistsException(String message) {
        super(message);
    }

}