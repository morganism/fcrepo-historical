/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://www.fedora.info/license/).
 */

package fedora.server.errors;

/**
 *
 * <p><b>Title:</b> StorageDeviceException.java</p>
 * <p><b>Description:</b> Signals that a storage device failed to behave as
 * expected.</p>
 *
 * @author cwilper@cs.cornell.edu
 * @version $Id$
 */
public class StorageDeviceException
        extends StorageException {

	private static final long serialVersionUID = 1L;
	
    /**
     * Creates a StorageDeviceException.
     *
     * @param message An informative message explaining what happened and
     *                (possibly) how to fix it.
     */
    public StorageDeviceException(String message) {
        super(message);
    }

    public StorageDeviceException(String message, Throwable cause) {
        super(message, cause);
    }

}