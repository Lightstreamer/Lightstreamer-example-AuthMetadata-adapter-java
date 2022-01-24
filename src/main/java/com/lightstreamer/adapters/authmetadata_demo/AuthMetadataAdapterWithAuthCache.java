package com.lightstreamer.adapters.authmetadata_demo;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.lightstreamer.interfaces.metadata.CreditsException;
import com.lightstreamer.interfaces.metadata.ItemsException;
import com.lightstreamer.interfaces.metadata.NotificationException;
import com.lightstreamer.interfaces.metadata.TableInfo;


//please start by inspecting the AuthMetadataAdapter, then come back here.
public class AuthMetadataAdapterWithAuthCache extends AuthMetadataAdapter {

    //In this demo we simulate that both Authentication and Authorization are performed by an external service.
    //To avoid blocking to call an external service during subscription, when the user creates his first session we 
    //cache his authorizations in this map. 
    private final Map<String,UserAuthorizations> authCache = new ConcurrentHashMap<String,UserAuthorizations>();
    private final Map<String, String> sessionToUser = new ConcurrentHashMap<String, String>();
    
    @Override
    public void notifyNewSession(final String user, String sessionID, Map clientContext) {
        //When the user opens a new session, we have to verify if a cache containing his authorizations is
        //already available, and, if not, query the external service to create one.
        
        //we save the sessionId->user association to use it in the notifySessionClose method.
        sessionToUser.put(sessionID,user);
        
        //First we check for the cache object and if needed we create one. 
        UserAuthorizations userCache;
        synchronized(authCache) {
            userCache = authCache.get(user);
            if (userCache == null) {
                userCache = new UserAuthorizations();
                authCache.put(user,userCache);
            }
        }
        
        //The cache object also counts the sessions associated to the related user, so we inform it
        //to count a new session
        boolean isFirstSession = userCache.newSession();
        if (isFirstSession) {
            
            //If this is the first session we have to query the service to retrieve the list of authorizations.
            //We don't need it right away, thus it would be a pity to block the thread (as long as this method 
            //does not exit the server does not start the session on the client). So we will make the request 
            //to the service on a spearated thread
            //NOTE: avoid this lookup in response to a notifyUser call, as there is no guarantee that the notifyUser 
            //will be followed by a notifyNewSession (although it quite always is). This is the best place to gather 
            //user-related information needed for the adapter functioning.
            final UserAuthorizations retrievedUserCache = userCache;
            AuthorizationsThreads.execute(new Runnable() {
                public void run() {
                    //In a real case, here we would call the service with a blocking call. In this demo the 
                    //authorization list is hard-coded int the AuthorizationRequest class, the call will not 
                    //block and will always work; in a real case you will probably need a fallback mechanism to
                    //release the CountDownLatch in UserAuthorization if the authorization mechanism fails
                    //or a timeout expires.
                    retrievedUserCache.cacheAuthorizations(AuthorizationRequest.getUserAuthorizations(user));
                    //the cache is now ready
                }
            });
        }
    }
    
    //We use this dedicate thread pool to retrieve authorizations lists. You might want to limit its size
    //Note that this is not the same thread pool used for the notifyUser calls and configured in the adapters.xml file.
    //I hid it here to avoid introducing it before time.
    private ExecutorService AuthorizationsThreads = Executors.newCachedThreadPool();
    
    @Override 
    public void notifyNewTables(java.lang.String user, java.lang.String sessionID, TableInfo[] tables) throws NotificationException, CreditsException {
        //A user is subscribing to one or more items, we have to verify if he is authorized to
        //see what he's asking for. In this case we already have a local cache or such cache is 
        //being filled, so we just check it. 
        
        UserAuthorizations userCache = authCache.get(user);
        if (userCache == null) {
            throw new CreditsException(-2, "Can't find cache for this user", "You are not authorized to see this item"); 
        }
        
        Set<String> authorizations = userCache.getAuthorizations();
        if(authorizations == null) {
            throw new CreditsException(-3, "User has no authorizations/authorization caching failed", "You are not authorized to see this item"); 
        }
        
        for (int i=0; i<tables.length; i++) {
            String[] items = tables[i].getSubscribedItems();

            for (int j=0; j<items.length; j++) {
                if (!authorizations.contains(items[j])) {
                    throw new CreditsException(-3, "User not authorized", "You are not authorized to see this item"); 
                }
            }
        }
    }
    
    
    
    
    @Override
    public void notifySessionClose(String sessionID) {
        //Once all the sessions for a certain user are closed we have to clean the cache, thus we have to
        //keep count of how many session a user has
        
        //First we lookup the user that owns this session
        String user = sessionToUser.remove(sessionID);
        
        //Then we check his cache object to verify the number of active sessions.
        //If this is the last one we simply destroy the cache
        synchronized(authCache) {
            UserAuthorizations userCache = authCache.get(user);
            if (userCache == null) {
                //this can't happen
                return;
            }
            boolean isLastSession = userCache.endSession();
            if (isLastSession) {
                authCache.remove(user);
            }
        }
    }
    
    
    //This is the cache object
    private class UserAuthorizations {
        
        int sessionCount = 0;
        private Set<String> authorizations;
        //If a subscriptions checks for authorizations before the authorizations
        //are filled, it will be kept waiting by this CountDownLatch.
        private CountDownLatch cacheWait = new CountDownLatch(1); 
        
        //returns true if is the first session
        public synchronized boolean newSession() {
            sessionCount++;
            return sessionCount == 1;
        }
        
        //returns true if it was the last session
        public synchronized boolean endSession() {
            sessionCount--;
            return sessionCount == 0;
        }
        
        //Saves the authorization Set and releases the CountDownLatch:
        //any subscription, waiting for this authorizations list, can
        //now continue.
        //We expect this method to be called only once (it is),
        //safety controls are out of scope here.
        public void cacheAuthorizations(Set<String> authorizations) {
           this.authorizations = authorizations;
           cacheWait.countDown();
        }
        
        //Retrieves the authorizations list if already available,
        //otherwise awaits.
        public Set<String> getAuthorizations() {
           try {
               //We do not wait forever, we have to release the thread.
               //In a real case if we exit because of the timeout we should 
               //deny the subscription with a special code to inform the client
               //to try again later
               cacheWait.await(3,TimeUnit.SECONDS);
            } catch (InterruptedException e) {
            }
            return this.authorizations;
        }
        
    }
    
}
