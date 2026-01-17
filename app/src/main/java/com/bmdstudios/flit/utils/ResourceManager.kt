package com.bmdstudios.flit.utils

import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Manages application resources to ensure proper cleanup.
 * Tracks resources and provides automatic cleanup on errors.
 */
class ResourceManager {
    private val resources = ConcurrentHashMap<String, AutoCloseable>()
    private val resourceCounter = AtomicInteger(0)

    /**
     * Registers a resource for tracking and automatic cleanup.
     * @param resource The resource to track
     * @param name Optional name for the resource (for logging)
     * @return A unique ID for the resource
     */
    fun <T : AutoCloseable> register(resource: T, name: String? = null): String {
        val id = "resource_${resourceCounter.incrementAndGet()}_${name ?: resource.javaClass.simpleName}"
        resources[id] = resource
        Timber.v("Registered resource: $id")
        return id
    }

    /**
     * Unregisters a resource (typically called after manual cleanup).
     */
    fun unregister(id: String) {
        resources.remove(id)?.let {
            Timber.v("Unregistered resource: $id")
        }
    }

    /**
     * Releases a specific resource by ID.
     */
    fun release(id: String) {
        resources.remove(id)?.let { resource ->
            try {
                resource.close()
                Timber.d("Released resource: $id")
            } catch (e: Exception) {
                Timber.w(e, "Error releasing resource: $id")
            }
        }
    }

    /**
     * Releases all registered resources.
     */
    fun releaseAll() {
        Timber.d("Releasing all resources (${resources.size} total)")
        val resourcesToRelease = resources.values.toList()
        resources.clear()

        resourcesToRelease.forEach { resource ->
            try {
                resource.close()
            } catch (e: Exception) {
                Timber.w(e, "Error releasing resource: ${resource.javaClass.simpleName}")
            }
        }
        Timber.d("All resources released")
    }

    /**
     * Executes a block with automatic resource cleanup on error.
     */
    inline fun <T> withResource(resource: AutoCloseable, name: String? = null, block: (T) -> Unit) {
        val id = register(resource, name)
        try {
            @Suppress("UNCHECKED_CAST")
            block(resource as T)
        } catch (e: Exception) {
            Timber.w(e, "Error in resource block, releasing resource: $id")
            release(id)
            throw e
        }
    }

    /**
     * Gets the count of currently tracked resources.
     */
    fun getResourceCount(): Int = resources.size
}
