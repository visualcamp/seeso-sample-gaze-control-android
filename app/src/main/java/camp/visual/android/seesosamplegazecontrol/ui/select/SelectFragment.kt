package camp.visual.android.seesosamplegazecontrol.ui.select

import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import camp.visual.android.seesosamplegazecontrol.R
import camp.visual.gazetracker.gaze.GazeInfo
import kotlinx.android.synthetic.main.fragment_control_select.*
import camp.visual.gazetracker.state.TrackingState
import kotlinx.android.synthetic.main.fragment_control_select.progress_next
import kotlinx.android.synthetic.main.fragment_control_select.progress_previous
import kotlinx.android.synthetic.main.fragment_control_select.roi_bottom
import kotlinx.android.synthetic.main.fragment_control_select.roi_top

class SelectFragment : Fragment() {
    // Display Size
    private var screenWidth: Int = 0
    private var screenHeight: Int = 0

    // ROI
    private var roiTopX: IntArray = IntArray(2)
    private var roiTopY: IntArray = IntArray(2)
    private var roiBottomX: IntArray = IntArray(2)
    private var roiBottomY: IntArray = IntArray(2)

    // Gaze Area Margin(Related to the screen size)
    private val marginRatio: Float = 0.5f

    // ROI Height (Related to the screen height)
    private val roiHeightRatio: Float = 0.3f

    // Button Progress
    private var progressBtnArray: Array<Float> = arrayOf(0.0f, 0.0f)

    // Button trigger/recover speed per second
    private var triggerSpeed: Float = 0.0f
    private var recoverSpeed: Float = 0.0f

    // ROI Visibility
    private var visibilityROI: Boolean = false

    // Item
    private val indexItemMin: Int = 0
    private val indexItemMax: Int = 10
    private var indexItem: Int = 0
    private var itemListArray: ArrayList<String> = ArrayList<String>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_control_select, container, false)
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

        // Item List
        for (i in indexItemMin..indexItemMax) {
            itemListArray.add(getString(R.string.title_item) + " $i")
        }

        // Item Setting
        setItemIndex(0)
    }

    // Resize ROI
    private fun resizeROI() {
        // ROI top
        roiTopX[0] = (-screenWidth * marginRatio).toInt()
        roiTopX[1] = roiTopX[0] + (screenWidth + (screenWidth * marginRatio * 2)).toInt()
        roiTopY[0] = (-screenHeight * marginRatio).toInt()
        roiTopY[1] = roiTopY[0] + (screenHeight * roiHeightRatio + (screenHeight * marginRatio)).toInt()

        val layoutParamTop = roi_top?.layoutParams
        roi_top?.x = roiTopX[0].toFloat()
        roi_top?.y = roiTopY[0].toFloat()
        layoutParamTop?.width = roiTopX[1] - roiTopX[0]
        layoutParamTop?.height = roiTopY[1] - roiTopY[0]

        roi_top?.layoutParams = layoutParamTop
        roi_top?.invalidate()

        // ROI bottom
        roiBottomX[0] = (-screenWidth * marginRatio).toInt()
        roiBottomX[1] = roiBottomX[0] + (screenWidth + (screenWidth * marginRatio * 2)).toInt()
        roiBottomY[0] = (screenHeight - (screenHeight * roiHeightRatio)).toInt()
        roiBottomY[1] = roiBottomY[0] + (screenHeight * roiHeightRatio + (screenHeight * marginRatio)).toInt()

        val layoutParamBottom = roi_bottom?.layoutParams
        roi_bottom?.x = roiBottomX[0].toFloat()
        roi_bottom?.y = roiBottomY[0].toFloat()
        layoutParamBottom?.width = roiBottomX[1] - roiBottomX[0]
        layoutParamBottom?.height = roiBottomY[1] - roiBottomY[0]

        roi_bottom?.layoutParams = layoutParamBottom
        roi_bottom?.invalidate()
    }

    // Item Index
    private fun setItemIndex(index: Int) {
        if (index < indexItemMin || indexItemMax < index) {
            return
        }

        indexItem = index

        activity?.runOnUiThread {
            // Previous Item Text
            if (index > indexItemMin + 1) {
                text_item_previous_2?.text = itemListArray[index-2]
            } else {
                text_item_previous_2?.text = ""
            }

            if (index > indexItemMin) {
                text_item_previous_1?.text = itemListArray[index-1]
            } else {
                text_item_previous_1?.text = ""
            }

            // Next Item Text
            if (index < indexItemMax - 1) {
                text_item_next_2?.text = itemListArray[index+2]
            } else {
                text_item_next_2?.text = ""
            }

            if (index < indexItemMax) {
                text_item_next_1?.text = itemListArray[index+1]
            } else {
                text_item_next_1?.text = ""
            }

            text_item_current?.text = itemListArray[index]

            // Button Visibility
            if (indexItemMin >= index) {
                btn_previous?.visibility = View.INVISIBLE
            } else {
                btn_previous?.visibility = View.VISIBLE
            }

            if (index >= indexItemMax) {
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
                    setItemIndex(--indexItem)
                } else if (id == 1) {
                    setItemIndex(++indexItem)
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
            roi_top?.visibility = View.VISIBLE
            roi_bottom?.visibility = View.VISIBLE
        } else {
            roi_top?.visibility = View.INVISIBLE
            roi_bottom?.visibility = View.INVISIBLE
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
        if (roiTopX[0] <= x && x <= roiTopX[1]) {
            if (isValidGazeInfo) {
                if (y < roiTopY[1] && indexItemMin < indexItem) {
                    progressButton(0, true, fps)
                } else {
                    progressButton(0, false, fps)
                }

                if (roiBottomY[0] < y && indexItem < indexItemMax) {
                    progressButton(1, true, fps)
                } else {
                    progressButton(1, false, fps)
                }
            }
        }

        // Update Progress UI
        activity?.runOnUiThread {
            progress_previous?.progress = progressBtnArray[0].toInt()
            progress_next?.progress = progressBtnArray[1].toInt()
        }
    }
}