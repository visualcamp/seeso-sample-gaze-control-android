package camp.visual.android.seesosamplegazecontrol.ui.button

import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import camp.visual.gazetracker.gaze.GazeInfo
import camp.visual.android.seesosamplegazecontrol.R
import camp.visual.gazetracker.state.TrackingState
import kotlinx.android.synthetic.main.fragment_control_button.*
import kotlinx.android.synthetic.main.fragment_control_button.btn_next
import kotlinx.android.synthetic.main.fragment_control_button.btn_previous
import kotlinx.android.synthetic.main.fragment_control_button.progress_next
import kotlinx.android.synthetic.main.fragment_control_button.progress_previous

class ButtonFragment : Fragment() {
    // Display Size
    private var screenWidth: Int = 0
    private var screenHeight: Int = 0

    // ROI
    private var roiPrevX: IntArray = IntArray(2)
    private var roiPrevY: IntArray = IntArray(2)
    private var roiNextX: IntArray = IntArray(2)
    private var roiNextY: IntArray = IntArray(2)

    // Button Area Margin(Related to the button size)
    private val marginRatio: Float = 0.5f

    // Button Progress
    private var progressBtnArray: Array<Float> = arrayOf(0.0f, 0.0f)

    // Button trigger/recover speed per second
    private var triggerSpeed: Float = 0.0f
    private var recoverSpeed: Float = 0.0f

    // ROI Visibility
    private var visibilityROI: Boolean = false

    // Page
    private val indexPageMin: Int = 0
    private val indexPageMax: Int = 5
    private var indexPage: Int = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_control_button, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set Screen Size
        val displayMetrics = DisplayMetrics()
        if (activity != null) {
            activity!!.windowManager.defaultDisplay.getMetrics(displayMetrics)
            screenWidth = displayMetrics.widthPixels
            screenHeight = displayMetrics.heightPixels
        }

        // Set button Speed
        triggerSpeed = 60.0f
        recoverSpeed = 70.0f

        // Resize ROI
        resizeROI()

        // Page Setting
        setPageIndex(0)
    }

    // Resize ROI
    private fun resizeROI() {
        // ROI previous
        if (btn_previous != null) {
            roiPrevX[0] = (-btn_previous!!.layoutParams.width * marginRatio).toInt()
            roiPrevX[1] = (roiPrevX[0] + (btn_previous!!.layoutParams.width + (btn_previous!!.layoutParams.width * marginRatio * 2))).toInt()
            roiPrevY[0] = (-btn_previous!!.layoutParams.height * marginRatio).toInt()
            roiPrevY[1] = (roiPrevY[0] + (btn_previous!!.layoutParams.height + (btn_previous!!.layoutParams.height * marginRatio * 2))).toInt()

            val layoutParam = roi_previous?.layoutParams
            roi_previous?.x = roiPrevX[0].toFloat()
            roi_previous?.y = roiPrevY[0].toFloat()
            layoutParam?.width = roiPrevX[1] - roiPrevX[0]
            layoutParam?.height = roiPrevY[1] - roiPrevY[0]

            roi_previous?.layoutParams = layoutParam
            roi_previous?.invalidate()
        }

        // ROI next
        if (btn_next != null) {
            roiNextX[0] = (screenWidth - btn_previous!!.layoutParams.width - (btn_previous!!.layoutParams.width * marginRatio)).toInt()
            roiNextX[1] = (roiNextX[0] + (btn_next!!.layoutParams.width + (btn_next!!.layoutParams.width * marginRatio * 2))).toInt()
            roiNextY[0] = (-btn_next!!.layoutParams.height * marginRatio).toInt()
            roiNextY[1] = (roiNextY[0] + (btn_next!!.layoutParams.height + (btn_next!!.layoutParams.height * marginRatio * 2))).toInt()

            val layoutParam = roi_next?.layoutParams
            roi_next?.x = roiNextX[0].toFloat()
            roi_next?.y = roiNextY[0].toFloat()
            layoutParam?.width = roiNextX[1] - roiNextX[0]
            layoutParam?.height = roiNextY[1] - roiNextY[0]

            roi_next?.layoutParams = layoutParam
            roi_next?.invalidate()
        }
    }

    // Page Index
    private fun setPageIndex(index: Int) {
        if (index < indexPageMin || indexPageMax < index) {
            return
        }

        indexPage = index

        activity?.runOnUiThread {
            text_page?.text = getText(R.string.title_page).toString() + " $index"

            if (indexPageMin >= index) {
                btn_previous?.visibility = View.INVISIBLE
            } else {
                btn_previous?.visibility = View.VISIBLE
            }

            if (index >= indexPageMax) {
                btn_next?.visibility = View.INVISIBLE
            } else {
                btn_next?.visibility = View.VISIBLE
            }
        }
    }

    // Button Progress
    private fun progressButton(id: Int, isGazed: Boolean, fps: Int) {
        val addValue: Float = triggerSpeed/fps
        val subValue: Float = recoverSpeed/fps

        if (isGazed) {
            if (progressBtnArray[id] < 100) {
                progressBtnArray[id] += addValue
            } else {
                progressBtnArray[id] = 0.0f
                if (id == 0) {
                    setPageIndex(--indexPage)
                } else if (id == 1) {
                    setPageIndex(++indexPage)
                }
            }
        } else {
            if (progressBtnArray[id] > 0) {
                progressBtnArray[id] -= subValue
            } else {
                progressBtnArray[id] = 0.0f
            }
        }
    }

    // Update ROI
    fun updateROI() {
        visibilityROI = !visibilityROI

        // ROI Visibility
        if (visibilityROI) {
            roi_previous?.visibility = View.VISIBLE
            roi_next?.visibility = View.VISIBLE
        } else {
            roi_previous?.visibility = View.INVISIBLE
            roi_next?.visibility = View.INVISIBLE
        }
    }

    // Gaze Data
    fun onGaze(gazeInfo: GazeInfo, fps: Int) {
        val x: Float = gazeInfo.x
        val y: Float = gazeInfo.y

        // Gaze Validation
        val isValidGazeInfo: Boolean =
            gazeInfo.trackingState == TrackingState.SUCCESS ||
            gazeInfo.trackingState == TrackingState.LOW_CONFIDENCE

        // ROI Events
        if (btn_previous != null && indexPageMin < indexPage) {
            if (roiPrevX[0] <= x && x <= roiPrevX[1] &&
                roiPrevY[0] <= y && y <= roiPrevY[1]) {
                if (isValidGazeInfo) {
                    progressButton(0, true, fps)
                } else {
                    progressButton(0, false, fps)
                }
            } else {
                progressButton(0, false, fps)
            }
        }

        if (btn_next != null && indexPage < indexPageMax) {
            if (roiNextX[0] <= x && x <= roiNextX[1] &&
                roiNextY[0] <= y && y <=  roiNextY[1]) {
                if (isValidGazeInfo) {
                    progressButton(1, true, fps)
                } else {
                    progressButton(1, false, fps)
                }
            } else {
                progressButton(1, false, fps)
            }
        }

        // Update Progress UI
        activity?.runOnUiThread {
            progress_previous?.progress = progressBtnArray[0].toInt()
            progress_next?.progress = progressBtnArray[1].toInt()
        }
    }
}