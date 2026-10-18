from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]

class SchedulerAndroidContractTest(unittest.TestCase):
    def test_manifest_declares_uidt_permission_and_non_exported_job_service(self):
        manifest = (ROOT / 'download-android/src/main/AndroidManifest.xml').read_text()
        self.assertIn('android.permission.RUN_USER_INITIATED_JOBS', manifest)
        self.assertIn('android.permission.BIND_JOB_SERVICE', manifest)
        self.assertIn('android:exported="false"', manifest)
        self.assertIn('.DownloadJobService', manifest)

    def test_uidt_scheduler_uses_required_android_14_job_flags(self):
        source = (ROOT / 'download-android/src/main/kotlin/app/novafetch/download/UidtDownloadScheduler.kt').read_text()
        self.assertIn('.setUserInitiated(true)', source)
        self.assertIn('.setEstimatedNetworkBytes(', source)
        self.assertIn('JobScheduler', source)

    def test_uidt_job_service_sets_notification_before_execution(self):
        source = (ROOT / 'download-android/src/main/kotlin/app/novafetch/download/DownloadJobService.kt').read_text()
        self.assertIn('setNotification(', source)
        self.assertIn('JOB_END_NOTIFICATION_POLICY_DETACH', source)
        self.assertIn('DownloadExecutionHost', source)

    def test_workmanager_scheduler_enqueues_unique_work(self):
        source = (ROOT / 'download-android/src/main/kotlin/app/novafetch/download/WorkManagerDownloadScheduler.kt').read_text()
        self.assertIn('enqueueUniqueWork(', source)
        self.assertIn('NetworkType.CONNECTED', source)
        self.assertIn('DownloadWorker', source)

if __name__ == '__main__':
    unittest.main()
