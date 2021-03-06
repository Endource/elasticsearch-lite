package grails.plugins.elasticsearch.lite

import grails.core.GrailsApplication
import grails.plugins.elasticsearch.exception.MappingException
import grails.plugins.elasticsearch.mapping.MappingMigrationStrategy
import grails.plugins.elasticsearch.util.ElasticSearchConfigAware
import grails.plugins.elasticsearch.util.IndexNamingUtils
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

/**
 * Created by marcoscarceles on 08/02/2017.
 */
@Slf4j
@CompileStatic
class LiteMigrationManager implements ElasticSearchConfigAware {

    GrailsApplication grailsApplication
    ElasticSearchLiteContext elasticSearchLiteContext
    ElasticSearchAdminService elasticSearchAdminService

    def applyMigrations(MappingMigrationStrategy migrationStrategy, Map<String, Map<Class<?>, ElasticSearchType>> indices, List<ElasticSearchType> mappingConflicts, Map indexSettings) {
        if(migrationStrategy == MappingMigrationStrategy.alias) {
            elasticSearchLiteContext.indicesRebuiltOnMigration = applyAliasStrategy(indices, mappingConflicts, indexSettings)
        }
    }

    private Set applyAliasStrategy(Map<String, Map<Class<?>, ElasticSearchType>> indices, List<ElasticSearchType> mappingConflicts, Map indexSettings) {

        Set<String> indexNames = mappingConflicts.collect { it.index } as Set<String>

        indexNames.each { String indexName ->
            log.debug("Creating new version and alias for conflicting index ${indexName}")
            boolean conflictOnAlias = elasticSearchAdminService.aliasExists(indexName)
            if (conflictOnAlias || migrationConfig?.aliasReplacesIndex) {
                if (!conflictOnAlias) {
                    elasticSearchAdminService.deleteIndex(indexName)
                }
                int nextVersion = elasticSearchAdminService.getNextVersion(indexName)
                List<ElasticSearchType> elasticSearchTypes = indices[indexName].values() as List
                rebuildIndexWithMappings(indexName, nextVersion, indexSettings, elasticSearchTypes)
                if(!conflictOnAlias) {
                    elasticSearchAdminService.pointAliasTo IndexNamingUtils.queryingIndexFor(indexName), indexName, nextVersion
                }
            } else {
                throw new MappingException("Could not create alias ${indexName} to solve error installing mappings, index with the same name already exists.")
            }

        }
        indices.keySet()
    }

    private void rebuildIndexWithMappings(String indexName, int nextVersion, Map indexSettings, List<ElasticSearchType> elasticSearchTypes) {
        elasticSearchAdminService.createIndex indexName, nextVersion, indexSettings, elasticSearchTypes
        elasticSearchAdminService.waitForIndex indexName, nextVersion //Ensure it exists so later on mappings are created on the right version
        elasticSearchAdminService.pointAliasTo indexName, indexName, nextVersion
        elasticSearchAdminService.pointAliasTo IndexNamingUtils.indexingIndexFor(indexName), indexName, nextVersion
    }
}
