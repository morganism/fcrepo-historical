/*
 * Copyright 2001-2002,2004 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 5.025
 */


package org.apache.catalina.realm;


import java.security.Principal;
import java.security.acl.Group;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;

import javax.security.auth.Subject;
import javax.security.auth.login.AccountExpiredException;
import javax.security.auth.login.CredentialExpiredException;
import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.apache.catalina.Container;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.util.StringManager;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * <p>Implmentation of <b>Realm</b> that authenticates users via the <em>Java
 * Authentication and Authorization Service</em> (JAAS).  JAAS support requires
 * either JDK 1.4 (which includes it as part of the standard platform) or
 * JDK 1.3 (with the plug-in <code>jaas.jar</code> file).</p>
 *
 * <p>The value configured for the <code>appName</code> property is passed to
 * the <code>javax.security.auth.login.LoginContext</code> constructor, to
 * specify the <em>application name</em> used to select the set of relevant
 * <code>LoginModules</code> required.</p>
 *
 * <p>The JAAS Specification describes the result of a successful login as a
 * <code>javax.security.auth.Subject</code> instance, which can contain zero
 * or more <code>java.security.Principal</code> objects in the return value
 * of the <code>Subject.getPrincipals()</code> method.  However, it provides
 * no guidance on how to distinguish Principals that describe the individual
 * user (and are thus appropriate to return as the value of
 * request.getUserPrincipal() in a web application) from the Principal(s)
 * that describe the authorized roles for this user.  To maintain as much
 * independence as possible from the underlying <code>LoginMethod</code>
 * implementation executed by JAAS, the following policy is implemented by
 * this Realm:</p>
 * <ul>
 * <li>The JAAS <code>LoginModule</code> is assumed to return a
 *     <code>Subject with at least one <code>Principal</code> instance
 *     representing the user himself or herself, and zero or more separate
 *     <code>Principals</code> representing the security roles authorized
 *     for this user.</li>
 * <li>On the <code>Principal</code> representing the user, the Principal
 *     name is an appropriate value to return via the Servlet API method
 *     <code>HttpServletRequest.getRemoteUser()</code>.</li>
 * <li>On the <code>Principals</code> representing the security roles, the
 *     name is the name of the authorized security role.</li>
 * <li>This Realm will be configured with two lists of fully qualified Java
 *     class names of classes that implement
 *     <code>java.security.Principal</code> - one that identifies class(es)
 *     representing a user, and one that identifies class(es) representing
 *     a security role.</li>
 * <li>As this Realm iterates over the <code>Principals</code> returned by
 *     <code>Subject.getPrincipals()</code>, it will identify the first
 *     <code>Principal</code> that matches the "user classes" list as the
 *     <code>Principal</code> for this user.</li>
 * <li>As this Realm iterates over the <code>Princpals</code> returned by
 *     <code>Subject.getPrincipals()</code>, it will accumulate the set of
 *     all <code>Principals</code> matching the "role classes" list as
 *     identifying the security roles for this user.</li>
 * <li>It is a configuration error for the JAAS login method to return a
 *     validated <code>Subject</code> without a <code>Principal</code> that
 *     matches the "user classes" list.</li>
 * </ul>
 *
 * @author Craig R. McClanahan
 * @version $Revision$ $Date$
 */

public class JAASRealm
    extends RealmBase
 {
    private static Log log = LogFactory.getLog(JAASRealm.class);

    // ----------------------------------------------------- Instance Variables


    /**
     * The application name passed to the JAAS <code>LoginContext</code>,
     * which uses it to select the set of relevant <code>LoginModules</code>.
     */
    protected String appName = null;


    /**
     * Descriptive information about this Realm implementation.
     */
    protected static final String info =
        "org.apache.catalina.realm.JAASRealm/1.0";


    /**
     * Descriptive information about this Realm implementation.
     */
    protected static final String name = "JAASRealm";


    /**
     * The list of role class names, split out for easy processing.
     */
    protected ArrayList roleClasses = new ArrayList();


    /**
     * The string manager for this package.
     */
    protected static final StringManager sm =
        StringManager.getManager(Constants.Package);


    /**
     * The set of user class names, split out for easy processing.
     */
    protected ArrayList userClasses = new ArrayList();


    // ------------------------------------------------------------- Properties

    
    /**
     * setter for the appName member variable
     * @deprecated JAAS should use the Engine ( domain ) name and webpp/host overrides
     */
    public void setAppName(String name) {
        appName = name;
    }
    
    /**
     * getter for the appName member variable
     */
    public String getAppName() {
        return appName;
    }

    public void setContainer(Container container) {
        super.setContainer(container);
        String name=container.getName();
        if( appName==null  ) {
            appName=name;
            log.info("Setting JAAS app name " + appName);
        }
    }

    /**
     * Comma-delimited list of <code>javax.security.Principal</code> classes
     * that represent security roles.
     */
    protected String roleClassNames = null;

    public String getRoleClassNames() {
        return (this.roleClassNames);
    }

    public void setRoleClassNames(String roleClassNames) {
        this.roleClassNames = roleClassNames;
        roleClasses.clear();
        String temp = this.roleClassNames;
        if (temp == null) {
            return;
        }
        while (true) {
            int comma = temp.indexOf(',');
            if (comma < 0) {
                break;
            }
            roleClasses.add(temp.substring(0, comma).trim());
            temp = temp.substring(comma + 1);
        }
        temp = temp.trim();
        if (temp.length() > 0) {
            roleClasses.add(temp);
        }
    }


    /**
     * Comma-delimited list of <code>javax.security.Principal</code> classes
     * that represent individual users.
     */
    protected String userClassNames = null;

    public String getUserClassNames() {
        return (this.userClassNames);
    }

    public void setUserClassNames(String userClassNames) {
        this.userClassNames = userClassNames;
        userClasses.clear();
        String temp = this.userClassNames;
        if (temp == null) {
            return;
        }
        while (true) {
            int comma = temp.indexOf(',');
            if (comma < 0) {
                break;
            }
            userClasses.add(temp.substring(0, comma).trim());
            temp = temp.substring(comma + 1);
        }
        temp = temp.trim();
        if (temp.length() > 0) {
            userClasses.add(temp);
        }
    }


    // --------------------------------------------------------- Public Methods


    /**
     * Return the Principal associated with the specified username and
     * credentials, if there is one; otherwise return <code>null</code>.
     *
     * If there are any errors with the JDBC connection, executing
     * the query or anything we return null (don't authenticate). This
     * event is also logged, and the connection will be closed so that
     * a subsequent request will automatically re-open it.
     *
     * @param username Username of the Principal to look up
     * @param credentials Password or other credentials to use in
     *  authenticating this username
     */
    public Principal authenticate(String username, String credentials) {

        // Establish a LoginContext to use for authentication
        try {
        LoginContext loginContext = null;
        if( appName==null ) appName="Tomcat";

        if( log.isDebugEnabled())
            log.debug("Authenticating " + appName + " " +  username);

        // What if the LoginModule is in the container class loader ?
        //
        ClassLoader ocl=Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
        try {
            loginContext = new LoginContext
                (appName, new JAASCallbackHandler(this, username,
                                                  credentials));
        } catch (Throwable e) {
            log.error(sm.getString("jaasRealm.unexpectedError"), e);
            return (null);
        } finally {
            Thread.currentThread().setContextClassLoader(ocl);
        }

        if( log.isDebugEnabled())
            log.debug("Login context created " + username);

        // Negotiate a login via this LoginContext
        Subject subject = null;
        try {
            loginContext.login();
            subject = loginContext.getSubject();
            if (subject == null) {
                if( log.isDebugEnabled())
                    log.debug(sm.getString("jaasRealm.failedLogin", username));
                return (null);
            }
        } catch (AccountExpiredException e) {
            if (log.isDebugEnabled())
                log.debug(sm.getString("jaasRealm.accountExpired", username));
            return (null);
        } catch (CredentialExpiredException e) {
            if (log.isDebugEnabled())
                log.debug(sm.getString("jaasRealm.credentialExpired", username));
            return (null);
        } catch (FailedLoginException e) {
            if (log.isDebugEnabled())
                log.debug(sm.getString("jaasRealm.failedLogin", username));
            return (null);
        } catch (LoginException e) {
            log.warn(sm.getString("jaasRealm.loginException", username), e);
            return (null);
        } catch (Throwable e) {
            log.error(sm.getString("jaasRealm.unexpectedError"), e);
            return (null);
        }

        if( log.isDebugEnabled())
            log.debug("Getting principal " + subject);

        // Return the appropriate Principal for this authenticated Subject
        Principal principal = createPrincipal(username, subject);
        if (principal == null) {
            log.debug(sm.getString("jaasRealm.authenticateFailure", username));
            return (null);
        }
        if (log.isDebugEnabled()) {
            log.debug(sm.getString("jaasRealm.authenticateSuccess", username));
        }

        return (principal);
        } catch( Throwable t) {
            log.error( "error ", t);
            return null;
        }
    }


    // -------------------------------------------------------- Package Methods


    // ------------------------------------------------------ Protected Methods


    /**
     * Return a short name for this Realm implementation.
     */
    protected String getName() {

        return (name);

    }


    /**
     * Return the password associated with the given principal's user name.
     */
    protected String getPassword(String username) {

        return (null);

    }


    /**
     * Return the Principal associated with the given user name.
     */
    protected Principal getPrincipal(String username) {

        return (null);

    }


    /**
     * Construct and return a <code>java.security.Principal</code> instance
     * representing the authenticated user for the specified Subject.  If no
     * such Principal can be constructed, return <code>null</code>.
     *
     * @param subject The Subject representing the logged in user
     */
    protected Principal createPrincipal(String username, Subject subject) {
        // Prepare to scan the Principals for this Subject
        String password = null; // -will- be carried forward
        ArrayList roles = new ArrayList();

        // Scan the Principals for this Subject
        Iterator principals = subject.getPrincipals().iterator();
        while (principals.hasNext()) {
            Principal principal = (Principal) principals.next();
            /* commenting out this existing code, as it prevents tomcat login modules 
             * from cooperating with additional login modules:
            // No need to look further - that's our own stuff
            if( principal instanceof GenericPrincipal ) {
                if( log.isDebugEnabled() )
                    log.debug("Found old GenericPrincipal " + principal );
                return principal;
            }
            */
            String principalClass = principal.getClass().getName();
            if( log.isDebugEnabled() )
                log.info("Principal: " + principalClass + " " + principal);
            log.debug("Principal: " + principalClass + " " + principal);

            //a generic principal now contributes without shortcircuiting method
            if (userClasses.contains(principalClass) || (principal instanceof GenericPrincipal)) {
            	log.debug("principal is either in userClasses or a tomcat generic principal");
                username = principal.getName();
                if (principal instanceof GenericPrincipal) {                	
                	password = ((GenericPrincipal) principal).getPassword();
                	log.debug("JAASRealm got password from generic principal, password=" + password);
                } else if (principal instanceof IdPasswordPrincipal) {
                	password = ((IdPasswordPrincipal) principal).getPassword();
                	log.debug("JAASRealm got password from generic idpasswordprincipal, password=" + password);
                } else {
                	log.debug("JAASRealm in neglected else");
                	log.debug("JAASRealm distributed by Fedora needs fixup");
                	/* if execution reaches here, you probably added a new principal 
                	 * if that principal conveys a password needed by a backend call,
                	 * add another else-if in the code above for that principal.
                	 * leave this final else intact and in place.    
                	*/            	
                }
            }
            
            //a generic principal now contributes roles, as opposed to supplies all of them
            if (principal instanceof GenericPrincipal) {
            	String[]  tempRoles = ((GenericPrincipal) principal).getRoles();
            	for (int i = 0; i < tempRoles.length; i++) {
                    roles.add(tempRoles[i]);            		
            	}
            } else if (roleClasses.contains(principalClass)) {
                roles.add(principal.getName());
            }
            
            // following code left intact:
            // Same as Jboss - that's a pretty clean solution
            if( (principal instanceof Group) &&
                 "Roles".equals( principal.getName())) {
                Group grp=(Group)principal;
                Enumeration en=grp.members();
                while( en.hasMoreElements() ) {
                    Principal roleP=(Principal)en.nextElement();
                    roles.add( roleP.getName());
                }
            }
            
        }

        GenericPrincipal tomcatSeesOnlyThisPrincipal = null;
        if (username != null) {
        	log.debug("JAASRealm creating generic principal, username=" 
                	+ username 
        			+ ", password=" 
        			+ password);
        	tomcatSeesOnlyThisPrincipal = new GenericPrincipal(this, username, password, roles);
        	log.debug("JAASRealm created generic principal, username=" 
        	+ tomcatSeesOnlyThisPrincipal.getName() 
			+ ", password=" 
			+ tomcatSeesOnlyThisPrincipal.getPassword());
        }
        return tomcatSeesOnlyThisPrincipal;
    }


    // ------------------------------------------------------ Lifecycle Methods


    /**
     *
     * Prepare for active use of the public methods of this Component.
     *
     * @exception LifecycleException if this component detects a fatal error
     *  that prevents it from being started
     */
    public void start() throws LifecycleException {

        // Perform normal superclass initialization
        super.start();

    }


    /**
     * Gracefully shut down active use of the public methods of this Component.
     *
     * @exception LifecycleException if this component detects a fatal error
     *  that needs to be reported
     */
    public void stop() throws LifecycleException {

        // Perform normal superclass finalization
        super.stop();

    }


}
