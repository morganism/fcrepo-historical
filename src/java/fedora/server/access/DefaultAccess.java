/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://www.fedora.info/license/).
 */

package fedora.server.access;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.net.URLDecoder;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.log4j.Logger;
import org.jrdf.graph.Triple;
import org.trippi.RDFFormat;
import org.trippi.TripleIterator;
import org.trippi.TrippiException;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import fedora.common.Constants;
import fedora.server.Context;
import fedora.server.Module;
import fedora.server.Server;
import fedora.server.access.dissemination.DisseminationService;
import fedora.server.errors.DatastreamNotFoundException;
import fedora.server.errors.DisseminatorNotFoundException;
import fedora.server.errors.InvalidUserParmException;
import fedora.server.errors.MethodNotFoundException;
import fedora.server.errors.ModuleInitializationException;
import fedora.server.errors.GeneralException;
import fedora.server.errors.ObjectIntegrityException;
import fedora.server.errors.RepositoryConfigurationException;
import fedora.server.errors.ServerException;
import fedora.server.errors.StreamIOException;
import fedora.server.management.DefaultManagement;
import fedora.server.search.FieldSearchQuery;
import fedora.server.search.FieldSearchResult;
import fedora.server.security.Authorization;
import fedora.server.storage.DOReader;
import fedora.server.storage.BDefReader;
import fedora.server.storage.BMechReader;
import fedora.server.storage.DOManager;
import fedora.server.storage.ExternalContentManager;
import fedora.server.storage.SimpleDOReader;
import fedora.server.storage.types.DSBinding;
import fedora.server.storage.types.Datastream;
import fedora.server.storage.types.DatastreamDef;
import fedora.server.storage.types.DatastreamReferencedContent;
import fedora.server.storage.types.DatastreamManagedContent;
import fedora.server.storage.types.DatastreamXMLMetadata;
import fedora.server.storage.types.DisseminationBindingInfo;
//import fedora.server.storage.types.Disseminator;
import fedora.server.storage.types.MethodDef;
import fedora.server.storage.types.MethodDefOperationBind;
import fedora.server.storage.types.MethodParmDef;
import fedora.server.storage.types.MIMETypedStream;
import fedora.server.storage.types.ObjectMethodsDef;
import fedora.server.storage.types.Property;
import fedora.server.storage.types.RelationshipTuple;
import fedora.server.utilities.DateUtility;
import fedora.server.utilities.ParserUtilityHandler;

/**
 *
 * <p><b>Title: </b>DefaultAccess.java</p>
 *
 * <p><b>Description: </b>The Access Module, providing support for the Fedora
 * Access subsystem.</p>
 *
 * @author rlw@virginia.edu
 * @version $Id$
 */
public class DefaultAccess extends Module implements Access
{

  /** Logger for this class. */
  private final static Logger LOG = Logger.getLogger(
        Access.class.getName());

  /** Current DOManager of the Fedora server. */
  private DOManager m_manager;

  /** OAI Provider domain name, for the describe request's identifier info. */
  private String m_repositoryDomainName;

  /** Dynamic Access Module */
  // FIXIT!! is this the right way to associate the dynamic access module???
  private DynamicAccessModule m_dynamicAccess;

  private ExternalContentManager m_externalContentManager;
  
  private String fedoraServerHost = null;

  private String fedoraServerPort = null;
  
  private Authorization m_authorizationModule;

  /**
   * <p>Creates and initializes the Access Module. When the server is starting
   * up, this is invoked as part of the initialization process.</p>
   *
   * @param moduleParameters A pre-loaded Map of name-value pairs comprising
   *        the intended configuration of this Module.
   * @param server The <code>Server</code> instance.
   * @param role The role this module fulfills, a java class name.
   * @throws ModuleInitializationException If initilization values are
   *         invalid or initialization fails for some other reason.
   */
  public DefaultAccess(Map moduleParameters, Server server, String role)
          throws ModuleInitializationException
  {
    super(moduleParameters, server, role);
  }

  /**
   * <p>Initializes the module.</p>
   *
   * @throws ModuleInitializationException If the module cannot be initialized.
   */
  public void initModule() throws ModuleInitializationException
  {

    String dsMediation = getParameter("doMediateDatastreams");
    if (dsMediation==null)
    {
        throw new ModuleInitializationException(
            "doMediateDatastreams parameter must be specified.", getRole());
    }
  }

  public void postInitModule()
      throws ModuleInitializationException
  {
    // get ref to DOManager
    m_manager=(DOManager) getServer().getModule(
        "fedora.server.storage.DOManager");
    if (m_manager == null)
    {
      throw new ModuleInitializationException("Can't get a DOManager "
          + "from Server.getModule", getRole());
    }
      // get ref to DynamicAccess module
      m_dynamicAccess = (DynamicAccessModule) getServer().
              getModule("fedora.server.access.DynamicAccess");
      
      // get ref to ExternalContentManager
      m_externalContentManager = (ExternalContentManager) getServer().
      getModule("fedora.server.storage.ExternalContentManager");      
      
    // get ref to OAIProvider, for repositoryDomainName param for oai info
    Module oaiProvider=(Module) getServer().getModule("fedora.oai.OAIProvider");
    if (oaiProvider==null) {
      throw new ModuleInitializationException("DefaultAccess module requires that the server "
          + "has an OAIProvider module configured so that it can get the repositoryDomainName parameter.", getRole());
    }
    m_repositoryDomainName=oaiProvider.getParameter("repositoryDomainName");
    if (m_repositoryDomainName==null) {
      throw new ModuleInitializationException("DefaultAccess module requires that the OAIProvider "
          + "module has the repositoryDomainName parameter specified.", getRole());
    }
    
    m_authorizationModule = (Authorization) getServer().getModule("fedora.server.security.Authorization");
    if (m_authorizationModule == null) {
        throw new ModuleInitializationException("Can't get an Authorization module (in default access) from Server.getModule", getRole());
    }

  }

  private static final Hashtable accessActionAttributes = new Hashtable();
  static {
    accessActionAttributes.put("api","apia");
  }  

  /**
   * <p>Disseminates the content produced by executing the specified method
   * of the associated Behavior Mechanism object of the specified digital
   * object.</p>
   *
   * @param context The context of this request.
   * @param PID The persistent identifier of the digital object.
   * @param bDefPID The persistent identifier of the Behavior Definition object.
   * @param methodName The name of the method to be executed.
   * @param userParms An array of user-supplied method parameters consisting
   *        of name/value pairs.
   * @param asOfDateTime The versioning datetime stamp.
   * @return A MIME-typed stream containing the result of the dissemination.
   * @throws ServerException If any type of error occurred fulfilling the
   *         request.
   */
  public MIMETypedStream getDissemination(Context context, String PID,
      String bDefPID, String methodName, Property[] userParms,
      Date asOfDateTime) throws ServerException
  {
    PID = Server.getPID(PID).toString();
    bDefPID = Server.getPID(bDefPID).toString();
    long initStartTime = new Date().getTime();
    long startTime = new Date().getTime();
    long stopTime;
    long interval;
    BMechReader bmechreader = null;
    
    DOReader reader = m_manager.getReader(asOfDateTime == null, context, PID);
    String authzAux_objState = reader.GetObjectState();
    
    // DYNAMIC!! If the behavior definition (bDefPID) is defined as dynamic, then
    // perform the dissemination via the DynamicAccess module.
    if (m_dynamicAccess.isDynamicBehaviorDefinition(context, PID, bDefPID))
    {
        m_authorizationModule.enforceGetDissemination(context, PID, bDefPID,
                methodName, asOfDateTime, authzAux_objState, "A",
                "fedora-system:4", "A", "A");
        MIMETypedStream retVal = 
            m_dynamicAccess.getDissemination(context, PID, bDefPID, methodName,
                                            userParms, asOfDateTime);
        stopTime = new Date().getTime();
        interval = stopTime - startTime;
        LOG.debug("Roundtrip DynamicDisseminator: " + interval + " milliseconds.");
        return(retVal);
    }
    boolean doCMDA = false;
    
    DOReader cmReader = null;
    RelationshipTuple cmPIDs[] = reader.getRelationships(null, Constants.RELS_EXT.HAS_FORMAL_CONTENT_MODEL.uri);
    boolean done = false;
    m_manager.initializeCModelBmechHashMap(context);
    if (cmPIDs != null && cmPIDs.length > 0)
    {
        for (int i = 0; i < cmPIDs.length && !done; i++)
        {
            String cModelPid = cmPIDs[i].getObjectPID();
            if (cModelPid.equals("self"))
            {
                cmReader = reader;
                cModelPid = PID;
            }
            else
            {
                cmReader = m_manager.getReader(asOfDateTime == null, context, cModelPid);
            }
            RelationshipTuple bDefPIDs[] = cmReader.getRelationships(null, Constants.RELS_EXT.HAS_BDEF.uri);
            for (int j = 0; j < bDefPIDs.length && !done; j++)
            {
                if (bDefPIDs[j].getObjectPID().endsWith(bDefPID))
                {
                    bmechreader = FindBMechForBDefAndCModel(context, bDefPID, cModelPid);
                    if (bmechreader != null)
                    {
                        done = true;
                        doCMDA = true;
                    }
                }
            }
        }
    }
 
    BDefReader bDefReader = m_manager.getBDefReader(asOfDateTime == null, context, bDefPID);
    String authzAux_bdefState = bDefReader.GetObjectState();

    String authzAux_dissState = "unknown";
    
    // SDP: get a bmech reader to get information that is specific to
    // a mechanism.
    Date versDateTime = asOfDateTime;
//    BMechReader bmechreader = null;
    if (!doCMDA)
    {
        String message = "[DefaultAccess] Disseminators are no longer supported ";
        throw new DisseminatorNotFoundException(message);
//        Disseminator[] dissSet = reader.GetDisseminators(versDateTime, null);
//        startTime = new Date().getTime();
//        for (int i=0; i<dissSet.length; i++)
//        {
//          if (dissSet[i].bDefID.equalsIgnoreCase(bDefPID))
//          {
//            authzAux_dissState = dissSet[i].dissState;
//            bmechreader = m_manager.getBMechReader(asOfDateTime == null, context, dissSet[i].bMechID);
//            break;
//          }
//        }
    }

    // if bmechreader is null, it means that no disseminators matched the specified bDef PID
    // This can occur if a date/time stamp value is specified that is earlier than the creation
    // date of all disseminators or if the specified bDef PID does not match the bDef of
    // any disseminators in the object.
    if(bmechreader == null) 
    {
        String message = "[DefaultAccess] Either there are no disseminators found in "
            + "the object \"" + PID + "\" that match the specified date/time stamp "
            + "of \"" + DateUtility.convertDateToString(asOfDateTime) + "\"  OR "
            + "the specified bDef PID of \"" + bDefPID + "\" does not match the "
            + "bDef PID of any disseminators for this digital object.";
        throw new DisseminatorNotFoundException(message);
    }
    stopTime = new Date().getTime();
    interval = stopTime - startTime;
    LOG.debug("Roundtrip Looping Diss: " + interval + " milliseconds.");

    // Check bmech object state
    String authzAux_bmechState = bmechreader.GetObjectState();
    String authzAux_bmechPID = bmechreader.GetObjectPID();

    m_authorizationModule.enforceGetDissemination(context, PID, bDefPID, methodName, asOfDateTime,
            authzAux_objState, authzAux_bdefState, authzAux_bmechPID, authzAux_bmechState, authzAux_dissState);
    
    // Get method parms
    Hashtable h_userParms = new Hashtable();
    MIMETypedStream dissemination = null;
    MethodParmDef[] defaultMethodParms = null;

    startTime = new Date().getTime();
    // Put any user-supplied method parameters into hash table
    if (userParms != null)
    {
        for (int i = 0; i < userParms.length; i++)
        {
            h_userParms.put(userParms[i].name, userParms[i].value);
        }
    }

    if (!doCMDA)
    {
        // Validate user-supplied parameters
        validateUserParms(context, PID, bDefPID, null, methodName, h_userParms, versDateTime);
    }
    else
    {
        // Validate user-supplied parameters
        validateUserParms(context, PID, bDefPID, bmechreader, methodName, h_userParms, versDateTime);
    }

    stopTime = new Date().getTime();
    interval = stopTime - startTime;
    LOG.debug("Roundtrip Get/Validate User Parms: " + interval + " milliseconds.");

    startTime = new Date().getTime();
    // SDP: GET INFO FROM BMECH READER:
    // Add any default method parameters to validated user parm list
    //defaultMethodParms = reader.GetBMechDefaultMethodParms(bDefPID,
    defaultMethodParms = bmechreader.getServiceMethodParms(methodName, versDateTime);
    for (int i=0; i<defaultMethodParms.length; i++)
    {
      if (!defaultMethodParms[i].parmType.equals(MethodParmDef.DATASTREAM_INPUT)) 
      {
          if (!h_userParms.containsKey(defaultMethodParms[i].parmName)) 
          {
            LOG.debug("addedDefaultName: "+defaultMethodParms[i].parmName);
            String pdv=defaultMethodParms[i].parmDefaultValue;
            try {
                // here we make sure the PID is decoded so that encoding
                // later won't doubly-encode it
                if (pdv.equalsIgnoreCase("$pid")) 
                {
                    pdv=URLDecoder.decode(PID, "UTF-8");
                } 
                else if (pdv.equalsIgnoreCase("$objuri")) 
                {
                    pdv="info:fedora/" + URLDecoder.decode(PID, "UTF-8");
                }
            } 
            catch (UnsupportedEncodingException uee) { }
            LOG.debug("addedDefaultValue: "+pdv);
            h_userParms.put(defaultMethodParms[i].parmName, pdv);
          }
      }
    }

    stopTime = new Date().getTime();
    interval = stopTime - startTime;
    LOG.debug("Roundtrip Get BMech Parms: " + interval + " milliseconds.");

    startTime = new Date().getTime();
    DisseminationBindingInfo[] dissBindInfo;
    if (doCMDA)
    {
        // Get dissemination binding info.
        dissBindInfo = GetCMDADisseminationBindingInfo(
            reader, cmReader, bmechreader, methodName, versDateTime);
        
    }
    else
    {
        String message = "[DefaultAccess] Disseminators are no longer supported ";
        throw new DisseminatorNotFoundException(message);
//        // Get dissemination binding info.
//        dissBindInfo =
//            reader.getDisseminationBindingInfo(bDefPID, methodName, versDateTime);
    }

    // Assemble and execute the dissemination request from the binding info.
    String reposBaseURL = getReposBaseURL(
            context.getEnvironmentValue(Constants.HTTP_REQUEST.SECURITY.uri).equals(Constants.HTTP_REQUEST.SECURE.uri) 
                ? "https" : "http",
            context.getEnvironmentValue(Constants.HTTP_REQUEST.SERVER_PORT.uri)
        );
    DisseminationService dissService = new DisseminationService();
    dissemination =
        dissService.assembleDissemination(context, PID, h_userParms, dissBindInfo, authzAux_bmechPID, methodName);

    stopTime = new Date().getTime();
    interval = stopTime - startTime;
    LOG.debug("Roundtrip Assemble Dissemination: " + interval + " milliseconds.");

    stopTime = new Date().getTime();
    interval = stopTime - initStartTime;
    LOG.debug("Roundtrip GetDissemination: " + interval + " milliseconds.");
    return dissemination;
  }
  
/*  No longer needed if the datastream name MUST match the parameter name in the BMech */
//  private DSBinding[] getBindingInfoFromContentModel(DOReader cmReader, Date versDateTime) throws ServerException
//  {
//      Datastream compositeModel = cmReader.GetDatastream("DS-COMPOSITE-MODEL", versDateTime);
//      return(getBindingInfoFromDatastream( compositeModel ));
//  }
//  
//  public static DSBinding[] getBindingInfoFromDatastream(Datastream ds) throws ServerException
//  {
//      ArrayList list = new ArrayList();
//      DSBinding result[] = null;
//      ParserUtilityHandler handler = new ParserUtilityHandler(list)
//      {          
//          ArrayList list = null;
//          public void startDocument()
//          {
//              list = (ArrayList)(parm1);
//          }
//          
//          public void startElement(String uri, String localName, String qName, Attributes attrs) 
//          {
//              if (localName.equals("dsTypeModel"))
//              {
//                  DSBinding ds = new DSBinding();
//                  for (int i = 0; i < attrs.getLength(); i++)
//                  {
//                      if (attrs.getLocalName(i).equals("SEMANTIC_ID"))
//                      {
//                          ds.bindKeyName = attrs.getValue(i).toString();
//                      }
//                      if (attrs.getLocalName(i).equals("ID"))
//                      {
//                          ds.datastreamID = attrs.getValue(i).toString();
//                      }
//                  }
//                  ds.bindLabel = "";
//                  ds.seqNo = "";
//                  list.add(ds);
//              }
//           }              
//      };
//      
//      if (ds != null)
//      {
//          SAXParser parser = null;
//          try 
//          {
//              SAXParserFactory spf = SAXParserFactory.newInstance();
//              spf.setNamespaceAware(true);
//              parser = spf.newSAXParser();
//          } 
//          catch (Exception e) 
//          {
//              throw new RepositoryConfigurationException("Error getting SAX "
//                      + "parser for Content Model info: " + e.getClass().getName()
//                      + ": " + e.getMessage());
//          }
//          try  
//          {
//              parser.parse(ds.getContentStream(), handler);
//          } 
//          catch (SAXException saxe) 
//          {
//              throw new ObjectIntegrityException("Parse error parsing Composite Model Metadata: " + saxe.getMessage());
//          } 
//          catch (IOException ioe) 
//          {
//              throw new StreamIOException("Stream error parsing Composite Model Metadata: " + ioe.getMessage());
//          }
//          finally
//          {
//              int size = list.size();
//              if (size > 0)
//              {
//                  result = new DSBinding[size];
//              }
//              for (int i = 0; i < size; i++)
//              {
//                  result[i] = (DSBinding)list.get(i);
//              }
//          }
//      }  
//      return(result);
//  }
    
private BMechReader FindBMechForBDefAndCModel(Context context, String bDefPID, String cModelPID) throws ServerException
{
    String bMechPID = m_manager.lookupBmechForCModel(cModelPID, bDefPID);
    BMechReader bmReader = m_manager.getBMechReader(false, context, bMechPID);
    return(bmReader);
}

private DisseminationBindingInfo[] GetCMDADisseminationBindingInfo(
          DOReader dObj, DOReader cmReader, BMechReader bmReader, 
          String methodName, Date versDateTime) throws MethodNotFoundException, ServerException
  {
      // Results will be returned in this array, one item per datastream
      DisseminationBindingInfo[] bindingInfo;

//      DSBinding[] dsBindings = getBindingInfoFromContentModel(cmReader, versDateTime);
//      int dsCount = dsBindings.length;
      String[] dsNames = dObj.ListDatastreamIDs(null);
      int dsCount = dsNames.length;
      bindingInfo = new DisseminationBindingInfo[dsCount];
      // The bmech reader provides information about the service and params.
      //wdn5e 2005.04.24 for 2.1 : using false here becuase of versDateTime
 //     BMechReader mech = m_repoReader.getBMechReader(false, m_context, diss.bMechID);
      MethodParmDef[] methodParms = bmReader.getServiceMethodParms(methodName, versDateTime);
      // Find the operation bindings for the method in question
      MethodDefOperationBind[] opBindings = bmReader.getServiceMethodBindings(versDateTime);
      String addressLocation = null;
      String operationLocation = null;
      String protocolType = null;
      boolean foundMethod = false;
      for (int i = 0; i < opBindings.length; i++) 
      {
          if (opBindings[i].methodName.equals(methodName)) 
          {
              foundMethod = true;
              addressLocation = opBindings[i].serviceBindingAddress;
              operationLocation = opBindings[i].operationLocation;
              protocolType = opBindings[i].protocolType;
          }
      }
      if (!foundMethod) 
      {
          throw new MethodNotFoundException("Method " + methodName
                  + " was not found in " + bmReader.GetObjectPID() + "'s operation "
                  + " binding.");
      }
      // For each datastream referenced by the disseminator's ds bindings,
      // add an element to the output array which includes key information
      // on the operation and the datastream.
      for (int i=0; i < dsCount; i++) 
      {
//          String dsID = dsBindings[i].datastreamID;
//          bindingInfo[i] = new DisseminationBindingInfo();
//          bindingInfo[i].DSBindKey = dsBindings[i].bindKeyName;
          String dsID = dsNames[i];
          bindingInfo[i] = new DisseminationBindingInfo();
          bindingInfo[i].DSBindKey = dsNames[i];
          // get key info about the datastream and put it here
          Datastream ds = dObj.GetDatastream(dsID, versDateTime);
          if (ds == null) 
          {
              String message = "The object \"" + dObj.GetObjectPID()+"\" "
                  + "contains no datastream for dsID \""+dsID+"\" "
                  + "that was created on or before the specified date/timestamp "
                  + " of \"" + DateUtility.convertDateToString(versDateTime)
                  + "\" .";
              throw new DatastreamNotFoundException(message);
          }
          bindingInfo[i].dsLocation = ds.DSLocation;
          bindingInfo[i].dsControlGroupType = ds.DSControlGrp;
          bindingInfo[i].dsID = dsID;
          bindingInfo[i].dsVersionID = ds.DSVersionID;
          bindingInfo[i].dsState = ds.DSState;
          // these will be the same for all elements of the array
          bindingInfo[i].methodParms = methodParms;
          bindingInfo[i].AddressLocation = addressLocation;
          bindingInfo[i].OperationLocation = operationLocation;
          bindingInfo[i].ProtocolType = protocolType;
      }
      return bindingInfo;
  }
  
  public ObjectMethodsDef[] listMethods(Context context, String PID,
      Date asOfDateTime) throws ServerException
  {
    long startTime = new Date().getTime();
    PID = Server.getPID(PID).toString();
    m_authorizationModule.enforceListMethods(context, PID, asOfDateTime);
    DOReader reader =
        m_manager.getReader(Server.USE_DEFINITIVE_STORE, context, PID);

    ObjectMethodsDef[] methodDefs = reader.listMethods(asOfDateTime);
    long stopTime = new Date().getTime();
    long interval = stopTime - startTime;
    LOG.debug("Roundtrip listMethods: " + interval + " milliseconds.");

    // DYNAMIC!! Grab any dynamic method definitions and merge them with
    // the statically bound method definitions
    ObjectMethodsDef[] dynamicMethodDefs =
        //m_dynamicAccess.getObjectMethods(context, PID, asOfDateTime);
        m_dynamicAccess.listMethods(context, PID, asOfDateTime);
    ArrayList methodList = new ArrayList();
    for (int i=0; i < methodDefs.length; i++)
    {
      methodList.add(methodDefs[i]);
    }
    for (int j=0; j < dynamicMethodDefs.length; j++)
    {
      methodList.add(dynamicMethodDefs[j]);
    }
    return (ObjectMethodsDef[])methodList.toArray(new ObjectMethodsDef[0]);
  }

  public DatastreamDef[] listDatastreams(Context context, String PID,
      Date asOfDateTime) throws ServerException
  {
    long startTime = new Date().getTime();
    PID = Server.getPID(PID).toString();
    m_authorizationModule.enforceListDatastreams(context, PID, asOfDateTime);
    DOReader reader =
        m_manager.getReader(Server.USE_DEFINITIVE_STORE, context, PID);

    Datastream[] datastreams = reader.GetDatastreams(asOfDateTime, null);
    DatastreamDef[] dsDefs = new DatastreamDef[datastreams.length];
    for (int i=0; i<datastreams.length; i++) {
        DatastreamDef dsDef = new DatastreamDef();
        dsDef.dsID = datastreams[i].DatastreamID;
        dsDef.dsLabel = datastreams[i].DSLabel;
        dsDef.dsMIME = datastreams[i].DSMIME;
        dsDefs[i] = dsDef;
    }
    
    long stopTime = new Date().getTime();
    long interval = stopTime - startTime;
    LOG.debug("Roundtrip listDatastreams: " + interval + " milliseconds.");
    return dsDefs;
  }

  public ObjectProfile getObjectProfile(Context context, String PID,
    Date asOfDateTime) throws ServerException
  {
    PID = Server.getPID(PID).toString();
    m_authorizationModule.enforceGetObjectProfile(context, PID, asOfDateTime);
    DOReader reader = m_manager.getReader(asOfDateTime == null, context, PID);

    Date versDateTime = asOfDateTime;
    ObjectProfile profile = new ObjectProfile();
    profile.PID = reader.GetObjectPID();
    profile.objectLabel = reader.GetObjectLabel();
    profile.objectOwnerId = reader.getOwnerId();
    profile.objectContentModel = reader.getContentModelId();
    profile.objectCreateDate = reader.getCreateDate();
    profile.objectLastModDate = reader.getLastModDate();
    profile.objectType = reader.getFedoraObjectTypes();

    String reposBaseURL = getReposBaseURL(
        context.getEnvironmentValue(Constants.HTTP_REQUEST.SECURITY.uri).equals(Constants.HTTP_REQUEST.SECURE.uri) 
            ? "https" : "http",
        context.getEnvironmentValue(Constants.HTTP_REQUEST.SERVER_PORT.uri)
    );
    profile.dissIndexViewURL = getDissIndexViewURL(reposBaseURL, reader.GetObjectPID(), versDateTime);
    profile.itemIndexViewURL = getItemIndexViewURL(reposBaseURL, reader.GetObjectPID(), versDateTime);
      return profile;
  }

  /**
   * <p>Lists the specified fields of each object matching the given
   * criteria.</p>
   *
   * @param context the context of this request
   * @param resultFields the names of the fields to return
   * @param maxResults the maximum number of results to return at a time
   * @param query the query
   * @return the results of te field search
   * @throws ServerException If any type of error occurred fulfilling the
   *         request.
   */
  public FieldSearchResult findObjects(Context context,
          String[] resultFields, int maxResults, FieldSearchQuery query)
          throws ServerException {
      m_authorizationModule.enforceFindObjects(context);
      return m_manager.findObjects(context, resultFields, maxResults, query);
  }

  /**
   * <p>Resumes an in-progress listing of object fields.</p>
   *
   * @param context the context of this request
   * @param sessionToken the token of the session in which the remaining
   *        results can be obtained
   * @return the next set of results from the initial field search
   * @throws ServerException If any type of error occurred fulfilling the
   *         request.
   */
  public FieldSearchResult resumeFindObjects(Context context,
          String sessionToken) throws ServerException {
      m_authorizationModule.enforceFindObjects(context);
      return m_manager.resumeFindObjects(context, sessionToken);
  }

  /**
   * <p>Gets information that describes the repository.</p>
   *
   * @param context the context of this request
   * @return information that describes the repository.
   * @throws ServerException If any type of error occurred fulfilling the
   *         request.
   */
  public RepositoryInfo describeRepository(Context context) throws ServerException
  {
    m_authorizationModule.enforceDescribeRepository(context);
    RepositoryInfo repositoryInfo = new RepositoryInfo();
    repositoryInfo.repositoryName = getServer().getParameter("repositoryName");
    String reposBaseURL = getReposBaseURL(
            context.getEnvironmentValue(Constants.HTTP_REQUEST.SECURITY.uri).equals(Constants.HTTP_REQUEST.SECURE.uri) 
                ? "https" : "http",
            context.getEnvironmentValue(Constants.HTTP_REQUEST.SERVER_PORT.uri)
        );    
    repositoryInfo.repositoryBaseURL = reposBaseURL + "/fedora";
    repositoryInfo.repositoryVersion =
      Server.VERSION_MAJOR + "." + Server.VERSION_MINOR;
    Module domgr = getServer().getModule("fedora.server.storage.DOManager");
    repositoryInfo.repositoryPIDNamespace = domgr.getParameter("pidNamespace");
    repositoryInfo.defaultExportFormat = domgr.getParameter("defaultExportFormat");
    repositoryInfo.OAINamespace = m_repositoryDomainName;
    repositoryInfo.adminEmailList = getAdminEmails();
    repositoryInfo.samplePID = repositoryInfo.repositoryPIDNamespace + ":100";
    repositoryInfo.sampleOAIIdentifer = "oai:" + repositoryInfo.OAINamespace
      + ":" + repositoryInfo.samplePID;
    repositoryInfo.sampleSearchURL = repositoryInfo.repositoryBaseURL
      + "/search";
    repositoryInfo.sampleAccessURL = repositoryInfo.repositoryBaseURL
      + "/get/" + "demo:5";
    repositoryInfo.sampleOAIURL = repositoryInfo.repositoryBaseURL
      + "/oai?verb=Identify";
    repositoryInfo.retainPIDs = getRetainPIDs();
    return repositoryInfo;
  }

  /**
   * <p>Gets the change history of an object by returning a list of timestamps
   * that correspond to modification dates of components. This currently includes
   * changes to datastreams and disseminators.</p>
   *
   * @param context The context of this request.
   * @param PID The persistent identifier of the digitla object.
   * @return An Array containing the list of timestamps indicating when changes
   *         were made to the object.
   * @throws ServerException If any type of error occurred fulfilling the
   *         request.
   */
  public String[] getObjectHistory(Context context, String PID) throws ServerException
  {
    PID = Server.getPID(PID).toString();
    m_authorizationModule.enforceGetObjectHistory(context, PID);
    DOReader reader = m_manager.getReader(Server.USE_DEFINITIVE_STORE, context, PID);

    return reader.getObjectHistory(PID);
  }

  private String[] getAdminEmails()
  {
    String emailsCSV = convertToCSV(getServer().getParameter("adminEmailList"));
    Vector emails = new Vector();
    StringTokenizer st = new StringTokenizer(emailsCSV, ",");
    while (st.hasMoreElements())
    {
      emails.add(st.nextElement());
    }
    return (String[])emails.toArray(new String[0]);
  }

  private String[] getRetainPIDs()
  {
    String retainPIDsCSV = convertToCSV(getServer().getModule("fedora.server.storage.DOManager").getParameter("retainPIDs"));
    Vector retainPIDs = new Vector();
    StringTokenizer st = new StringTokenizer(retainPIDsCSV, ",");
    while (st.hasMoreElements())
    {
      retainPIDs.add(st.nextElement());
    }
    return (String[])retainPIDs.toArray(new String[0]);
  }
  private String convertToCSV(String list)
  {
    // make sure values in the list are comma delimited
    String original = list.trim();
    Pattern spaces = Pattern.compile(" ++");
    Matcher m = spaces.matcher(original);
    String interim = m.replaceAll(",");
    Pattern multcommas = Pattern.compile(",++");
    Matcher m2 = multcommas.matcher(interim);
    String csv = m2.replaceAll(",");
    return csv;
  }

  /**
   * <p>Validates user-supplied method parameters against values
   * in the corresponding Behavior Definition object. The method will validate
   * for:</p>
   * <ol>
   * <li> Valid name - each name must match a valid method parameter name</li>
   * <li> DefaultValue - any specified parameters with valid default values
   * will have the default value substituted if the user-supplied value is null
   * </li>
   * <li> Required name - each required method parameter name must be present
   * </ol>
   *
   * @param context The context of this request.
   * @param PID The persistent identifier of the digital object.
   * @param bDefPID The persistent identifier of the Behavior Definition object.
   * @param methodName The name of the method.
   * @param h_userParms A hashtable of user-supplied method parameter
   *        name/value pairs.
   * @param versDateTime The version datetime stamp of the digital object.
   * @throws ServerException If any type of error occurred fulfilling the
   *         request.
   *
   */
  private void validateUserParms(Context context, String PID, String bDefPID, BMechReader bmechreader,
      String methodName, Hashtable h_userParms, Date versDateTime)
      throws ServerException
  {
    PID = Server.getPID(PID).toString();
    bDefPID = Server.getPID(bDefPID).toString();
    MethodParmDef[] methodParms = null;
    MethodParmDef methodParm = null;
    StringBuffer sb = new StringBuffer();
    Hashtable h_validParms = new Hashtable();
    boolean isValid = true;

    DOReader reader = null;
        
    if (bmechreader != null)  // this code will be used for the CMDA example
    {
        MethodDef[] methods = bmechreader.getServiceMethods(versDateTime);
        // Filter out parms that are internal to the mechanism and not part
        // of the abstract method definition.  We just want user parms.
        reader = m_manager.getReader(false, context, PID);
        for (int i=0; i<methods.length; i++)
        {
          if (methods[i].methodName.equalsIgnoreCase(methodName))
          {
              ArrayList filteredParms = new ArrayList();
              MethodParmDef[] parms = methods[i].methodParms;
              for (int j=0; j<parms.length; j++)
              {
                if (parms[j].parmType.equalsIgnoreCase(MethodParmDef.USER_INPUT))
                {
                  filteredParms.add(parms[j]);
                }
              }
              methodParms = (MethodParmDef[])filteredParms.toArray(new MethodParmDef[0]);
          }
        }
    }
    else
    {
        String message = "[DefaultAccess] Disseminators are no longer supported ";
        throw new DisseminatorNotFoundException(message);
//        reader = m_manager.getReader(Server.GLOBAL_CHOICE, context, PID);
//        methodParms = reader.getObjectMethodParms(bDefPID, methodName, versDateTime);
    }

    // Put valid method parameters and their attributes into hashtable
    if (methodParms != null)
    {
      for (int i=0; i<methodParms.length; i++)
      {
        methodParm = methodParms[i];
        h_validParms.put(methodParm.parmName,methodParm);
        LOG.debug("methodParms[" + i + "]: "
            + methodParms[i].parmName
            + "\nlabel: " + methodParms[i].parmLabel
            + "\ndefault: " + methodParms[i].parmDefaultValue
            + "\nrequired: " + methodParms[i].parmRequired
            + "\ntype: " + methodParms[i].parmType);
        for (int j=0; j<methodParms[i].parmDomainValues.length; j++)
        {
          LOG.debug("domainValue: " + methodParms[i].parmDomainValues[j]);
        }
      }
    }

    if (!h_validParms.isEmpty())
    {
      // Iterate over valid parmameters to check for any missing required parms.
      Enumeration e = h_validParms.keys();
      while (e.hasMoreElements())
      {
        String validName = (String)e.nextElement();
        MethodParmDef mp = (MethodParmDef)h_validParms.get(validName);
        if(mp.parmRequired && h_userParms.get(validName) == null)
        {
          // This is a fatal error. A required method parameter does not
          // appear in the list of user supplied parameters.
          sb.append("The required parameter \""
              + validName + "\" was not found in the "
              + "user-supplied parameter list.");
          throw new InvalidUserParmException("[Invalid User Parameters] "
              + sb.toString());
        }
      }

      // Iterate over each user supplied parameter name
      Enumeration parmNames = h_userParms.keys();
      while (parmNames.hasMoreElements())
      {
        String parmName = (String)parmNames.nextElement();
        methodParm = (MethodParmDef)h_validParms.get(parmName);
        if (methodParm != null && methodParm.parmName != null)
        {
          // Method has one or more parameters defined
          // Check for default value if user-supplied value is null or empty
          String value = (String)h_userParms.get(methodParm.parmName);
          if (value == null || value.equalsIgnoreCase(""))
          {
            // Value of user-supplied parameter is  null or empty
            if(methodParm.parmDefaultValue != null)
            {
              // Default value is specified for this parameter.
              // Substitute default value.
              h_userParms.put(methodParm.parmName, methodParm.parmDefaultValue);
            } else
            {
              // This is a non-fatal error. There is no default specified
              // for this parameter and the user has supplied no value for
              // the parameter. The value of the empty string will be used
              // as the value of the parameter.
              LOG.warn("The method parameter \""
                  + methodParm.parmName
                  + "\" has no default value and no "
                  + "value was specified by the user.  "
                  + "The value of the empty string has "
                  + "been assigned to this parameter.");
            }
          } else
          {
            // Value of user-supplied parameter contains a value.
            // Validate the supplied value against the parmDomainValues list.
            String[] parmDomainValues = methodParm.parmDomainValues;
            if (parmDomainValues.length > 0)
            {
              if (!parmDomainValues[0].equalsIgnoreCase("null"))
              {
                boolean isValidValue = false;
                String userValue = (String)h_userParms.get(methodParm.parmName);
                for (int i=0; i<parmDomainValues.length; i++)
                {
                  if (userValue.equalsIgnoreCase(parmDomainValues[i]) ||
                      parmDomainValues[i].equalsIgnoreCase("null"))
                  {
                    isValidValue = true;
                  }
                }
                if (!isValidValue)
                {
                  for (int i=0; i<parmDomainValues.length; i++)
                  {
                    if (i == parmDomainValues.length-1)
                    {
                      sb.append(parmDomainValues[i]);
                    } else
                    {
                      sb.append(parmDomainValues[i]+", ");
                    }
                  }
                  sb.append("The method parameter \""
                            + methodParm.parmName
                            + "\" with a value of \""
                            + (String)h_userParms.get(methodParm.parmName)
                            + "\" is not allowed for the method \""
                            + methodName + "\". Allowed values for this "
                            + "method include \"" + sb.toString() + "\".");
                  isValid = false;
                }
              }
            }
          }
        } else
        {
          // This is a fatal error. A user-supplied parameter name does
          // not match any valid parameter names for this method.
          sb.append("The method parameter \"" + parmName
                    + "\" is not valid for the method \""
                    + methodName + "\".");
          isValid = false;
        }
      }
    } else
    {
      // There are no method parameters define for this method.
      if (!h_userParms.isEmpty())
      {
        // This is an error. There are no method parameters defined for
        // this method and user parameters are specified in the
        // dissemination request.
        Enumeration e = h_userParms.keys();
        while (e.hasMoreElements())
        {
          sb.append("The method parameter \"" + (String)e.nextElement()
                    + "\" is not valid for the method \""
                    + methodName + "\"."
                    + "The method \"" + methodName
                    + "\" defines no method parameters.");
        }
        throw new InvalidUserParmException("[Invalid User Parameters] "
            + sb.toString());
      }
    }
    if (!isValid)
    {
      throw new InvalidUserParmException("[Invalid User Parameter] "
          + sb.toString());
    }
    return;
  }

  private String getDissIndexViewURL(String reposBaseURL, String PID, Date versDateTime)
  {
      String dissIndexURL = null;

      if (versDateTime == null)
      {
        dissIndexURL = reposBaseURL + "/fedora/get/" + PID +
                      "/fedora-system:3/viewMethodIndex";
      }
      else
      {
          dissIndexURL = reposBaseURL + "/fedora/get/"
            + PID + "/fedora-system:3/viewMethodIndex/"
            + DateUtility.convertDateToString(versDateTime);
      }
      return dissIndexURL;
  }

  // FIXIT!! Consider implications of hard-coding the default dissemination
  // aspects of the URL (e.g. fedora-system3 as the PID and viewItemIndex.
  private String getItemIndexViewURL(String reposBaseURL, String PID, Date versDateTime)
  {
      String itemIndexURL = null;

      if (versDateTime == null)
      {
        itemIndexURL = reposBaseURL + "/fedora/get/" + PID +
                       "/fedora-system:3/viewItemIndex";
      }
      else
      {
          itemIndexURL = reposBaseURL + "/fedora/get/"
            + PID + "/fedora-system:3/viewItemIndex/"
            + DateUtility.convertDateToString(versDateTime);
      }
      return itemIndexURL;
  }

  private String getReposBaseURL(String protocol, String port)
  {
    String reposBaseURL = null;
    InetAddress hostIP = null;
    try
    {
      hostIP = InetAddress.getLocalHost();
    } catch (UnknownHostException uhe)
    {
      LOG.error("Unable to resolve host of Fedora server", uhe);
    }
    
    String fedoraServerHost = getServer().getParameter("fedoraServerHost");
    if (fedoraServerHost==null || fedoraServerHost.equals("")) {
        fedoraServerHost=hostIP.getHostName();
    }
    reposBaseURL = protocol + "://" + fedoraServerHost + ":" + port;
    return reposBaseURL;
  }

  public MIMETypedStream getDatastreamDissemination(Context context, String PID,
          String dsID, Date asOfDateTime) throws ServerException {
      PID = Server.getPID(PID).toString();
      m_authorizationModule.enforceGetDatastreamDissemination(context, PID, dsID, asOfDateTime);
      MIMETypedStream mimeTypedStream = null;      
      long startTime = new Date().getTime();
      DOReader reader = m_manager.getReader(Server.USE_DEFINITIVE_STORE, context, PID);

      Datastream ds = (Datastream) reader.GetDatastream(dsID, asOfDateTime);
      if (ds == null) {
          String message = "[DefaulAccess] No datastream could be returned. "
              + "Either there is no datastream for the digital "
              + "object \"" + PID + "\" with datastream ID of \"" + dsID
              + " \"  OR  there are no datastreams that match the specified "
              + "date/time value of \"" + DateUtility.convertDateToString(asOfDateTime)
              + " \"  .";
          throw new DatastreamNotFoundException(message);
      }
      
      if (ds.DSControlGrp.equalsIgnoreCase("E")) {
          DatastreamReferencedContent drc = (DatastreamReferencedContent) reader.GetDatastream(dsID, asOfDateTime);
          mimeTypedStream = m_externalContentManager.getExternalContent(drc.DSLocation, context);
      } else if(ds.DSControlGrp.equalsIgnoreCase("M")) {
          DatastreamManagedContent dmc = (DatastreamManagedContent) reader.GetDatastream(dsID, asOfDateTime);
          mimeTypedStream = new MIMETypedStream(ds.DSMIME, dmc.getContentStream(), null);
      } else if(ds.DSControlGrp.equalsIgnoreCase("X")) {
          DatastreamXMLMetadata dxm =  (DatastreamXMLMetadata) reader.GetDatastream(dsID, asOfDateTime);
          mimeTypedStream = new MIMETypedStream(ds.DSMIME, dxm.getContentStream(), null);
      } else if(ds.DSControlGrp.equalsIgnoreCase("R")){
          DatastreamReferencedContent drc = (DatastreamReferencedContent) reader.GetDatastream(dsID, asOfDateTime);
          // The dsControlGroupType of Redirect("R") is a special control type
          // used primarily for streaming media. Datastreams of this type are
          // not mediated (proxied by Fedora) and their physical dsLocation is
          // simply redirected back to the client. Therefore, the contents
          // of the MIMETypedStream returned for dissemination requests will
          // contain the raw URL of the dsLocation and will be assigned a
          // special fedora-specific MIME type to identify the stream as
          // a MIMETypedStream whose contents contain a URL to which the client
          // should be redirected.
          try
          {
            InputStream inStream = new ByteArrayInputStream(drc.DSLocation.getBytes("UTF-8"));
            mimeTypedStream = new MIMETypedStream("application/fedora-redirect", inStream, null);
          } catch (UnsupportedEncodingException uee)
          {
            String message = "[DefaultAccess] An error has occurred. "
                + "The error was a \"" + uee.getClass().getName() + "\"  . The "
                + "Reason was \"" + uee.getMessage() + "\"  . String value: "
                + drc.DSLocation + "  . ";
            LOG.error(message);
            throw new GeneralException(message);
          }          
      }
      long stopTime = new Date().getTime();
      long interval = stopTime - startTime;
      LOG.debug("Roundtrip getDatastreamDissemination: " + interval + " milliseconds.");      
      return mimeTypedStream;
  }
}
