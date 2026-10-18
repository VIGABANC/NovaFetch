package app.novafetch.download

enum class SchedulerRoute {
    UIDT,
    WORK_MANAGER,
    NONE,
}

class DownloadSchedulerPolicy {
    fun select(apiLevel: Int, userPaused: Boolean): SchedulerRoute = when {
        userPaused -> SchedulerRoute.NONE
        apiLevel >= 34 -> SchedulerRoute.UIDT
        else -> SchedulerRoute.WORK_MANAGER
    }
}
