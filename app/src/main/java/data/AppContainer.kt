package data

import android.app.Application
import android.content.Context

/**
 * App container for Dependency injection.
 */
interface AppContainer {
    val itemsRepository: ItemsRepository
   val loginRepository: LoginRepository
}

/**
 * [AppContainer] implementation that provides instances of [OfflineItemsRepository] and [DefaultLoginRepository]
 */
class AppDataContainer(private val context: Context) : AppContainer {
    /**
     * Implementation for [ItemsRepository]
     */
    override val itemsRepository: ItemsRepository by lazy {
        OfflineItemsRepository(InventoryDatabase.getDatabase(context).itemDao())
    }

    /**
     * Implementation for [LoginRepository]
     */
    override val loginRepository: LoginRepository by lazy {
        LoginRepository(LoginDatabase.getDatabase( context).loginDao())
    }
}
