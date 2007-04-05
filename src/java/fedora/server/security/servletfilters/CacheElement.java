/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://www.fedora.info/license/).
 */

package fedora.server.security.servletfilters;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Calendar;


/** 
 *  @author Bill Niebel (niebel@virginia.edu)
 */
public class CacheElement {
	
    private Log log = LogFactory.getLog(CacheElement.class);
	
	private final String userid;
	
	private String credentialsCheck;

	private boolean valid = false;
	private Boolean authenticated = null;
	private Map namedValues = null;
	
	private String errorMessage = null;
	
	public final void audit() {
		log.debug("userid==" + userid);
		log.debug("authenticated==" + authenticated);
		log.debug("namedAttributes==");
		for (Iterator it1 = namedValues.keySet().iterator(); it1.hasNext(); ) {
			Object temp = it1.next();
			if (! (temp instanceof String)) {
				log.debug(">>>>> NAME IS NOT STRING <<<<<");				
			} else {
				String name = (String) temp;
				log.debug(name + "==");
				temp = namedValues.get(name);
				if (temp instanceof String) {
					log.debug(temp);				
				} else if (temp instanceof Set) {
					Set values = (Set) temp;
					log.debug("in audit set n==" + values.size());
					for (Iterator it2 = values.iterator(); it2.hasNext(); ) {
						temp = it2.next();
						if (! (temp instanceof String)) {
							log.debug(">>>>> VALUE IS NOT STRING <<<<<");				
						} else {
							String value = (String) temp;
							log.debug(value + ",");
						}
					}
				} else {
					log.debug(">>>>> VALUES IS NEITHER STRING NOR SET <<<<<");
				}
			}
		}
	}
	
	public final HashSet NULL_SET =  new HashSet(); //<<<<<<<<<<<<<<new ImmutableHashSet();
	public final Hashtable EMPTY_MAP = new Hashtable(); //<<<<<<<<<<<<<<
	
	public final void populate(Boolean authenticated, Set predicates, Map namedValues, String errorMessage) {
		this.errorMessage = errorMessage;
		log.debug("in ce.pop");
		if (errorMessage == null) {
			valid = true;
			this.authenticated = authenticated;
			if (namedValues == null) {
				log.debug("in ce.pop(), namedValues is null");
				this.namedValues = EMPTY_MAP;
			} else {
				log.debug("in ce.pop(), namedValues is not null " + namedValues);
				this.namedValues = namedValues;
			}
			audit();
		} else {
			log.debug(errorMessage);
		}
	}
	
	private Calendar expiration = null;	
	
	public CacheElement(String userid, /*String password,*/ Cache cache) {
		this.userid = userid;
		//this.cache = cache;
	}
	
	public String getUserid() {
		return userid;
	}

	private static final Calendar getExpiration(int duration, String unit) {
		Calendar expiration = Calendar.getInstance();
		if ("second".equalsIgnoreCase(unit) || "seconds".equalsIgnoreCase(unit)) {
			expiration.add(Calendar.SECOND, duration);
		} else if ("minute".equalsIgnoreCase(unit) || "minutes".equalsIgnoreCase(unit)) {
			expiration.add(Calendar.MINUTE, duration);			
		} else if ("hour".equalsIgnoreCase(unit) || "hours".equalsIgnoreCase(unit)) {
			expiration.add(Calendar.HOUR, duration);			
		} else {
			//properties read failure
			LogFactory.getLog(CacheElement.class).fatal(CacheElement.class.getName() + ".getExpiration() " + "bad expiration properties");			
			expiration.add(Calendar.YEAR, -666);			
		}
		return expiration;
	}

	/* synchronize so evaluation of cache item state will be sequential, non-interlaced
	(protect against overlapping calls resulting in redundant authenticator calls)
	*/
	public final synchronized Boolean authenticate(Cache cache, String password) {
		Boolean authenticated = null;
		CacheElementPopulator cacheElementPopulator = cache.getCacheElementPopulator();
		Calendar now = Calendar.getInstance();
		log.debug(this.getClass().getName() + ".authenticate");
		if ((expiration != null) && now.before(expiration)) {
			log.debug(this.getClass().getName() + " cache valid");
			//previous authentication is still set and valid
			if (password != null) {
				authenticated =	password.equals(credentialsCheck);
			}
		} else { //previous authentication has timed out (or no previous authentication)
			log.debug(this.getClass().getName() + " new authentication needed");
			valid = false;
			cacheElementPopulator.populateCacheElement(this, password);
			credentialsCheck = password;
			if (valid && (authenticated != null)) {
				if (((Boolean)authenticated).booleanValue()) {
					log.debug(this.getClass().getName() + " authentication succeeded");
					expiration = getExpiration(cache.getAuthSuccessTimeoutDuration(), cache.getAuthSuccessTimeoutUnit());				
				} else {
					log.debug(this.getClass().getName() + " authentication failed");
					expiration = getExpiration(cache.getAuthFailureTimeoutDuration(), cache.getAuthFailureTimeoutUnit());				
				}
			} else {
				log.debug(this.getClass().getName() + " couldn't authenticate");
				expiration = getExpiration(cache.getAuthExceptionTimeoutDuration(), cache.getAuthExceptionTimeoutUnit());				
			} 
			authenticated = this.authenticated;
		}
		return authenticated;
	}
		
	/* synchronize so evaluation of cache item state will be sequential, non-interlaced
	(protect against overlapping calls resulting in redundant authenticator calls)
	*/
	public final synchronized Map getNamedValues(Cache cache, String password) {
		CacheElementPopulator cacheElementPopulator = cache.getCacheElementPopulator();
		Calendar now = Calendar.getInstance();
		log.debug(this.getClass().getName() + ".getNamedValues()");
		log.debug(this.getClass().getName() + ".getNamedValues() expiration==" + expiration);
		log.debug(this.getClass().getName() + ".getNamedValues() now==" + now);
		if (expiration != null) {
			log.debug(this.getClass().getName() + ".getNamedValues() expiration==" + expiration.toString());
			log.debug(this.getClass().getName() + ".getNamedValues() cmp==" + now.before(expiration));
		}
		if (now != null) {
			log.debug(this.getClass().getName() + ".getNamedValues() now==" + now.toString());
		}
		if (valid && (expiration != null) && now.before(expiration)) {
			log.debug(this.getClass().getName() + " cache valid"); 
		} else { //previous X has timed out (or no previous X)
			log.debug(this.getClass().getName() + " new X needed");
			namedValues = null;
			try {
				valid = false;
				cacheElementPopulator.populateCacheElement(this, password);
				if (valid) {
					expiration = getExpiration(cache.getAuthSuccessTimeoutDuration(), cache.getAuthSuccessTimeoutUnit());				
				} else {
					expiration = getExpiration(cache.getAuthExceptionTimeoutDuration(), cache.getAuthExceptionTimeoutUnit());				
				}
				if (namedValues == null) {
					namedValues = EMPTY_MAP;
				}
			} catch (Throwable t) {
			}
		}
		return namedValues;
	}		

}
