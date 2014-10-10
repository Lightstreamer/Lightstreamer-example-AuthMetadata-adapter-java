# Lightstreamer - Authentication and Authorization Demo - Java Adapter

The Lightstreamer Authentication and Authorization Demo is a simple example illustrating authentication and authorization mechanisms when an external 
Web/Application Server is involved in the process.

This project includes a simple MetadataProvider implementation that includes user validation and items authorization logics.


##Details

This *Authentication and Authorization Demo* illustrates the typical best practice used for Lightstreamer Web applications, when a Web/Application server is involved in the process. 
The actual authentication is usually handled by the legacy Web/Application server, irrespective of Lightstreamer. 
Some sort of token is sent back to the Client through cookies, response payload or any other technique. 
When the Web Client creates the Lightstreamer session, instead of sending again the full credentials (usually involving a password) to 
Lightstreamer Server, it sends just the username and the token.
The Metadata Adapter is passed this information and validates the token against the Web/Application Server that 
generated it (or a database or whatever back-end system).

Here an overview of the whole sequence:

![sequence diagram](sequence_diagram.png)

In this demo client the Web/Application server is not actually involved and calls to placeholder methods are performed to validate the token.

from `src/authmetadata_demo/adapters/AuthMetadataAdapter.java`:
```java
[...]

if (!AuthorizationRequest.isValidToken(user, token)) {
    throw new AccessException("Invalid user/token");
}
  
[...]
```

This demo also implements Authorization handling of item subscription requests.
Every time a subscription is issued, the adapter verifies if the user issuing the request is actually authorized to subscribe to the selected item(s).
Again, a real case might query an external service to know the various authroizations, while this demo example simply checks on an hard-coded list

from `src/authmetadata_demo/adapters/AuthMetadataAdapter.java`:
```java
[...]

if (!AuthorizationRequest.canUserSeeItems(user, items)) {
    throw new CreditsException(-1, "User not authorized", "You are not authorized to see this item"); 
}

[...]
```

Querying an external service at subscription time is a discouraged approach. If the authorizations are actually placed on an external service,
it is suggested to use the approach shown in the AuthMetadataAdapterWithAuthCache class.

More details and comments on how the auth/auth cycle is accomplished is available in the source code of the application.

##Deploy

To have something to show to the user (i.e.: items to be subscribed), this demo relies on the the QUOTE_ADAPTER, from the Stock-List Demo 
(see [Lightstreamer - Stock-List Demo - Java Adapter](https://github.com/Weswit/Lightstreamer-example-StockList-adapter-java) ). 

* Download Lightstreamer Server (Lightstreamer Server comes with a free non-expiring demo license for 20 connected users) from Lightstreamer Download page, and install it, as explained in the GETTING_STARTED.TXT file in the installation home directory.
Make sure that Lightstreamer Server is not running.
* Get the deploy.zip file, from the releases of this project, unzip it, go to the Deployment_LS folder and copy the AuthDemo folder into the adapters 
folder of your Lightstreamer Server installation.
* Launch Lightstreamer Server.

##Build 

TODO

### The Adapter Set Configuration

This Adapter Set is configured and will be referenced by the clients as `AUTHDEMO`. 

The `adapters.xml` file for the *Authentication and Authorization Demo*, should look like:

```xml      
<?xml version="1.0"?>

<adapters_conf id="AUTHDEMO">

    <metadata_provider>
    
        <adapter_class>authmetadata_demo.adapters.AuthMetadataAdapter</adapter_class>

        <!-- use a dedicated pool for notifyUser call, see source code of AuthMetadataAdapter -->
        <authentication_pool>
            <max_size>10</max_size>
            <max_free>0</max_free>
        </authentication_pool>
        
    </metadata_provider>

    <data_provider name="QUOTE_ADAPTER">
    
        <!-- this class is taken from the Stock-List Demo - Java Adapter project-->
        <adapter_class>stocklist_demo.adapters.StockQuotesDataAdapter</adapter_class>
        
    </data_provider>

</adapters_conf>
```



### Related Projects

* [Lightstreamer - Reusable Metadata Adapters - Java Adapter](https://github.com/Weswit/Lightstreamer-example-ReusableMetadata-adapter-java)