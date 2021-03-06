package com.applivery.applvsdklib.features.feedback

import com.applivery.base.domain.model.Feedback
import com.applivery.base.util.AppliveryLog
import com.applivery.data.AppliveryApiService
import com.applivery.data.request.FeedbackRequest
import com.google.gson.JsonParseException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException

class FeedbackUseCase(
    private val apiService: AppliveryApiService
) {

    fun sendFeedback(feedback: Feedback, callback: (Boolean) -> Unit) =
        GlobalScope.launch(Dispatchers.Main) {

            try {
                val feedbackRequest = FeedbackRequest.fromFeedback(feedback)
                val response =
                    withContext(Dispatchers.IO) { apiService.sendFeedback(feedbackRequest) }

                callback(response.successful)

            } catch (parseException: JsonParseException) {
                AppliveryLog.error("feedback parse error", parseException)
                callback(false)
            } catch (httpException: HttpException) {
                AppliveryLog.error("Send feedback - Network error", httpException)
                callback(false)
            } catch (ioException: IOException) {
                AppliveryLog.error("Send feedback config error", ioException)
                callback(false)
            }
        }

    companion object {
        @Volatile
        private var instance: FeedbackUseCase? = null

        fun getInstance() =
            instance ?: synchronized(this) {
                instance ?: buildFeedbackUseCase().also {
                    instance = it
                }
            }

        private fun buildFeedbackUseCase() = FeedbackUseCase(
            apiService = AppliveryApiService.getInstance()
        )
    }
}
