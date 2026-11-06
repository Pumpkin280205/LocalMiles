package com.example.localmiles.Screens.Driver.Sync

import com.example.localmiles.Core.Data.Local.OfflineActionEntity
import com.example.localmiles.Core.Network.Dto.StopResponse
import com.example.localmiles.Core.Network.JobResponse
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

private val gson = Gson()
private val MAP_TYPE = object : TypeToken<Map<String, Any>>() {}.type

// Works out what status each job/stop will have once its pending actions sync (newest wins)
private fun pendingStatuses(pending: List<OfflineActionEntity>, entityType: String): Map<Long, String> =
    pending.filter { it.entityType == entityType }
        .sortedBy { it.createdAt }
        .mapNotNull { a ->
            val status = when (a.actionType) {
                // the API marks the stop Completed itself when a POD comes in
                "SubmitPod" -> "Completed"
                else -> gson.fromJson<Map<String, Any>>(a.payload, MAP_TYPE)["Status"] as? String
            }
            status?.let { a.entityId to it }
        }
        .toMap()

// Puts the driver's unsynced changes on top of the server's stops so the list doesn't flip back
fun overlayStops(stops: List<StopResponse>, pending: List<OfflineActionEntity>): List<StopResponse> {
    val byId = pendingStatuses(pending, "Stop")
    return stops.map { s -> byId[s.stopId]?.let { s.copy(status = it) } ?: s }
}

fun overlayJobs(jobs: List<JobResponse>, pending: List<OfflineActionEntity>): List<JobResponse> {
    val byId = pendingStatuses(pending, "Job")
    return jobs.map { j -> byId[j.jobId]?.let { j.copy(status = it) } ?: j }
}
