package com.mtdevelopment.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class DataResultTest {

    @Test
    fun `map transforms success data`() {
        val result: DataResult<Int> = DataResult.Success(21)
        val mapped = result.map { it * 2 }
        assertEquals(DataResult.Success(42), mapped)
    }

    @Test
    fun `map preserves error`() {
        val exception = IllegalStateException("boom")
        val result: DataResult<Int> = DataResult.Error(exception, "boom")

        val mapped = result.map { it * 2 }

        val error = mapped as DataResult.Error
        assertSame(exception, error.exception)
        assertEquals("boom", error.message)
    }

    @Test
    fun `map preserves loading`() {
        val result: DataResult<Int> = DataResult.Loading
        assertSame(DataResult.Loading, result.map { it * 2 })
    }
}
