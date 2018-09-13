package org.jetbrains.kotlinconf

import android.app.*
import android.content.*
import android.support.multidex.*
import android.widget.Toast
import kotlinx.coroutines.*
import kotlinx.coroutines.android.UI
import org.jetbrains.anko.*
import org.jetbrains.kotlinconf.model.*
import org.jetbrains.kotlinconf.presentation.DataRepository
import java.util.*

class KotlinConfApplication : Application(), AnkoLogger {
    lateinit var dataRepository: DataRepository
    lateinit var viewModel: KotlinConfViewModel

    override fun onCreate() {
        super.onCreate()

        Thread.setDefaultUncaughtExceptionHandler { _, throwable ->
            println(throwable)
            throwable.printStackTrace()
            throwable?.cause?.printStackTrace()
        }

        val userId = getUserId()

        dataRepository = KonfAppDataModel(userId)
        viewModel = KotlinConfViewModel(this, dataRepository, this::showError)

        launch(UI) {
            viewModel.update()
        }
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        MultiDex.install(this)
    }

    private fun showError(error: KotlinConfViewModel.Error) {
        val message = when (error) {
            KotlinConfViewModel.Error.FAILED_TO_DELETE_RATING -> R.string.msg_failed_to_delete_vote
            KotlinConfViewModel.Error.FAILED_TO_POST_RATING -> R.string.msg_failed_to_post_vote
            KotlinConfViewModel.Error.FAILED_TO_GET_DATA -> R.string.msg_failed_to_get_data
            KotlinConfViewModel.Error.EARLY_TO_VOTE -> R.string.msg_early_vote
            KotlinConfViewModel.Error.LATE_TO_VOTE -> R.string.msg_late_vote
        }
        toast(message)
    }

    private fun getUserId(): String {
        defaultSharedPreferences.getString(USER_ID_KEY, null)?.let { return it }

        val userId = "android-" + UUID.randomUUID().toString()
        defaultSharedPreferences
                .edit()
                .putString(USER_ID_KEY, userId)
                .apply()

        return userId
    }

    companion object {
        const val USER_ID_KEY = "UserId"
    }
}