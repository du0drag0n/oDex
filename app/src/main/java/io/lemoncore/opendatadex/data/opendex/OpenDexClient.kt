package io.lemoncore.opendatadex.data.opendex

import io.lemoncore.opendatadex.data.MAX_SUPPORTED_GEN
import io.lemoncore.opendatadex.data.user.AssetDao
import io.lemoncore.opendatadex.data.user.SpeciesAssetEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

/**
 * Reads species artwork from the openDex API (https://api.opendex.lemoncore.io, OpenAPI at /openapi.json).
 *
 * openDex is a trading-card catalogue today; its `/v1/species` resource is the only part with game data the app can use
 * (official artwork and the default sprite per species). Everything else comes from the bundled seed until openDex grows.
 */
class OpenDexClient(private val http: OkHttpClient, private val baseUrl: String = BASE_URL) {
    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    data class SpeciesPage(val data: List<Species> = emptyList(), val nextAfter: Int? = null)

    @Serializable
    data class Species(val id: Int, val name: String = "", val assets: List<Asset> = emptyList())

    @Serializable
    data class Asset(val role: String = "", val url: String = "", val status: String = "")

    suspend fun species(after: Int?, limit: Int = PAGE_SIZE): SpeciesPage = withContext(Dispatchers.IO) {
        val url = buildString {
            append(baseUrl).append("/v1/species?limit=").append(limit)
            if (after != null) append("&after=").append(after)
        }
        http.newCall(Request.Builder().url(url).header("Accept", "application/json").build()).execute().use { response ->
            if (!response.isSuccessful) throw IOException("openDex ${response.code} for $url")
            json.decodeFromString(SpeciesPage.serializer(), response.body.string())
        }
    }

    companion object {
        const val BASE_URL = "https://api.opendex.lemoncore.io"
        const val PAGE_SIZE = 100 // the API's maximum
    }
}

/** Copies openDex species artwork URLs into the local database at most once a day. */
class AssetSync(private val client: OpenDexClient, private val dao: AssetDao, private val maxSpeciesId: Int) {
    suspend fun syncIfStale(now: Long = System.currentTimeMillis()): Result<Int> = runCatching {
        val last = dao.lastFetch()
        if (last != null && now - last < DAY_MS) return@runCatching 0
        var after: Int? = null
        val rows = mutableListOf<SpeciesAssetEntity>()
        while (true) {
            val page = client.species(after)
            rows += page.data.filter { it.id <= maxSpeciesId }.map { s ->
                fun pick(role: String) = s.assets.firstOrNull { it.role == role && it.status == "verified" && it.url.isNotBlank() }?.url
                SpeciesAssetEntity(s.id, pick("artwork"), pick("sprite"), now)
            }
            val next = page.nextAfter
            if (next == null || next >= maxSpeciesId || page.data.isEmpty()) break
            after = next
        }
        // Saved only once every page has arrived: a sync cut off partway leaves the old date, so the next launch tries again.
        dao.upsert(rows)
        rows.size
    }

    companion object {
        private const val DAY_MS = 24L * 60 * 60 * 1000
        /** National numbers of the last species in each supported generation. */
        val maxSpeciesForGen = mapOf(1 to 151, 2 to 251, 3 to 386, 4 to 493, 5 to 649, 6 to 721, 7 to 809, 8 to 905, 9 to 1025)
        val defaultMaxSpecies get() = maxSpeciesForGen.getValue(MAX_SUPPORTED_GEN)
    }
}
