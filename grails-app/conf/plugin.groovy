elasticSearch {
    /**
     * Date formats used by the unmarshaller of the JSON responses
     */
    date.formats = ["yyyy-MM-dd'T'HH:mm:ss.S'Z'"]

    /**
     * Hosts for remote ElasticSearch instances.
     * Will only be used with the "transport" client mode.
     * If the client mode is set to "transport" and no hosts are defined, ["localhost", 9300] will be used by default.
     */
    client.hosts = [
            [host: 'localhost', port: 9300]
    ]

    /**
     * Determines if the plugin should reflect any database save/update/delete automatically
     * on the ES instance. Default to false.
     */
    autoIndex = 'async'

    /**
     * Should the database be indexed at startup.
     *
     * Indexing is always asynchronous (compared to Searchable plugin) and executed after BootStrap.groovy.
     */
    bulkIndexOnStartup = 'migrated'

    /**
     * The strategy to be used in case of a conflict installing mappings
     */
    migration.strategy = 'alias'

    /**
     * Whether to replace existing indices with aliases when there's a conflict and the 'alias' strategy is chosen
     */
    migration.aliasReplacesIndex = true

    /**
     * When set to false, in case of an alias migration, prevents the alias to point to the newly created index
     */
    migration.disableAliasChange = false

    index.numberOfReplicas = 0

	/**
	* Disable dynamic method injection in domain class
	*/
	disableDynamicMethodsInjection = false

	/** 
	* Search method name in domain class, defaults to search
	*/
	searchMethodName = "search"

	/** 
	* countHits method name in domain class, defaults to search
	*/
	countHitsMethodName = "countHits"
}

environments {
    development {
        /**
         * Possible values : "local", "node", "transport"
         * If set to null, "node" mode is used by default.
         */
        elasticSearch.client.mode = 'local'
    }
    test {
        elasticSearch {
            client.mode = 'local'
            index.store.type = 'simplefs' // store local node in memory and not on disk
        }
    }
    production {
        elasticSearch.client.mode = 'node'
    }
}
