package com.example.localmiles.Core.Utils

object Role {
    const val ADMIN = "admin"      // the "dispatcher" persona in the UI
    const val DRIVER = "driver"
    const val CLIENT = "client"
}

object JobStatus {
    const val PENDING = "Pending"
    const val ASSIGNED = "Assigned"
    const val EN_ROUTE = "EnRoute"
    const val COMPLETED = "Completed"
    const val CANCELLED = "Cancelled"

    val ALL = listOf(PENDING, ASSIGNED, EN_ROUTE, COMPLETED, CANCELLED)

    //The only transitions a driver (non-admin) may make on their own job. */
    val DRIVER_SETTABLE = listOf(EN_ROUTE, COMPLETED)
}

object JobPriority {
    const val LOW = "Low"
    const val NORMAL = "Normal"
    const val HIGH = "High"
    const val URGENT = "Urgent"

    val ALL = listOf(LOW, NORMAL, HIGH, URGENT)
}

object StopStatus {
    const val PENDING = "Pending"
    const val ARRIVED = "Arrived"
    const val COMPLETED = "Completed"
    const val FAILED = "Failed"
    const val CANCELLED = "Cancelled"

    val ALL = listOf(PENDING, ARRIVED, COMPLETED, FAILED, CANCELLED)

    //The only transitions a driver (non-admin) may make on a stop. */
    val DRIVER_SETTABLE = listOf(ARRIVED, COMPLETED, FAILED)
}

//`ActionType` values the offline-sync endpoint knows how to apply. */
object SyncActionType {
    const val UPDATE_JOB_STATUS = "UpdateJobStatus"
    const val UPDATE_STOP_STATUS = "UpdateStopStatus"
}

//`EntityType` values for a queued sync action. */
object SyncEntityType {
    const val DELIVERY_JOB = "DeliveryJob"
    const val DELIVERY_STOP = "DeliveryStop"
}
