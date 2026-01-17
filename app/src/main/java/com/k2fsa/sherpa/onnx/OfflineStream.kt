package com.k2fsa.sherpa.onnx

import timber.log.Timber

class OfflineStream(var ptr: Long) {
    init {
        if (ptr != 0L) {
            Timber.d("OfflineStream initialized with native pointer: 0x${ptr.toString(16)}")
        } else {
            Timber.w("OfflineStream created with null pointer - this may indicate an initialization error")
        }
    }
    fun acceptWaveform(samples: FloatArray, sampleRate: Int) {
        if (ptr == 0L) {
            Timber.w("acceptWaveform called with null pointer")
        } else {
            Timber.v("acceptWaveform: ${samples.size} samples at $sampleRate Hz")
        }
        acceptWaveform(ptr, samples, sampleRate)
    }

    protected fun finalize() {
        if (ptr != 0L) {
            Timber.d("Finalizing OfflineStream, releasing native pointer: 0x${ptr.toString(16)}")
            delete(ptr)
            ptr = 0
        } else {
            Timber.v("Finalizing OfflineStream with null pointer (no cleanup needed)")
        }
    }

    fun release() {
        Timber.d("Releasing OfflineStream")
        finalize()
    }

    fun use(block: (OfflineStream) -> Unit) {
        Timber.d("Using OfflineStream with try-with-resources pattern")
        try {
            block(this)
        } catch (e: Exception) {
            Timber.e(e, "Exception in OfflineStream.use block")
            throw e
        } finally {
            Timber.d("Cleaning up OfflineStream in use() finally block")
            release()
        }
    }

    private external fun acceptWaveform(ptr: Long, samples: FloatArray, sampleRate: Int)
    private external fun delete(ptr: Long)

    companion object {
        init {
            Timber.d("Loading native library: sherpa-onnx-jni")
            System.loadLibrary("sherpa-onnx-jni")
            Timber.d("Native library loaded successfully")
        }
    }
}
