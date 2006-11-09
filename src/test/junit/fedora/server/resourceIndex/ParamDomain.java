package fedora.server.resourceIndex;

import java.util.TreeSet;

/**
 * A sorted set of domain values for a parameter.
 *
 * As per the <code>SortedSet</code> contract, iterators over the values
 * in this collection will provide the elements in ascending order.
 *
 * @author cwilper@cs.cornell.edu
 */
public class ParamDomain extends TreeSet<String> {

    /**
     * The parameter whose domain is being described.
     */
    private String _parameterName;

    /**
     * Whether specifying a value is required.
     */
    private boolean _isRequired;

    /**
     * Construct an empty <code>ParamDomain</code>.
     *
     * @param parameterName the parameter whose domain is being described.
     * @param isRequired whether specifying a value is required.
     */
    public ParamDomain(String parameterName, boolean isRequired) {
        _parameterName = parameterName;
        _isRequired = isRequired;
    }

    /**
     * Construct a <code>ParamDomain</code> with values from the given array.
     *
     * @param parameterName the parameter whose domain is being described.
     * @param isRequired whether specifying a value is required.
     * @param values the domain values.
     */
    public ParamDomain(String parameterName, boolean isRequired, 
            String[] domainValues) {
        _parameterName = parameterName;
        _isRequired = isRequired;
        for (int i = 0; i < domainValues.length; i++) {
            add(domainValues[i]);
        }
    }

    /**
     * Get the name of the parameter whose domain is being described.
     */
    public String getParameterName() {
        return _parameterName;
    }

    /**
     * Tell whether specifying a value is required.
     */
    public boolean isRequired() {
        return _isRequired;
    }

}