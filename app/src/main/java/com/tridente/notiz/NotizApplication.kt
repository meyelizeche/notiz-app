package com.tridente.notiz

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.tridente.notiz.data.local.NotizDatabase
import com.tridente.notiz.data.repository.AuthRepository
import com.tridente.notiz.data.repository.FolderRepository
import com.tridente.notiz.data.repository.NoteRepository
import com.tridente.notiz.data.repository.ShareRepository
import com.tridente.notiz.data.repository.SyncRepository
import com.tridente.notiz.data.security.TokenManager
import com.tridente.notiz.sync.SyncWorker
import java.util.concurrent.TimeUnit

class NotizApplication : Application() {

    lateinit var database: NotizDatabase
        private set

    lateinit var tokenManager: TokenManager
        private set

    lateinit var authRepository: AuthRepository
        private set

    lateinit var folderRepository: FolderRepository
        private set

    lateinit var noteRepository: NoteRepository
        private set

    lateinit var shareRepository: ShareRepository
        private set

    lateinit var syncRepository: SyncRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Initialize Local Room Database
        database = NotizDatabase.getDatabase(this)

        // Initialize Encrypted Token Manager
        tokenManager = TokenManager(this)

        // Initialize Repositories
        authRepository = AuthRepository(database.userDao(), tokenManager)
        folderRepository = FolderRepository(database.folderDao(), database.syncQueueDao())
        noteRepository = NoteRepository(database.noteDao(), database.attachmentDao(), database.syncQueueDao())
        shareRepository = ShareRepository(database.noteShareDao(), database.syncQueueDao())
        syncRepository = SyncRepository(
            database.folderDao(),
            database.noteDao(),
            database.attachmentDao(),
            database.syncQueueDao(),
            tokenManager
        )

        createNotificationChannel()
        setupPeriodicSync()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Notiz Recordatorios"
            val descriptionText = "Notificaciones de fechas límite para tus notas"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(true)
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun setupPeriodicSync() {
        val syncConstraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(syncConstraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "NotizPeriodicSync",
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
    }

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "notiz_reminders_channel"
        lateinit var instance: NotizApplication
            private set
    }
}
