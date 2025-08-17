package com.shinjiindustrial.portmapper.repository

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.time.Instant

data class IGD(val id: String, val name: String)
data class PortMapping(val id: String, val igdId: String, val description: String, val expiresAt: Instant?)

interface UpnpRepository {
    val igds: Flow<List<IGD>>
    fun mappingsFor(igdId: String): Flow<List<PortMapping>>
    fun allMappings(): Flow<List<PortMapping>>

    // “Command” APIs that also update the store when they succeed
    suspend fun refreshMappings(igdId: String)
    suspend fun upsertMapping(igdId: String, mapping: PortMapping)
    suspend fun deleteMapping(igdId: String, mappingId: String)
}

class InMemoryUpnpRepository(
    //private val client: UpnpClient,
    private val scope: CoroutineScope
) : UpnpRepository {

    // Store: IGD -> map of PortMappings
    private val store = MutableStateFlow<Map<String, Map<String, PortMapping>>>(emptyMap())

    override val igds: Flow<List<IGD>> =
        store.map { m -> m.keys.map { IGD(it, name = it) } }  // plug real names if you have them

    override fun mappingsFor(igdId: String): Flow<List<PortMapping>> =
        store.map { it[igdId]?.values?.toList().orEmpty() }

    override fun allMappings(): Flow<List<PortMapping>> =
        store.map { it.values.flatMap { mm -> mm.values } }

    override suspend fun refreshMappings(igdId: String) {
        //val fresh: List<PortMapping> = client.fetchMappings(igdId) // network/UPnP
//        store.update { s ->
//            s + (igdId to fresh.associateBy { it.id })
//        }
    }

    override suspend fun upsertMapping(igdId: String, mapping: PortMapping) {
        //client.addOrUpdateMapping(igdId, mapping) // if this throws, nothing changes
        store.update { s ->
            val oldMap = s[igdId].orEmpty()
            s + (igdId to (oldMap + (mapping.id to mapping)))
        }
    }

    override suspend fun deleteMapping(igdId: String, mappingId: String) {
//        client.deleteMapping(igdId, mappingId)
//        store.update { s ->
//            val oldMap = s[igdId].orEmpty()
//            s + (igdId to (oldMap - mappingId))
//        }
    }
}
