/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://www.fedora.info/license/).
 */

package fedora.oai;

/**
 *
 * <p><b>Title:</b> NoMetadataFormatsException.java</p>
 * <p><b>Description:</b> Signals that there are no metadata formats available
 * for the specified item.</p>
 *
 * <p>This may occur while fulfilling a ListMetadataFormats request.</p>
 *
 * @author cwilper@cs.cornell.edu
 * @version $Id$
 */
public class NoMetadataFormatsException
        extends OAIException {

	private static final long serialVersionUID = 1L;
	
    public NoMetadataFormatsException() {
        super("noMetadataFormats", null);
    }

    public NoMetadataFormatsException(String message) {
        super("noMetadataFormats", message);
    }

}