package grails.plugins.elasticsearch.lite

import grails.core.GrailsApplication
import grails.plugins.elasticsearch.lite.mapping.ElasticSearchMarshaller
import grails.plugins.elasticsearch.util.ElasticSearchConfigAware
import org.elasticsearch.action.bulk.BulkRequestBuilder
import org.elasticsearch.action.bulk.BulkResponse
import org.elasticsearch.action.delete.DeleteRequestBuilder
import org.elasticsearch.action.delete.DeleteResponse
import org.elasticsearch.action.index.IndexRequestBuilder
import org.elasticsearch.action.index.IndexResponse
import org.elasticsearch.action.search.SearchRequestBuilder
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.client.Client
import org.elasticsearch.index.query.MoreLikeThisQueryBuilder
import org.elasticsearch.index.query.QueryBuilder
import org.grails.datastore.mapping.engine.event.AbstractPersistenceEvent
import org.grails.datastore.mapping.engine.event.PostInsertEvent
import org.grails.datastore.mapping.engine.event.PostUpdateEvent
import reactor.spring.context.annotation.Consumer
import reactor.spring.context.annotation.Selector

/**
 * Created by marcoscarceles on 08/02/2017.
 */
@Consumer
class ElasticSearchService implements ElasticSearchConfigAware {

    GrailsApplication grailsApplication
    ElasticSearchLiteContext elasticSearchLiteContext

    Client getClient() {
        elasticSearchLiteContext.client
    }

    @Selector('gorm:postInsert')
    void onInsert(PostInsertEvent event) {
        onUpsert(event);
    }

    @Selector('gorm:postUpdate')
    void onUpdate(PostUpdateEvent event) {
        onUpsert(event);
    }

    void onUpsert(AbstractPersistenceEvent event) {
        if(elasticSearchLiteContext.autoIndexingEnabled) {
            index(event.entityObject)
        }
    }

    @Selector('gorm:postDelete')
    void onDelete(AbstractPersistenceEvent event) {
        if(elasticSearchLiteContext.autoIndexingEnabled) {
            unindex(event.entityObject)
        }
    }

    IndexResponse index(Object domainObject) {
        Class domainClass = domainObject.class
        IndexResponse response
        if(elasticSearchLiteContext.isSearchable(domainClass)) {
            log.debug("Indexing instance of ${domainClass} with id ${domainObject.id} into ElasticSearch")
            try {
                ElasticSearchMarshaller marshaller = elasticSearchLiteContext.getMarshaller(domainClass)

                response = prepareIndex(domainClass).setId(domainObject.id as String)
                        .setSource(marshaller.toSource(domainObject)).get()
            } catch (Exception e) {
                log.error("Unable to index instance of ${domainClass} with id ${domainObject.id} due to Exception", e)
            }
        }
        response
    }

    BulkResponse index(Object ... domainObjects) {
        index(domainObjects as List)
    }

    BulkResponse index(List domainObjects) {

        BulkRequestBuilder bulkRequest = client.prepareBulk()

        domainObjects.each { domainObject ->
            Class domainClass = domainObject.class
            if(elasticSearchLiteContext.isSearchable(domainClass)) {
                log.debug("Indexing instance of ${domainClass} with id ${domainObject.id} into ElasticSearch")
                try {
                    ElasticSearchMarshaller marshaller = elasticSearchLiteContext.getMarshaller(domainClass)
                    IndexRequestBuilder indexRequest = prepareIndex(domainClass)
                            .setId(domainObject.id as String)
                            .setSource(marshaller.toSource(domainObject))
                    bulkRequest.add(indexRequest)
                } catch (Exception e) {
                    log.error("Unable to index instance of ${domainClass} with id ${domainObject.id} due to Exception", e)
                }
            }
        }
        bulkRequest.get()
    }

    SearchResponse search(Class domainClass, QueryBuilder query) {
        SearchResponse response
        if(elasticSearchLiteContext.isSearchable(domainClass)) {
            try {
                response = prepareSearch(domainClass).setQuery(query).get()
            } catch (Exception e) {
                log.error("Unable to search instance of ${domainClass} with query ${query} due to Exception", e)
            }
        } else {
            log.warn("Attempted to serch an instance of ${domainClass}, which is not @Searchable")
        }
        response
    }

    DeleteResponse unindex(Object domainObject) {
        Class domainClass = domainObject.class
        DeleteResponse response
        if(elasticSearchLiteContext.isSearchable(domainClass)) {
            log.debug("Deleting instance of ${domainClass} with id ${domainObject.id} from ElasticSearch")
            try {
                response = prepareDelete(domainClass).setId(domainObject.id as String).get()
            } catch (Exception e) {
                log.info("Unable to delete instance of ${domainClass} with id ${domainObject.id} due to Exception", e)
            }
        }
        response
    }

    BulkResponse unindex(Object ... domainObjects) {
        index(domainObjects as List)
    }

    BulkResponse unindex(List domainObjects) {

        BulkRequestBuilder bulkRequest = client.prepareBulk()

        domainObjects.each { domainObject ->
            Class domainClass = domainObject.class
            if(elasticSearchLiteContext.isSearchable(domainClass)) {
                log.debug("Deleting instance of ${domainClass} with id ${domainObject.id} from ElasticSearch")
                try {
                    DeleteRequestBuilder deleteRequest = prepareDelete(domainClass).setId(domainObject.id as String)
                    bulkRequest.add(deleteRequest)
                } catch (Exception e) {
                    log.error("Unable to delete instance of ${domainClass} with id ${domainObject.id} due to Exception", e)
                }
            }
        }
        bulkRequest.get()
    }


    IndexRequestBuilder prepareIndex(Class domain) {
        ElasticSearchType esType = elasticSearchLiteContext.getType(domain)
        prepareIndex(esType)
    }

    IndexRequestBuilder prepareIndex(ElasticSearchType esType) {
        client.prepareIndex(esType.indexingIndex, esType.type)
    }

    SearchRequestBuilder prepareSearch(Class domain) {
        ElasticSearchType esType = elasticSearchLiteContext.getType(domain)
        prepareSearch(esType)
    }

    SearchRequestBuilder prepareSearch(ElasticSearchType esType) {
        client.prepareSearch(esType.queryingIndex).setTypes(esType.type)
    }

    DeleteRequestBuilder prepareDelete(Class domain) {
        ElasticSearchType esType = elasticSearchLiteContext.getType(domain)
        prepareDelete(esType)
    }

    DeleteRequestBuilder prepareDelete(ElasticSearchType esType) {
        client.prepareDelete().setIndex(esType.indexingIndex).setType(esType.type)
    }

    MoreLikeThisQueryBuilder.Item moreLike(Object domain) {
        MoreLikeThisQueryBuilder.Item item
        if(elasticSearchLiteContext.isSearchable(domain?.class)) {
            ElasticSearchType esType = elasticSearchLiteContext.getType(domain.class)
            item = new MoreLikeThisQueryBuilder.Item(esType.queryingIndex, esType.type, domain.id as String)
        }
        return item
    }
}
