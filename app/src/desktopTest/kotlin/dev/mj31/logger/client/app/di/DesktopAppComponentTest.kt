package dev.mj31.logger.client.app.di

import com.google.common.truth.Truth.assertThat
import kotlin.test.Test

/**
 * The graph itself is checked by the compiler, so this test only pins the wiring decisions a type
 * checker cannot see: that the workspace really shares one instance of the stateful collaborators.
 */
class DesktopAppComponentTest {

    @Test
    fun `the component builds a usable workspace`() {
        val component = DesktopAppComponent::class.create()
        try {
            assertThat(component.store.state.value.totalEntryCount).isEqualTo(0)
            assertThat(component.store.state.value.sync.isSynced).isFalse()
            assertThat(component.fileChooser).isNotNull()
        } finally {
            component.dispose()
        }
    }

    @Test
    fun `stateful collaborators are shared inside the component`() {
        val component = DesktopAppComponent::class.create()
        try {
            assertThat(component.store).isSameInstanceAs(component.store)
            assertThat(component.applicationScope).isSameInstanceAs(component.applicationScope)
            assertThat(component.fileChooser).isSameInstanceAs(component.fileChooser)
        } finally {
            component.dispose()
        }
    }

    @Test
    fun `two components are independent workspaces`() {
        val first = DesktopAppComponent::class.create()
        val second = DesktopAppComponent::class.create()
        try {
            assertThat(first.store).isNotSameInstanceAs(second.store)
        } finally {
            first.dispose()
            second.dispose()
        }
    }
}
