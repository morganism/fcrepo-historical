package fedora.server.storage.types;

import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * A holder of Fedora digital object information.
 * <p></p>
 * A DigitalObject instance may be used by DOReader and DOWriter instances 
 * as temporary storage for an object's attributes and components.
 * <p></p>
 * Implementations of this interface are responsible for temporary 
 * storage of these items, by whatever mechanism they deem fit.  The most 
 * obvious implementation would simply store everything in memory.
 * <p></p>
 * Implementations of this interface are <b>not</b> responsible for any sort of
 * validation on these items, or serialization/deserialization to/from specific
 * formats.
 *
 * @author cwilper@cs.cornell.edu
 */
public interface DigitalObject {

    public static int FEDORA_OBJECT=0;
    public static int FEDORA_BDEF_OBJECT=1;    
    public static int FEDORA_BMECH_OBJECT=2;

    public int getFedoraObjectType();

    public void setFedoraObjectType(int t);

    /**
     * Gets the pid.
     *
     * @return The pid, or null if it hasn't been set.
     */
    public String getPid();

    /**
     * Sets the pid.
     *
     * @param pid The pid.
     */
    public void setPid(String pid);

    /**
     * Gets the state.
     *
     * @return The state, or null if it hasn't been set.
     */
    public String getState();

    /**
     * Sets the state.
     *
     * @param state The state.
     */
    public void setState(String state);

    /**
     * Gets the label.
     *
     * @return The label, or null if it hasn't been set.
     */    
    public String getLabel();

    /**
     * Sets the label.
     *
     * @param label The label.
     */    
    public void setLabel(String label);
    
    /**
     * Gets the content model id.
     *
     * @return The content model id.
     */
    public String getContentModelId();
    
    /**
     * Sets the content model id.
     *
     * @param id The content model id.
     */
    public void setContentModelId(String id);
    
    /**
     * Gets the date the object was created.
     *
     * @return The date, or null if it hasn't been set.
     */
    public Date getCreateDate();

    /**
     * Sets the date the object was created.
     *
     * @param createDate The date.
     */
    public void setCreateDate(Date createDate);

    /**
     * Gets the date the object was last modified.
     *
     * @return The date, or null if it hasn't been set.
     */    
    public Date getLastModDate();

    /**
     * Sets the date the object was last modified.
     *
     * @param date The date.
     */
    public void setLastModDate(Date lastModDate);
    
    /**
     * Sets the preferred namespace uri-to-prefix map for XML renditions of
     * this object or components.
     * 
     * @param mapping The uri-to-prefix mapping.
     */
    public void setNamespaceMapping(Map mapping);
    
    /**
     * Gets the preferred namespace uri-to-prefix map for XML renditions of
     * this object or components.
     *
     * @return The uri-to-prefix mapping.
     */
    public Map getNamespaceMapping();

    /**
     * Gets this object's mutable List of AuditRecord objects.
     *
     * @return The List of AuditRecords, possibly of zero size but never null.
     */     
    public List getAuditRecords();

    /**
     * Gets an Iterator over the ids for which there exist one or more 
     * datastreams in this object.
     * <p></p>
     * The Iterator is not tied to the underlying Collection and cannot
     * be used to remove datastreams.
     *
     * @return A new Iterator of datastream ids, possibly of zero size but 
     *         never null.
     */
    public Iterator datastreamIdIterator();

    /**
     * Gets a mutable List of Datastreams in this object whose ids match the 
     * provided id.
     *
     * @param id The datastream id.
     * @return The list, possibly of zero size but never null.
     */
    public List datastreams(String id);

    /**
     * Gets an Iterator over the ids for which there exist one or more 
     * disseminators in this object.
     * <p></p>
     * The Iterator is not tied to the underlying Collection and cannot
     * be used to remove datastreams.
     *
     * @return A new Iterator of disseminator ids, possibly of zero size but 
     *         never null.
     */
    public Iterator disseminatorIdIterator();
    
    /**
     * Gets a mutable List of Disseminators in this object whose ids match the 
     * provided id.
     *
     * @param id The datastream id.
     * @return The list, possibly of zero size but never null.
     */
    public List disseminators(String id);
    
}