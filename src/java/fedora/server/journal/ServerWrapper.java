package fedora.server.journal;

import fedora.server.Server;
import fedora.server.errors.ServerException;
import fedora.server.management.ManagementDelegate;
import fedora.server.storage.DOManager;

/**
 * 
 * <p>
 * <b>Title:</b> ServerWrapper.java
 * </p>
 * <p>
 * <b>Description:</b> Wrap a Server in an object that implements an interface,
 * so it can be passed to the JournalWorker classes and their dependents. It's
 * also easy to mock, for unit tests.
 * </p>
 * 
 * @author jblake@cs.cornell.edu
 * @version $Id$
 */

public class ServerWrapper implements ServerInterface {
    private final Server server;

    public ServerWrapper(Server server) {
        this.server = server;
    }

    public void logSevere(String message) {
        server.logSevere(message);
    }

    public void logInfo(String message) {
        server.logInfo(message);
    }

    public boolean hasInitialized() {
        return server.hasInitialized();
    }

    public ManagementDelegate getManagementDelegate() {
        return (ManagementDelegate) server
                .getModule("fedora.server.management.ManagementDelegate");
    }

    public String getRepositoryHash() throws ServerException {
        DOManager doManager = (DOManager) server
                .getModule("fedora.server.storage.DOManager");
        return doManager.getRepositoryHash();
    }

}