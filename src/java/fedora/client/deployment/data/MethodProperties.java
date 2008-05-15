/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://www.fedora.info/license/).
 */

package fedora.client.deployment.data;

/**
 * @author Sandy Payette
 */
public class MethodProperties {

    public static final String HTTP_MESSAGE_PROTOCOL = "HTTP";

    //public static final String SOAP_MESSAGE_PROTOCOL = "SOAP";

    // Data entered via MethodPropertiesDialog
    public MethodParm[] methodParms = new MethodParm[0];

    public String[] returnMIMETypes = new String[0];

    public String[] dsBindingKeys = new String[0];

    public String protocolType = null;

    public String methodRelativeURL = null;

    public String methodFullURL = null;

    public boolean wasValidated = false;

    public MethodProperties() {
    }
}