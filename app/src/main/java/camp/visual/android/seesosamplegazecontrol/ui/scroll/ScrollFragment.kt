package camp.visual.android.seesosamplegazecontrol.ui.scroll

import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import camp.visual.android.seesosamplegazecontrol.R
import camp.visual.gazetracker.gaze.GazeInfo
import camp.visual.gazetracker.state.TrackingState
import kotlinx.android.synthetic.main.fragment_control_scroll.*

class ScrollFragment : Fragment() {
    // Display Size
    private var screenWidth: Int = 0
    private var screenHeight: Int = 0

    // ROI
    private var roiTopX: IntArray = IntArray(2)
    private var roiTopY: IntArray = IntArray(2)
    private var roiBottomX: IntArray = IntArray(2)
    private var roiBottomY: IntArray = IntArray(2)

    // ROI Height (Related to the screen height)
    private val roiHeightRatio: Float = 0.3f

    // Gaze Area Margin(Related to the screen size)
    private val marginRatio: Float = 0.5f

    // Scroll speed per second
    private var scrollSpeed: Int = 0

    // ROI Visibility
    private var visibilityROI: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_control_scroll, container, false)
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

        // Set scroll Speed
        scrollSpeed = screenHeight/5

        // Resize ROI
        resizeROI()

        // Set Text
        var text: String = ""
        for (i in 0 until 100) {
            text +=  getText(R.string.title_line).toString() + " $i\n"
        }
        text_content?.text = text
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
                if (y < roiTopY[1]) {
                    scroll_view_contents?.smoothScrollBy(0, -scrollSpeed/fps)
                } else if (roiBottomY[0] < y) {
                    scroll_view_contents?.smoothScrollBy(0, scrollSpeed/fps)
                }
            }
        }
    }
}