package io.github.egt17baa.morseprende.data.economy

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import io.github.egt17baa.morseprende.data.progress.AppDatabase
import io.github.egt17baa.morseprende.game.economy.BuyResult
import io.github.egt17baa.morseprende.game.economy.Shop
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Monedero de Destellos (una sola fila, id = 0). */
@Entity(tableName = "wallet")
data class WalletEntity(@PrimaryKey val id: Int = 0, val balance: Int, val earnedTotal: Int)

@Entity(tableName = "owned_items")
data class OwnedItemEntity(@PrimaryKey val itemId: String, val boughtAt: Long)

@Dao
interface EconomyDao {
    @Query("SELECT balance FROM wallet WHERE id = 0")
    fun observeBalance(): Flow<Int?>

    @Query("SELECT * FROM wallet WHERE id = 0")
    suspend fun wallet(): WalletEntity?

    @Upsert
    suspend fun upsertWallet(wallet: WalletEntity)

    @Query("SELECT itemId FROM owned_items")
    fun observeOwned(): Flow<List<String>>

    @Query("SELECT itemId FROM owned_items")
    suspend fun owned(): List<String>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOwned(item: OwnedItemEntity)

    @Transaction
    suspend fun earn(amount: Int) {
        if (amount <= 0) return
        val w = wallet() ?: WalletEntity(balance = 0, earnedTotal = 0)
        upsertWallet(w.copy(balance = w.balance + amount, earnedTotal = w.earnedTotal + amount))
    }

    /** Comprueba y cobra en la misma transacción: nunca hay saldo negativo ni cobro doble. */
    @Transaction
    suspend fun buy(itemId: String, now: Long): BuyResult {
        val w = wallet() ?: WalletEntity(balance = 0, earnedTotal = 0)
        val owned = owned().toSet() + Shop.defaults()
        val result = Shop.check(itemId, w.balance, owned)
        if (result == BuyResult.Bought) {
            val price = Shop.item(itemId)!!.price
            upsertWallet(w.copy(balance = w.balance - price))
            insertOwned(OwnedItemEntity(itemId, now))
        }
        return result
    }
}

interface EconomyRepository {
    val balance: Flow<Int>

    /** Objetos disponibles (incluye los de serie). */
    val owned: Flow<Set<String>>
    suspend fun earn(amount: Int)
    suspend fun buy(itemId: String): BuyResult

    /** true si ya existe el monedero (se crea con el primer Destello ganado). */
    suspend fun hasWallet(): Boolean
}

class RoomEconomyRepository(db: AppDatabase, private val now: () -> Long = System::currentTimeMillis) : EconomyRepository {
    private val dao = db.economyDao()
    override val balance: Flow<Int> = dao.observeBalance().map { it ?: 0 }
    override val owned: Flow<Set<String>> = dao.observeOwned().map { it.toSet() + Shop.defaults() }
    override suspend fun earn(amount: Int) = dao.earn(amount)
    override suspend fun buy(itemId: String): BuyResult = dao.buy(itemId, now())
    override suspend fun hasWallet(): Boolean = dao.wallet() != null
}
