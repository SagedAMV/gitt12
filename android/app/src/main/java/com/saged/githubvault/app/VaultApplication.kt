package com.saged.githubvault.app
import android.app.Application
import androidx.room.Room
import com.saged.githubvault.core.database.VaultDatabase
import com.saged.githubvault.core.security.SessionStore
import com.saged.githubvault.core.network.VaultApi
class VaultApplication:Application() {
 override fun onCreate() {
  super.onCreate()
  val connectivity=getSystemService(android.net.ConnectivityManager::class.java)
  connectivity.registerDefaultNetworkCallback(object:android.net.ConnectivityManager.NetworkCallback(){
   override fun onAvailable(network:android.net.Network) {com.saged.githubvault.feature.downloads.SyncWorker.enqueue(this@VaultApplication)}
  })
 }

 val database by lazy {Room.databaseBuilder(this,VaultDatabase::class.java,"vault.db").build()}
 val github by lazy {com.saged.githubvault.feature.github.GitHubClient(this)}
 val api by lazy {VaultApi(SessionStore(this),this)}
}
