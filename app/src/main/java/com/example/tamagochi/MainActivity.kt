package com.example.tamagochi

import android.annotation.SuppressLint
import android.app.PendingIntent.getActivity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.DisplayMetrics
import android.util.Rational
import android.view.View
import android.widget.ImageView
import kotlinx.android.synthetic.main.activity_main.*
import java.sql.Time
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.*
import kotlin.concurrent.scheduleAtFixedRate
import kotlin.concurrent.timerTask

class MainActivity : AppCompatActivity(), SensorEventListener {
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private lateinit var tamagochi : Tamagochi
    private lateinit var sensorManager : SensorManager
    private lateinit var accelerometer : Sensor
    private lateinit var compass : Sensor
    private lateinit var light : Sensor
    private val timer = Timer()
    private val handler = Handler()
    var width = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        initSensors()

        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        width = displayMetrics.widthPixels
        initField(width - 50)
        tamagochi =  Tamagochi(field)
        timer.scheduleAtFixedRate(0, 30) {
            handler.post(Runnable{tamagochi.update()})

        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration?) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)

        if (isInPictureInPictureMode) {
            button.visibility = View.INVISIBLE
            initField(415)
        } else {
            button.visibility = View.VISIBLE
            initField(width)
        }
    }

    fun initSensors() {
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager!!.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        compass = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        light = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);

        sensorManager!!.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        sensorManager!!.registerListener(this, compass, SensorManager.SENSOR_DELAY_GAME)
        sensorManager!!.registerListener(this, light, SensorManager.SENSOR_DELAY_GAME)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null)
            return

        when (event.sensor.getType()) {
            Sensor.TYPE_ACCELEROMETER -> {
                tamagochi.acs = PointF(-event.values[0], event.values[1])
            }
            Sensor.TYPE_LIGHT -> {
                tamagochi.light = event.values[0]
            }
        }
    }

    fun btnClick(v : View) {
        val rational = Rational(field.width,
            field.height)


        val params = PictureInPictureParams.Builder()
            .setAspectRatio(rational)
            .build()

        enterPictureInPictureMode(params)
    }

    fun initField(size : Int) {
        field.layoutParams.height = size
        field.layoutParams.width = size
        field.requestLayout()
    }
}

class Tamagochi(field: ImageView) {
    private val map : ImageView = field
    private var bitmap = (map.drawable as BitmapDrawable).bitmap.copy(Bitmap.Config.ARGB_8888, true)
    private val radius = 50f
    private val offset = PointF(50f, 50f)
    private var pos = PointF(0f, 0f)
    var acs = PointF(0f, 0f)
    private var speed = PointF(0f, 0f)
    var light = 0f
    private var openEyes = true
    private var time = System.currentTimeMillis()
    private var blinkTime = System.currentTimeMillis()
    private var blinkCount = 0

    fun update(){
        speed.x += acs.x * 10
        speed.y += acs.y * 10
        pos.x += speed.x / 100
        pos.y += speed.y / 100
        val timeNow = System.currentTimeMillis()
        if (light > 0) {
            time = timeNow
            if (!openEyes && blinkCount == 0) {
                blinkCount = 5
                blinkTime = System.currentTimeMillis()
                openEyes = true
            }
        }

        if (timeNow - time > 3000) {
            openEyes = false
            blinkCount = 0
        }

        if (blinkCount > 0) {
            if (blinkCount % 2 == 0) {
                if (timeNow - blinkTime > 200) {
                    openEyes = false
                    blinkTime = System.currentTimeMillis()
                    blinkCount--
                }
            }
            else {
                if (timeNow - blinkTime > 200) {
                    blinkTime = System.currentTimeMillis()
                    openEyes = true
                    blinkCount--
                }
            }
        }

        if (pos.x + offset.x + radius > bitmap.width){
            pos.x -= 2 * ((pos.x + offset.x + radius) - bitmap.width)
            speed.x *= -0.5f
        }
        if (pos.x + offset.x - radius < 0){
            pos.x -= 2*(pos.x + offset.x - radius)
            speed.x *= -0.5f
        }
        if (pos.y + offset.y + radius > bitmap.width){
            pos.y -= 2 * ((pos.y + offset.y + radius) - bitmap.width)
            speed.y *= -0.5f
        }
        if (pos.y + offset.y - radius < 0){
            pos.y -= 2*(pos.y + offset.y - radius)
            speed.y *= -0.5f
        }

        draw()
    }

    fun draw(){
        val image = map.drawable
        if (image is BitmapDrawable) {
            val tmpBitmap = bitmap.copy(bitmap.config, true)
            val canvas = Canvas(tmpBitmap)
            val paint = Paint()
            paint.color = Color.BLUE
            canvas.drawCircle(pos.x + offset.x, pos.y + offset.y + 13f, radius, paint)

            paint.color = Color.BLACK
            if (openEyes) {
                canvas.drawCircle(pos.x + offset.x + 12f, pos.y + offset.y - 10f + 13f, 8f, paint)
                canvas.drawCircle(pos.x + offset.x - 12f, pos.y + offset.y - 10f + 13f, 8f, paint)
            }
            else {
                paint.strokeWidth = 5f
                canvas.drawLine(pos.x + offset.x + 6f, pos.y + offset.y - 10f + 13f, pos.x + offset.x + 18f, pos.y + offset.y - 10f + 13f, paint)
                canvas.drawLine(pos.x + offset.x - 6f, pos.y + offset.y - 10f + 13f, pos.x + offset.x - 18f, pos.y + offset.y - 10f + 13f, paint)
            }

            map.setImageBitmap(tmpBitmap)
        }
    }
}


