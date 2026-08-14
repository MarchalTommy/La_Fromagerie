package com.mtdevelopment.admin.domain.usecase

import com.mtdevelopment.admin.domain.repository.FirebaseAdminRepository
import com.mtdevelopment.core.model.PickupPoint
import com.mtdevelopment.core.model.PickupPointType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PickupPointUseCasesTest {

    private val repository: FirebaseAdminRepository = mockk(relaxed = true)

    private fun market(id: String, date: String, label: String = "Marché $id") = PickupPoint(
        id = id,
        type = PickupPointType.MARKET,
        label = label,
        address = "Place Saint-Bénigne, 25300 Pontarlier",
        date = date
    )

    private fun shop() = PickupPoint(
        id = "shop",
        type = PickupPointType.SHOP,
        label = "La Fromagerie",
        address = "3 Grande Rue, 25300 Pontarlier",
        openingDays = listOf("SATURDAY")
    )

    @Test
    fun `points come back with the shop first and markets in chronological order`() = runTest {
        coEvery { repository.getAllPickupPoints() } returns Result.success(
            listOf(
                market("m2", "20/09/2026"),
                shop(),
                market("m1", "15/08/2026")
            )
        )

        val result = GetAllPickupPointsUseCase(repository).invoke()

        assertEquals(
            listOf("shop", "m1", "m2"),
            result.getOrThrow().map { it.id }
        )
    }

    @Test
    fun `a read failure is propagated rather than flattened to an empty list`() = runTest {
        // An empty list would tell the shop it has no market dates configured, which is a
        // very different statement from "we could not read them".
        coEvery { repository.getAllPickupPoints() } returns Result.failure(RuntimeException("offline"))

        val result = GetAllPickupPointsUseCase(repository).invoke()

        assertTrue(result.isFailure)
    }

    @Test
    fun `an incomplete point is refused instead of being stored`() = runTest {
        val incomplete = shop().copy(openingDays = emptyList())

        val added = AddNewPickupPointUseCase(repository).invoke(incomplete)
        val updated = UpdatePickupPointUseCase(repository).invoke(incomplete)

        assertTrue(added.isFailure)
        assertTrue(updated.isFailure)
        coVerify(exactly = 0) { repository.addNewPickupPoint(any()) }
        coVerify(exactly = 0) { repository.updatePickupPoint(any()) }
    }

    @Test
    fun `a complete point reaches the repository`() = runTest {
        coEvery { repository.addNewPickupPoint(any()) } returns Result.success(Unit)

        val result = AddNewPickupPointUseCase(repository).invoke(market("m1", "15/08/2026"))

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { repository.addNewPickupPoint(any()) }
    }

    @Test
    fun `deleting does not require a complete point`() = runTest {
        // Whatever made a stored point invalid must not block removing it.
        coEvery { repository.deletePickupPoint(any()) } returns Result.success(Unit)

        val result = DeletePickupPointUseCase(repository).invoke(
            shop().copy(openingDays = emptyList())
        )

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { repository.deletePickupPoint(any()) }
    }
}
