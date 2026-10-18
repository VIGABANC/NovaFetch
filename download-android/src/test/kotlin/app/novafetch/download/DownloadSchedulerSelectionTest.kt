package app.novafetch.download

fun main() {
    val policy = DownloadSchedulerPolicy()

    check(policy.select(apiLevel = 34, userPaused = false) == SchedulerRoute.UIDT)
    check(policy.select(apiLevel = 35, userPaused = false) == SchedulerRoute.UIDT)
    check(policy.select(apiLevel = 33, userPaused = false) == SchedulerRoute.WORK_MANAGER)
    check(policy.select(apiLevel = 29, userPaused = false) == SchedulerRoute.WORK_MANAGER)
    check(policy.select(apiLevel = 36, userPaused = true) == SchedulerRoute.NONE)

    println("DownloadSchedulerSelectionTest: PASS")
}
