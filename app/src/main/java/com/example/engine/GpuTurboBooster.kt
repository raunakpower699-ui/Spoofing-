package com.example.engine

import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.opengl.GLES20
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/**
 * Real Hardware GPU Overclock Pipeline.
 * Creates an offscreen EGL 512x512 PBuffer with heavy trigonometric raymarching
 * fragment shaders. Executes back-to-back GPU draw passes without delay,
 * driving Qualcomm Adreno (kgsl) & ARM Mali GPU frequency governors to maximum turbo clock.
 */
object GpuTurboBooster {

    private const val TAG = "GpuTurboBooster"
    private var gpuJob: Job? = null
    @Volatile private var isGpuBoostActive = false

    private const val VERTEX_SHADER = """
        attribute vec4 vPosition;
        void main() {
            gl_Position = vPosition;
        }
    """

    // Heavy multi-iteration fractal trigonometric fragment shader
    private const val FRAGMENT_SHADER = """
        precision highp float;
        uniform vec2 u_resolution;
        uniform float u_time;
        void main() {
            vec2 uv = (gl_FragCoord.xy * 2.0 - u_resolution.xy) / min(u_resolution.x, u_resolution.y);
            vec3 finalColor = vec3(0.0);
            for (float i = 0.0; i < 8.0; i++) {
                uv = fract(uv * 1.6) - 0.5;
                float d = length(uv) * exp(-length(uv));
                vec3 col = 0.5 + 0.5 * cos(u_time + i * 0.4 + vec3(0.0, 1.0, 2.0));
                d = sin(d * 8.0 + u_time) / 8.0;
                d = abs(d);
                d = pow(0.01 / (d + 0.0001), 1.2);
                finalColor += col * d;
            }
            gl_FragColor = vec4(finalColor, 1.0);
        }
    """

    fun startGpuOverclock() {
        if (isGpuBoostActive) return
        isGpuBoostActive = true

        gpuJob = CoroutineScope(Dispatchers.Default).launch {
            Log.i(TAG, "Starting Heavy Hardware EGL GPU Overclock Pipeline...")

            var eglDisplay: EGLDisplay = EGL14.EGL_NO_DISPLAY
            var eglContext: EGLContext = EGL14.EGL_NO_CONTEXT
            var eglSurface: EGLSurface = EGL14.EGL_NO_SURFACE
            var program = 0

            try {
                eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
                val version = IntArray(2)
                EGL14.eglInitialize(eglDisplay, version, 0, version, 1)

                val configAttribs = intArrayOf(
                    EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                    EGL14.EGL_SURFACE_TYPE, EGL14.EGL_PBUFFER_BIT,
                    EGL14.EGL_RED_SIZE, 8,
                    EGL14.EGL_GREEN_SIZE, 8,
                    EGL14.EGL_BLUE_SIZE, 8,
                    EGL14.EGL_DEPTH_SIZE, 16,
                    EGL14.EGL_NONE
                )

                val configs = arrayOfNulls<EGLConfig>(1)
                val numConfigs = IntArray(1)
                EGL14.eglChooseConfig(eglDisplay, configAttribs, 0, configs, 0, 1, numConfigs, 0)

                val contextAttribs = intArrayOf(
                    EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
                    EGL14.EGL_NONE
                )
                eglContext = EGL14.eglCreateContext(eglDisplay, configs[0], EGL14.EGL_NO_CONTEXT, contextAttribs, 0)

                // 512x512 Surface for real GPU computation
                val pbufferAttribs = intArrayOf(
                    EGL14.EGL_WIDTH, 512,
                    EGL14.EGL_HEIGHT, 512,
                    EGL14.EGL_NONE
                )
                eglSurface = EGL14.eglCreatePbufferSurface(eglDisplay, configs[0], pbufferAttribs, 0)
                EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)

                val vShader = compileShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER)
                val fShader = compileShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER)
                program = GLES20.glCreateProgram().also {
                    GLES20.glAttachShader(it, vShader)
                    GLES20.glAttachShader(it, fShader)
                    GLES20.glLinkProgram(it)
                }

                val quadCoords = floatArrayOf(
                    -1.0f,  1.0f,
                    -1.0f, -1.0f,
                     1.0f, -1.0f,
                    -1.0f,  1.0f,
                     1.0f, -1.0f,
                     1.0f,  1.0f
                )
                val vertexBuffer: FloatBuffer = ByteBuffer.allocateDirect(quadCoords.size * 4)
                    .order(ByteOrder.nativeOrder())
                    .asFloatBuffer()
                    .put(quadCoords)
                vertexBuffer.position(0)

                GLES20.glUseProgram(program)
                val posHandle = GLES20.glGetAttribLocation(program, "vPosition")
                GLES20.glEnableVertexAttribArray(posHandle)
                GLES20.glVertexAttribPointer(posHandle, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer)

                val timeHandle = GLES20.glGetUniformLocation(program, "u_time")
                val resHandle = GLES20.glGetUniformLocation(program, "u_resolution")
                GLES20.glUniform2f(resHandle, 512f, 512f)

                var frameTime = 0f
                var passCounter = 0

                // Continuous Hardware GPU Workload Loop (No artificial sleep)
                while (isActive && isGpuBoostActive) {
                    frameTime += 0.03f
                    GLES20.glUniform1f(timeHandle, frameTime)
                    GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6)
                    EGL14.eglSwapBuffers(eglDisplay, eglSurface)

                    passCounter++
                    if (passCounter % 15 == 0) {
                        yield() // Yield to maintain coroutine responsiveness without letting GPU idle
                    }
                }

            } catch (e: Exception) {
                Log.w(TAG, "Hardware GPU offscreen driver warning: ${e.message}")
            } finally {
                if (program != 0) GLES20.glDeleteProgram(program)
                if (eglDisplay != EGL14.EGL_NO_DISPLAY) {
                    EGL14.eglMakeCurrent(eglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
                    if (eglSurface != EGL14.EGL_NO_SURFACE) EGL14.eglDestroySurface(eglDisplay, eglSurface)
                    if (eglContext != EGL14.EGL_NO_CONTEXT) EGL14.eglDestroyContext(eglDisplay, eglContext)
                    EGL14.eglTerminate(eglDisplay)
                }
                Log.i(TAG, "Hardware GPU Overclock Pipeline stopped.")
            }
        }
    }

    fun stopGpuOverclock() {
        isGpuBoostActive = false
        gpuJob?.cancel()
        gpuJob = null
    }

    private fun compileShader(type: Int, code: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, code)
        GLES20.glCompileShader(shader)
        return shader
    }
}
