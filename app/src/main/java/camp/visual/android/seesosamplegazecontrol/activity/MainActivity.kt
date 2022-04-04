package camp.visual.android.seesosamplegazecontrol.activity

import android.Manifest
import android.content.pm.PackageManager
import android.os.*
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import camp.visual.android.seesosamplegazecontrol.R
import camp.visual.android.seesosamplegazecontrol.databinding.ActivityMainBinding
import camp.visual.android.seesosamplegazecontrol.seeso.GazeTrackerManager
import camp.visual.android.seesosamplegazecontrol.ui.button.ButtonFragment
import camp.visual.android.seesosamplegazecontrol.ui.select.SelectFragment
import camp.visual.android.seesosamplegazecontrol.ui.scroll.ScrollFragment
import camp.visual.android.seesosamplegazecontrol.view.CalibrationView
import camp.visual.android.seesosamplegazecontrol.view.PointView
import camp.visual.gazetracker.GazeTracker
import camp.visual.gazetracker.callback.CalibrationCallback
import camp.visual.gazetracker.callback.GazeCallback
import camp.visual.gazetracker.callback.InitializationCallback
import camp.visual.gazetracker.callback.StatusCallback
import camp.visual.gazetracker.constant.AccuracyCriteria
import camp.visual.gazetracker.constant.CalibrationModeType
import camp.visual.gazetracker.constant.InitializationErrorType
import camp.visual.gazetracker.constant.StatusErrorType
import camp.visual.gazetracker.filter.OneEuroFilterManager
import camp.visual.gazetracker.gaze.GazeInfo
import camp.visual.gazetracker.state.ScreenState
import camp.visual.gazetracker.util.ViewLayoutChecker
import kotlinx.android.synthetic.main.activity_main.*

/****************************************
 *
 * Basic Process Logic:
 * Check Camera permission ->
 * initialize SeeSo GazeTracker ->
 * callback(initialization success) ->
 * start SeeSo tracking ->
 * callback(start tracking success) ->
 * callback(gazeInfo) ->
 * send gazeInfo data to fragments
 *
 ****************************************/

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private val tag: String = "SeeSo"

    // Required Permissions
    private val permissions = arrayOf(
        Manifest.permission.CAMERA
    )
    private val permissionCode: Int = 1000


    // View
    private var viewPoint: PointView? = null
    private var viewCalibration: CalibrationView? = null
    private var bottomNavigationView: BottomNavigationView? = null
    private var navigationHostFragment: Fragment? = null


    // SeeSo
    private var gazeTrackerManager: GazeTrackerManager? = null
    private val viewLayoutChecker: ViewLayoutChecker = ViewLayoutChecker()
    private val backgroundThread: HandlerThread = HandlerThread("background")
    private var backgroundHandler: Handler? = null
    private val trackingFps: Int = 30


    // Screen Offset
    private var offsets: IntArray = IntArray(2)


    // Calibration
    private val calibrationType: CalibrationModeType = CalibrationModeType.DEFAULT
    private val criteria: AccuracyCriteria = AccuracyCriteria.DEFAULT


    // OneEuro Filter
    private val oneEuroFilterManager: OneEuroFilterManager = OneEuroFilterManager(2)
    private val isUseGazeFilter: Boolean = true


    // LifeCycle
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_activity_main)

        navView.setupWithNavController(navController)

        // SeeSo GazeTracker Manager
        gazeTrackerManager = GazeTrackerManager.makeNewInstance(this)
        Log.i(tag, "SeeSo GazeTracker version: " + GazeTracker.getVersionName())

        // View
        viewPoint = findViewById(R.id.view_point)
        viewCalibration = findViewById(R.id.view_calibration)
        bottomNavigationView = findViewById(R.id.nav_view)
        navigationHostFragment = supportFragmentManager.findFragmentByTag("navHostFragment")

        // Handler
        initHandler()

        // Button
        initButtonEvent()

        // Permission
        checkPermission()
    }

    override fun onStart() {
        super.onStart()

        // Set Callbacks for SeeSo GazeTracker
        gazeTrackerManager?.setGazeTrackerCallbacks(
            gazeCallback,
            calibrationCallback,
            statusCallback,
        )
    }

    override fun onResume() {
        super.onResume()

        // Set Offset for Views
        setOffsetOfView()
    }

    override fun onDestroy() {
        super.onDestroy()

        // Handler
        releaseHandler()
    }


    // permission
    private fun checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!hasPermissions(permissions)) {
                requestPermissions(permissions, permissionCode)
            } else {
                checkPermission(true)
            }
        } else {
            checkPermission(true)
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun hasPermissions(permissions: Array<String>): Boolean {
        // Check permission status in string array
        for (perms in permissions) {
            if (perms == Manifest.permission.SYSTEM_ALERT_WINDOW) {
                if (!Settings.canDrawOverlays(this)) {
                    return false
                }
            }
            val result = ContextCompat.checkSelfPermission(this, perms)
            if (result == PackageManager.PERMISSION_DENIED) {
                // Unauthorized permission is found
                return false
            }
        }
        // All permissions are allowed
        return true
    }

    private fun checkPermission(isGranted: Boolean) {
        if (isGranted) {
            permissionGranted()
        } else {
            finish()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            permissionCode -> if (grantResults.isNotEmpty()) {
                val cameraPermissionAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED
                if (cameraPermissionAccepted) {
                    checkPermission(true)
                } else {
                    checkPermission(false)
                }
            }
        }
    }

    private fun permissionGranted() {
        // Initialize SeeSo GazeTracker
        initGaze()
    }


    // Handler
    private fun initHandler() {
        backgroundThread.start()
        backgroundHandler = Handler(backgroundThread.looper)
    }

    private fun releaseHandler() {
        backgroundThread.quitSafely()
    }


    // Button Event
    private fun initButtonEvent() {
        btn_calibration?.let {
            it.setOnClickListener {
                if (isTracking() == true) {
                    startCalibration()
                }
            }
        }

        btn_roi?.let {
            it.setOnClickListener {
                // Set ROI Visibility
                val fragments: List<Fragment>? = navigationHostFragment?.childFragmentManager?.fragments
                if (fragments != null) {
                    for (fragment in fragments!!) {
                        val buttonFragment = fragment as? ButtonFragment
                        val selectFragment = fragment as? SelectFragment
                        val scrollFragment = fragment as? ScrollFragment
                        buttonFragment?.updateROI()
                        selectFragment?.updateROI()
                        scrollFragment?.updateROI()
                    }
                }
            }
        }
    }


    // View
    private fun setOffsetOfView() {
        viewLayoutChecker.setOverlayView(viewPoint as View) { x, y ->
            viewPoint?.setOffset(x, y)
            viewCalibration?.setOffset(x, y)

            offsets[0] = x
            offsets[1] = y
        }
    }

    private fun showGazePoint(x: Float, y: Float, type: ScreenState) {
        runOnUiThread {
            viewPoint?.setType(if (type == ScreenState.INSIDE_OF_SCREEN) PointView.TYPE_DEFAULT else PointView.TYPE_OUT_OF_SCREEN)
            viewPoint?.setPosition(x, y)
        }
    }

    private fun setCalibrationPoint(x: Float, y: Float) {
        runOnUiThread {
            viewCalibration?.setPointPosition(x, y)
            viewCalibration?.setPointAnimationPower(0.0f)
        }
    }

    private fun setCalibrationProgress(progress: Float) {
        runOnUiThread {
            viewCalibration?.setPointAnimationPower(progress)
        }
    }

    private fun showCalibrationView() {
        runOnUiThread {
            viewCalibration?.visibility = View.VISIBLE
            viewPoint?.visibility = View.INVISIBLE
            bottomNavigationView?.visibility = View.INVISIBLE
        }
    }

    private fun hideCalibrationView() {
        runOnUiThread {
            viewCalibration?.visibility = View.INVISIBLE
            viewPoint?.visibility = View.VISIBLE
            bottomNavigationView?.visibility = View.VISIBLE
        }
    }


    // Gaze Filter (OneEuro)
    private fun processFilterGaze(gazeInfo: GazeInfo): FloatArray {
        if (isUseGazeFilter) {
            if (oneEuroFilterManager.filterValues(gazeInfo.timestamp, gazeInfo.x, gazeInfo.y)) {
                return oneEuroFilterManager.filteredValues
            }
        }
        return floatArrayOf(gazeInfo.x, gazeInfo.y)
    }


    // SeeSo GazeTracker Functions
    private fun initGaze() {
        gazeTrackerManager?.initGazeTracker(initializationCallback)
    }

    private fun releaseGaze() {
        gazeTrackerManager?.deInitGazeTracker()
    }

    private fun isTracking(): Boolean? {
        return gazeTrackerManager?.isTracking()
    }

    private fun startTracking() {
        gazeTrackerManager?.startGazeTracking()
    }

    private fun stopTracking() {
        gazeTrackerManager?.stopGazeTracking()
    }

    private fun setTrackingFps(fps: Int) {
        gazeTrackerManager?.setGazeTrackingFps(fps)
    }

    private fun startCalibration() {
        gazeTrackerManager?.startCalibration(calibrationType, criteria)
        showCalibrationView()
    }

    private fun startCollectSamples() {
        gazeTrackerManager?.startCollectingCalibrationSamples()
    }

    private fun stopCalibration() {
        gazeTrackerManager?.stopCalibration()
        hideCalibrationView()
    }


    // SeeSo GazeTracker Callback
    private val initializationCallback = object : InitializationCallback {
        override fun onInitialized(gazeTracker: GazeTracker?, error: InitializationErrorType) {
            if (gazeTracker != null) {
                setTrackingFps(trackingFps)
                startTracking()
            } else {
                showToast("SeeSo GazeTracker Init Failed: $error", false)
            }
        }
    }

    private val gazeCallback = object : GazeCallback {
        override fun onGaze(gazeInfo: GazeInfo) {
            if (gazeTrackerManager?.isCalibrating() == false) {
                val gaze = processFilterGaze(gazeInfo)

                // Draw Gaze Point
                runOnUiThread {
                    showGazePoint(gaze[0], gaze[1], gazeInfo.screenState)
                }

                // Apply Offsets for Fragment
                gazeInfo.x = gaze[0] - offsets[0]
                gazeInfo.y = gaze[1] - offsets[1]

                // Send Gaze Data to each Fragments
                val fragments: List<Fragment>? = navigationHostFragment?.childFragmentManager?.fragments
                if (fragments != null) {
                    for (fragment in fragments!!) {
                        val buttonFragment = fragment as? ButtonFragment
                        val selectFragment = fragment as? SelectFragment
                        val scrollFragment = fragment as? ScrollFragment
                        buttonFragment?.onGaze(gazeInfo, trackingFps)
                        selectFragment?.onGaze(gazeInfo, trackingFps)
                        scrollFragment?.onGaze(gazeInfo, trackingFps)
                    }
                }
            }
        }
    }

    private val calibrationCallback: CalibrationCallback = object : CalibrationCallback {
        override fun onCalibrationProgress(progress: Float) {
            setCalibrationProgress(progress)
        }

        override fun onCalibrationNextPoint(x: Float, y: Float) {
            setCalibrationPoint(x, y)

            // Give user time to find the calibration point with eyes, then start collect data
            backgroundHandler?.postDelayed({
                startCollectSamples()
            }, 1000)
        }

        override fun onCalibrationFinished(calibrationData: DoubleArray) {
            hideCalibrationView()
        }
    }

    private val statusCallback: StatusCallback = object : StatusCallback {
        override fun onStarted() {
            // When the camera stream is started (isTracking == true)

        }

        override fun onStopped(error: StatusErrorType) {
            // When the camera stream is stopped (isTracking == false)
            if (error != StatusErrorType.ERROR_NONE) {
                when (error) {
                    StatusErrorType.ERROR_CAMERA_START -> // When the camera stream can't be started
                        showToast("ERROR_CAMERA_START", false)
                    StatusErrorType.ERROR_CAMERA_INTERRUPT -> // When the camera stream is interrupted
                        showToast("ERROR_CAMERA_INTERRUPT", false)
                }
            }
        }
    }


    // Toast function
    private fun showToast(msg: String, isShort: Boolean) {
        runOnUiThread {
            Toast.makeText(
                this,
                msg,
                if (isShort) Toast.LENGTH_SHORT else Toast.LENGTH_LONG
            ).show()
        }
    }
}