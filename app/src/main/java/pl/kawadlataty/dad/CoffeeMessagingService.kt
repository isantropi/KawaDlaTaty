package pl.kawadlataty.dad

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class CoffeeMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(message: RemoteMessage) { super.onMessageReceived(message) }
}
