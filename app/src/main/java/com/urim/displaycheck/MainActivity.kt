package com.urim.displaycheck

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.hardware.camera2.CameraCharacteristics
import android.media.MediaRecorder
import android.net.ConnectivityManager
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.DisplayMetrics
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File


class MainActivity : AppCompatActivity() {
    private var cameraProvider: ProcessCameraProvider? = null
    lateinit var mIntent: Intent
    lateinit var mRecognizer: SpeechRecognizer
    val PERMISSION = 1
    lateinit var sttResult: TextView
    lateinit var button : Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sttResult = findViewById(R.id.mic)
        button = findViewById<Button>(R.id.sttStart)

        if (!allPermissionsGranted()){
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }

        mIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        mIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, packageName);
        mIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR");

        button.setOnClickListener {
            mRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
            mRecognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(p0: Bundle?) {
                    button.text = "말씀하세요"
                }

                override fun onBeginningOfSpeech() {

                }

                override fun onRmsChanged(p0: Float) {
                }

                override fun onBufferReceived(p0: ByteArray?) {
                }

                override fun onEndOfSpeech() {
                    button.text = "음성인식 시작"
                }

                override fun onError(p0: Int) {
                    Toast.makeText(applicationContext, "에러 발생", Toast.LENGTH_SHORT).show()
                }

                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    matches?.get(0)?.let { sttResult.text = it }
                }

                override fun onPartialResults(p0: Bundle?) {
                }

                override fun onEvent(p0: Int, p1: Bundle?) {
                }

            });
            mRecognizer.startListening(intent);
        }


    }


    @ExperimentalCamera2Interop
    override fun onResume() {
        super.onResume()

        ProcessCameraProvider.getInstance(this).also { provider ->
            provider.addListener({
                cameraProvider = provider.get()
                if (cameraProvider == null) {
                    Log.e("Camera Provider", "null")
                }


                val camera = findViewById<TextView>(R.id.camera)
                ("Camera State: ${cameraProvider?.let { getCameraList(it) }}").also {
                    camera.text = it
                }

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(findViewById<PreviewView>(R.id.previewView).surfaceProvider)
                }

                val cameraSelector = selectExternalOrBestCamera(cameraProvider!!)

//            val cameraSelector = CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()

                try {
                    cameraProvider?.unbindAll()

                    cameraProvider?.bindToLifecycle(this, cameraSelector!!, preview)
                } catch (e: Exception) {
                    Toast.makeText(this, e.toString(), Toast.LENGTH_SHORT).show()
                }


            }, ContextCompat.getMainExecutor(this))
        }


        val dm = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(dm)

        val display = findViewById<TextView>(R.id.display)
        ("Client Display : densityDpi ${dm.densityDpi},\n\n$dm").also { display.text = it }

        val network = findViewById<TextView>(R.id.network)
        ("is Network Available? : ${isNetworkAvailable()}").also { network.text = it }

    }


    @ExperimentalCamera2Interop
    fun getCameraList(provider: ProcessCameraProvider): List<String> {
        val cam2Infos = provider.availableCameraInfos.map {
            Camera2CameraInfo.from(it)
        }.sortedByDescending {
            // HARDWARE_LEVEL is Int type, with the order of:
            // LEGACY < LIMITED < FULL < LEVEL_3 < EXTERNAL
            it.getCameraCharacteristic(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)
        }

        return cam2Infos.map { it.cameraId }
    }

    @ExperimentalCamera2Interop
    fun selectExternalOrBestCamera(provider: ProcessCameraProvider): CameraSelector? {
        val cam2Infos = provider.availableCameraInfos.map {
            Camera2CameraInfo.from(it)
        }.sortedByDescending {
            // HARDWARE_LEVEL is Int type, with the order of:
            // LEGACY < LIMITED < FULL < LEVEL_3 < EXTERNAL
            it.getCameraCharacteristic(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)
        }

        return when {
            cam2Infos.isNotEmpty() -> {
                CameraSelector.Builder()
                    .addCameraFilter {
                        it.filter { camInfo ->
                            // cam2Infos[0] is either EXTERNAL or best built-in camera
                            val thisCamId = Camera2CameraInfo.from(camInfo).cameraId
                            thisCamId == cam2Infos[0].cameraId
                        }
                    }.build()
            }
            else -> null
        }
    }


    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetworkInfo = connectivityManager.activeNetworkInfo
        return activeNetworkInfo != null && activeNetworkInfo.isConnected
    }

    //returns whether the microphone is available
    fun getMicrophoneAvailable(): Boolean {
        val recorder = MediaRecorder()
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
        recorder.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT)
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT)
        recorder.setOutputFile(
            File(
                applicationContext.getCacheDir(),
                "MediaUtil#micAvailTestFile"
            ).getAbsolutePath()
        )
        var available = true
        try {
            recorder.prepare()
            recorder.start()
        } catch (exception: Exception) {
            available = false
        }
        recorder.release()
        return available
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults:
        IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
            } else {
                Toast.makeText(
                    this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }


    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS =
            mutableListOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
            ).toTypedArray()
    }

}

fun pxToDp(px: Int): Int {
    return (px / Resources.getSystem().displayMetrics.density) as Int
}

fun dpToPx(dp: Int): Int {
    return (dp * Resources.getSystem().displayMetrics.density) as Int
}