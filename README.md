# Lightstreamer - Authentication and Authorization Demo - Java Adapter

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