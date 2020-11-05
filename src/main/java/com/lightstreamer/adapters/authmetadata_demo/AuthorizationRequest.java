package com.lightstreamer.adapters.authmetadata_demo;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


public class AuthorizationRequest {
    
    //here is the list of the user/token
    //these info are shared with the demo client
    private static final ConcurrentHashMap<String,String> TOKENS = new ConcurrentHashMap<String,String>();
    static {
        TOKENS.put("user1","ikgdfigdfhihdsih");
        TOKENS.put("patient0","lookihaveanewtokenhere");
        TOKENS.put("leto","powerfultoken");
        TOKENS.put("gollum","toobadforyou");
        TOKENS.put("lucky","srsly");
    }
    
    //here is the list of the user/authorizations
    //these info are shared with the demo client (the client simply shows these info in the interface, does not directly use it)
    private static final ConcurrentHashMap<String,Set<String>> AUTHORIZATIONS = new ConcurrentHashMap<String,Set<String>>();
    static {
        //user1
        Set<String> userSet = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
        userSet.add("item1");
        userSet.add("item2");
        userSet.add("item3");
        AUTHORIZATIONS.put("user1", userSet);
        
        //patient0
        //will never reach subscriptions, so it's pointless give him authorizations
        
        //leto
        userSet = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
        for (int i=1; i<=30; i++) {
            userSet.add("item"+i);
        }
        AUTHORIZATIONS.put("leto", userSet);
        
        //gollum
        //no authorization for this user
        
        //lucky
        userSet = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
        userSet.add("item13");
        userSet.add("item17");
        AUTHORIZATIONS.put("lucky", userSet);
        
    }
    
    public static boolean isValidToken(String user, String token) {
        //In a real case, here the application would lookup the user/token 
        //on an external service (or a local cache); 
        //In this demo we simply lookup the hard-coded map 
        String correctToken = TOKENS.get(user);
        return correctToken != null && correctToken.equals(token);
    }
    
    public static boolean canUserSeeItems(String user, String[] items) {
        //In a real case, here the application would lookup the user authorizations 
        //on an external service (or a local cache); 
        //In this demo we simply lookup the hard-coded map 
        
        Set<String> authItems = AUTHORIZATIONS.get(user);
        if (authItems == null) {
            return false;
        }
        
        for (int i=0; i<items.length; i++) {
            if (!authItems.contains(items[i])) {
                return false;
            }
        }
        
        return true;
        
    }
    
    public static Set<String> getUserAuthorizations(String user) {
        //In a real case, here the application would lookup the user authorizations 
        //on an external service (or a local cache); 
        //In this demo we simply lookup the hard-coded map 
        return AUTHORIZATIONS.get(user);
    }
    
}
