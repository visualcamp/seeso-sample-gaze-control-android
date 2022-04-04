package camp.visual.android.seesosamplegazecontrol.seeso

import android.content.Context
import camp.visual.gazetracker.callback.InitializationCallback
import camp.visual.gazetracker.callback.GazeCallback
import camp.visual.gazetracker.callback.CalibrationCallback
import camp.visual.gazetracker.callback.StatusCallback
import camp.visual.gazetracker.GazeTracker
import camp.visual.gazetracker.device.GazeDevice
import camp.visual.gazetracker.callback.GazeTrackerCallback
import camp.visual.android.seesosamplegazecontrol.calibration.CalibrationDataStorage
import camp.visual.gazetracker.constant.*
import camp.visual.gazetracker.gaze.GazeInfo
import java.lang.ref.WeakReference
import java.util.ArrayList

class GazeTrackerManager private constructor(context: Context) {
    private val initializationCallbacks: MutableList<InitializationCallback> = ArrayList()
    private val gazeCallbacks: MutableList<GazeCallback> = ArrayList()
    private val calibrationCallbacks: MutableList<CalibrationCallback> = ArrayList()
    private val statusCallbacks: MutableList<StatusCallback> = ArrayList()
    private val mContext: WeakReference<Context> = WeakReference(context)
    private var gazeTracker: GazeTracker? = null

    // TODO: change licence key
    private val licenseKey = "your license key"

    companion object {
        private var instance: GazeTrackerManager? = null
        fun makeNewInstance(context: Context): GazeTrackerManager? {
            if (instance != null) {
                instance!!.deInitGazeTracker()
            }
            instance = GazeTrackerManager(context)
            return instance
        }
    }

    // Check if the GazeTracker is initialized
    fun hasGazeTracker(): Boolean {
        return gazeTracker != null
    }

    // Check if the GazeTracker is tracking
    fun isTracking(): Boolean {
        return if (hasGazeTracker()) {
            gazeTracker!!.isTracking
        } else {
            false
        }
    }

    // Check if the GazeTracker is calibrating
    fun isCalibrating(): Boolean {
        return if (hasGazeTracker()) {
            gazeTracker!!.isCalibrating
        } else {
            false
        }
    }

    // Initialize the GazeTracker
    fun initGazeTracker(callback: InitializationCallback) {
        val gazeDevice = GazeDevice()
        initializationCallbacks.add(callback)
        GazeTracker.initGazeTracker(
            mContext.get(),
            gazeDevice,
            licenseKey,
            initializationCallback,
            UserStatusOption()
        )
    }

    // De-initialize the GazeTracker
    fun deInitGazeTracker() {
        if (hasGazeTracker()) {
            GazeTracker.deinitGazeTracker(gazeTracker)
            gazeTracker = null
        }
    }

    // Set callbacks for the GazeTracker
    fun setGazeTrackerCallbacks(vararg callbacks: GazeTrackerCallback?) {
        for (callback in callbacks) {
            if (callback is GazeCallback) {
                gazeCallbacks.add(callback)
            } else if (callback is CalibrationCallback) {
                calibrationCallbacks.add(callback)
            } else if (callback is StatusCallback) {
                statusCallbacks.add(callback)
            }
        }
    }

    // Remove callbacks for the GazeTracker
    fun removeCallbacks(vararg callbacks: GazeTrackerCallback?) {
        for (callback in callbacks) {
            gazeCallbacks.remove(callback)
            calibrationCallbacks.remove(callback)
            statusCallbacks.remove(callback)
        }
    }

    // Set Gaze Tracking FPS
    fun setGazeTrackingFps(fps: Int): Boolean {
        if (hasGazeTracker()) {
            gazeTracker!!.setTrackingFPS(fps)
        }
        return false
    }

    // Start Gaze Tracking
    fun startGazeTracking(): Boolean {
        if (hasGazeTracker()) {
            gazeTracker!!.startTracking()
            return true
        }
        return false
    }

    // Stop Gaze Tracking
    fun stopGazeTracking(): Boolean {
        if (isTracking()) {
            gazeTracker!!.stopTracking()
            return true
        }
        return false
    }

    // Start Calibration
    fun startCalibration(modeType: CalibrationModeType?, criteria: AccuracyCriteria?): Boolean {
        if (isTracking()) {
            gazeTracker!!.startCalibration(modeType, criteria)
            return true
        } else {
            return false
        }
    }

    // Stop Calibration
    fun stopCalibration(): Boolean {
        if (isCalibrating()) {
            gazeTracker!!.stopCalibration()
            return true
        }
        return false
    }

    // Start Collect calibration sample data
    fun startCollectingCalibrationSamples(): Boolean {
        if (isCalibrating()) {
            gazeTracker!!.startCollectSamples()
            return true
        } else {
            return false
        }
    }

    // GazeTracker Callbacks
    private val initializationCallback =
        InitializationCallback { gazeTracker, initializationErrorType ->
            this.gazeTracker = gazeTracker

            for (initializationCallback in initializationCallbacks) {
                initializationCallback.onInitialized(gazeTracker, initializationErrorType)
            }
            initializationCallbacks.clear()

            gazeTracker?.setTrackingFPS(30)
            gazeTracker?.setCallbacks(
                gazeCallback,
                calibrationCallback,
                statusCallback,
            )
        }

    private val gazeCallback: GazeCallback = object : GazeCallback {
        override fun onGaze(gazeInfo: GazeInfo?) {
            for (gazeCallback in gazeCallbacks) {
                gazeCallback.onGaze(gazeInfo)
            }
        }
    }

    private val calibrationCallback: CalibrationCallback = object : CalibrationCallback {
        override fun onCalibrationProgress(progress: Float) {
            for (calibrationCallback in calibrationCallbacks) {
                calibrationCallback.onCalibrationProgress(progress)
            }
        }

        override fun onCalibrationNextPoint(x: Float, y: Float) {
            for (calibrationCallback in calibrationCallbacks) {
                calibrationCallback.onCalibrationNextPoint(x, y)
            }
        }

        override fun onCalibrationFinished(calibrationData: DoubleArray) {
            CalibrationDataStorage.saveCalibrationData(mContext.get(), calibrationData)
            for (calibrationCallback in calibrationCallbacks) {
                calibrationCallback.onCalibrationFinished(calibrationData)
            }
        }
    }

    private val statusCallback: StatusCallback = object : StatusCallback {
        override fun onStarted() {
            for (statusCallback in statusCallbacks) {
                statusCallback.onStarted()
            }
        }

        override fun onStopped(statusErrorType: StatusErrorType) {
            for (statusCallback in statusCallbacks) {
                statusCallback.onStopped(statusErrorType)
            }
        }
    }
}