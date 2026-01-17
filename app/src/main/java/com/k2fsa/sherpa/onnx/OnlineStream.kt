package com.k2fsa.sherpa.onnx

import timber.log.Timber

class OnlineStream(var ptr: Long = 0) {
    init {
        if (ptr != 0L) {
            Timber.d("OnlineStream initialized with native pointer: 0x${ptr.toString(16)}")
        } else {
            Timber.v("OnlineStream created with null pointer (will be initialized later)")
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

    fun inputFinished() {
        Timber.d("inputFinished called, ptr=0x${ptr.toString(16)}")
        inputFinished(ptr)
    }

    protected fun finalize() {
        if (ptr != 0L) {
            Timber.d("Finalizing OnlineStream, releasing native pointer: 0x${ptr.toString(16)}")
            delete(ptr)
            ptr = 0
        } else {
            Timber.v("Finalizing OnlineStream with null pointer (no cleanup needed)")
        }
    }

    fun release() {
        Timber.d("Releasing OnlineStream")
        finalize()
    }

    fun use(block: (OnlineStream) -> Unit) {
        Timber.d("Using OnlineStream with try-with-resources pattern")
        try {
            block(this)
        } catch (e: Exception) {
            Timber.e(e, "Exception in OnlineStream.use block")
            throw e
        } finally {
            Timber.d("Cleaning up OnlineStream in use() finally block")
            release()
        }
    }

    private external fun acceptWaveform(ptr: Long, samples: FloatArray, sampleRate: Int)
    private external fun inputFinished(ptr: Long)
    private external fun delete(ptr: Long)


    companion object {
        init {
            Timber.d("Loading native library: sherpa-onnx-jni")
            System.loadLibrary("sherpa-onnx-jni")
            Timber.d("Native library loaded successfully")
        }
    }
}
