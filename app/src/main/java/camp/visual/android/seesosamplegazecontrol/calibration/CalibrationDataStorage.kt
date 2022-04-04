package camp.visual.android.seesosamplegazecontrol.calibration

import android.content.Context
import android.util.Log
import java.lang.Exception
import java.util.*

object CalibrationDataStorage {
    private const val tag: String = "SeeSo"
    private const val CALIBRATION_DATA = "calibrationData"

    // Store calibration data to SharedPreference
    fun saveCalibrationData(context: Context?, calibrationData: DoubleArray?) {
        if (calibrationData != null && calibrationData.isNotEmpty()) {
            val editor = context?.getSharedPreferences(tag, Context.MODE_PRIVATE)?.edit()
            editor?.putString(CALIBRATION_DATA, Arrays.toString(calibrationData))
            editor?.apply()
        } else {
            Log.e(tag, "Abnormal calibration data")
        }
    }

    // Get calibration data from SharedPreference
    fun loadCalibrationData(context: Context): DoubleArray? {
        val prefs = context.getSharedPreferences(tag, Context.MODE_PRIVATE)
        val saveData = prefs.getString(CALIBRATION_DATA, null)
        if (saveData != null) {
            try {
                val split =
                    saveData.substring(1, saveData.length - 1).split(", ".toRegex()).toTypedArray()
                val array = DoubleArray(split.size)
                for (i in split.indices) {
                    array[i] = split[i].toDouble()
                }
                return array
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e(tag, "Wrong type of calibration data")
            }
        }
        return null
    }
}