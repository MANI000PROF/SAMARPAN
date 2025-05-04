import android.content.Context
import com.google.auth.oauth2.GoogleCredentials
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileInputStream
import java.util.Collections

object FirebaseAccessToken {
    suspend fun getAccessToken(context: Context): String? = withContext(Dispatchers.IO) {
        try {
            val credentials = GoogleCredentials
                .fromStream(context.assets.open("serviceAccountKey.json"))
                .createScoped(listOf("https://www.googleapis.com/auth/firebase.messaging"))

            credentials.refreshIfExpired()
            credentials.accessToken.tokenValue
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}


