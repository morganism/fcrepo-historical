package fedora.server.storage;

import java.io.InputStream;
import java.util.Date;

import fedora.server.errors.ServerException;
import fedora.server.storage.types.Datastream;
import fedora.server.storage.types.Disseminator;

/**
 * The standard interface for write operations on a digital object.
 * <p></p>
 * A <code>DOWriter</code> instance is a handle on a Fedora digital object,
 * and is obtained via a <code>getWriter(String)</code> call on a 
 * <code>DOManager</code>.
 * <p></p>
 * This interface supports transaction behavior with the commit(String) and
 * rollBack() methods.  When a DOWriter is instantiated, there is an implicit
 * transaction.  Write methods may be called, but they won't affect the
 * the underlying data store until commit(String) is invoked.  This also has
 * the effect of creating another implicit transaction.  If temporary
 * changes are no longer wanted, rollBack() may be called to return the object 
 * to it's original form.  rollBack() is only valid for the current transaction.
 * <p></p>
 * The read methods of DOWriter reflect on the composition of the object in
 * the context of the current transaction.
 *
 * @author cwilper@cs.cornell.edu
 */
public interface DOWriter 
        extends DOReader {

    /**
     * Sets the content of the entire digital object.
     *
     * @param content A stream of encoded content of the digital object.
     * @throws ServerException If any type of error occurred fulfilling the 
     *         request.
     */
    public void set(InputStream content) 
            throws ServerException;

    /**
     * Sets the state of the entire digital object.
     *
     * @param state The state.
     * @throws ServerException If any type of error occurred fulfilling the 
     *         request.
     */
    public void setState(String state) 
            throws ServerException;

    /**
     * Removes the entire digital object.
     *
     * @throws ServerException If any type of error occurred fulfilling the 
     *         request.
     */    
    public void remove() 
            throws ServerException;

    /**
     * Adds a datastream to the object.
     *
     * @param datastream The datastream.
     * @return An internally-unique datastream id.
     * @throws ServerException If any type of error occurred fulfilling the 
     *         request.
     */
    public String addDatastream(Datastream datastream) 
            throws ServerException;

    /**
     * Adds a disseminator to the object.
     *
     * @param disseminator The disseminator.
     * @return An internally-unique disseminator id.
     * @throws ServerException If any type of error occurred fulfilling the 
     *         request.
     */
    public String addDisseminator(Disseminator disseminator) 
            throws ServerException;

    /**
     * Removes a datastream from the object.
     *
     * @param id The id of the datastream.
     * @param start The start date (inclusive) of versions to remove.  If 
     *        <code>null</code>, this is taken to be the smallest possible 
     *        value.
     * @param end The end date (inclusive) of versions to remove.  If 
     *        <code>null</code>, this is taken to be the greatest possible 
     *        value.
     * @throws ServerException If any type of error occurred fulfilling the 
     *         request.
     */
    public void removeDatastream(String id, Date start, Date end) 
            throws ServerException;

    /**
     * Removes a disseminator from the object.
     *
     * @param id The id of the datastream.
     * @param start The start date (inclusive) of versions to remove.  If 
     *        <code>null</code>, this is taken to be the smallest possible 
     *        value.
     * @param end The end date (inclusive) of versions to remove.  If 
     *        <code>null</code>, this is taken to be the greatest possible 
     *        value.
     * @throws ServerException If any type of error occurred fulfilling the 
     *         request.
     */
    public void removeDisseminator(String id, Date start, Date end) 
            throws ServerException;

    /**
     * Saves the changes thus far to the permanent copy of the digital object.
     *
     * @param logMessage An explanation of the change(s).
     * @throws ServerException If any type of error occurred fulfilling the 
     *         request.
     */
    public void commit(String logMessage) 
            throws ServerException;

    /**
     * Clears the temporary storage area of changes to this object.
     * <p></p>
     * Subsequent calls will behave as if the changes made thus far never 
     * happened.
     *
     * @throws ServerException If any type of error occurred fulfilling the 
     *         request.
     */
    public void rollBack() 
            throws ServerException;
}