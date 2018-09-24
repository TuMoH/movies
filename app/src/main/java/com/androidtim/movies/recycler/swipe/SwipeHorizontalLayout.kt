package com.androidtim.movies.recycler.swipe

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewConfiguration
import android.widget.FrameLayout
import com.androidtim.movies.R

class SwipeHorizontalLayout @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0, defStyleRes: Int = 0
) : FrameLayout(context, attrs, defStyleAttr, defStyleRes) {

    companion object {
        private const val TRANSLATION_DURATION = 200
        private const val TIP_DURATION = 300
        private const val TIP_PAUSE = 400L
        private const val SWIPE_TO_DISMISS_PERCENT = 0.6f
        private const val SWIPE_MENU_OPEN_PERCENT = 0.5f
    }

    private lateinit var contentView: View
    private lateinit var menuView: View

    private val scaledTouchSlop: Int
    private val scaledMinimumFlingVelocity: Int
    private val scaledMaximumFlingVelocity: Int

    private var downX: Int = 0
    private var downY: Int = 0
    private var lastX: Int = 0
    private var lastY: Int = 0

    private var dragging = false
    private var velocityTracker: VelocityTracker? = null
    private var translationAnimator: ValueAnimator? = null

    private var tipHelper: TipHelper? = null

    var swipeCallback: SwipeCallback? = null

    private val isMenuOpen
        get() = Math.abs(contentView.translationX) >= menuView.width

    init {
        val viewConfig = ViewConfiguration.get(context)
        scaledTouchSlop = viewConfig.scaledTouchSlop
        scaledMinimumFlingVelocity = viewConfig.scaledMinimumFlingVelocity
        scaledMaximumFlingVelocity = viewConfig.scaledMaximumFlingVelocity
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        isClickable = true
        contentView = findViewById(R.id.swipe_view_content)
        menuView = findViewById(R.id.swipe_view_menu)
    }

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        if (Math.abs(contentView.translationX) >= contentView.width) {
            //already closed view, ignore new events
            return super.onInterceptTouchEvent(event)
        }
        if (translationAnimator != null && translationAnimator!!.isRunning) {
            return super.onInterceptTouchEvent(event)
        }

        var isIntercepted = super.onInterceptTouchEvent(event)
        val action = event.action
        when (action) {
            MotionEvent.ACTION_DOWN -> {
                lastX = event.x.toInt()
                downX = lastX
                downY = event.y.toInt()
                isIntercepted = false
            }
            MotionEvent.ACTION_MOVE -> {
                val disX = (event.x - downX).toInt()
                val disY = (event.y - downY).toInt()
                isIntercepted = Math.abs(disX) > scaledTouchSlop && Math.abs(disX) > Math.abs(disY)
            }
            MotionEvent.ACTION_UP -> {
                isIntercepted = false
                // menu view opened and click on content view,
                // we just close the menu view and intercept the up event
                if (isMenuOpen && isClickOnContentView(event.x)) {
                    smoothCloseMenu()
                    isIntercepted = true
                }
            }
            MotionEvent.ACTION_CANCEL -> {
                isIntercepted = false
                if (translationAnimator != null && translationAnimator!!.isRunning) {
                    translationAnimator!!.end()
                }
            }
        }
        return isIntercepted
    }

    private fun isClickOnContentView(clickX: Float): Boolean {
        return contentView.translationX > 0 || clickX < contentView.width - menuView.width
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (translationAnimator != null && translationAnimator!!.isRunning) {
            return super.onTouchEvent(event)
        }
        if (Math.abs(contentView.translationX) >= contentView.width) {
            //already closed view, ignore new events
            return super.onTouchEvent(event)
        }

        if (velocityTracker == null) {
            velocityTracker = VelocityTracker.obtain()
        }
        velocityTracker!!.addMovement(event)

        val dx: Int
        val dy: Int
        val action = event.action
        when (action) {
            MotionEvent.ACTION_DOWN -> {
                lastX = event.x.toInt()
                lastY = event.y.toInt()
            }
            MotionEvent.ACTION_MOVE -> {
                val disX = (lastX - event.x).toInt()
                val disY = (lastY - event.y).toInt()
                if (!dragging
                        && Math.abs(disX) > scaledTouchSlop
                        && Math.abs(disX) > Math.abs(disY)) {
                    val parent = parent
                    parent?.requestDisallowInterceptTouchEvent(true)
                    dragging = true
                }
                if (dragging) {
                    val translationX = contentView.translationX - disX
                    if (translationX < 0 && Math.abs(translationX) <= contentView.width) {
                        //user swipes from right to left, we show context menu
                        showMenuView()
                        contentView.translationX = translationX
                        swipeCallback?.onSwipeChanged(translationX.toInt())
                    }
                    lastX = event.x.toInt()
                    lastY = event.y.toInt()
                    tipHelper?.cancel(true)
                }
            }
            MotionEvent.ACTION_UP -> {
                val parent = parent
                parent?.requestDisallowInterceptTouchEvent(false)
                dx = (downX - event.x).toInt()
                dy = (downY - event.y).toInt()
                dragging = false
                velocityTracker!!.computeCurrentVelocity(1000, scaledMaximumFlingVelocity.toFloat())
                val velocityX = velocityTracker!!.xVelocity.toInt()
                val velocity = Math.abs(velocityX)
                if (velocity > scaledMinimumFlingVelocity) {
                    //swipe to dismiss (only if swipe is long enough)
                    if (Math.abs(contentView.translationX) > menuView.width
                            && velocity > scaledMinimumFlingVelocity * 3) {
                        val duration = getSwipeFinishDuration(event, velocity, contentView.width)
                        smoothSwipeOutItem(duration)
                    } else {
                        //swipe to to show context menu action
                        val duration = getSwipeFinishDuration(event, velocity, menuView.width)
                        if (velocityX < 0) {
                            smoothOpenMenu(duration)
                        } else {
                            smoothCloseMenu(duration)
                        }
                    }
                } else {
                    //swipe is slow, just judge what to do with swiped content
                    judgeOpenClose(dx)
                }
                velocityTracker!!.clear()
                velocityTracker!!.recycle()
                velocityTracker = null
                if (Math.abs(dx) > scaledTouchSlop
                        || Math.abs(dy) > scaledTouchSlop
                        || isMenuOpen) { // ignore click listener, cancel this event
                    val motionEvent = MotionEvent.obtain(event)
                    motionEvent.action = MotionEvent.ACTION_CANCEL
                    return super.onTouchEvent(motionEvent)
                }
            }
            MotionEvent.ACTION_CANCEL -> {
                dragging = false
                if (translationAnimator != null && translationAnimator!!.isRunning) {
                    translationAnimator!!.end()
                } else {
                    dx = (downX - event.x).toInt()
                    judgeOpenClose(dx)
                }
            }
        }
        return super.onTouchEvent(event)
    }

    private fun hideMenuView() {
        menuView.visibility = View.GONE
    }

    private fun showMenuView() {
        menuView.visibility = View.VISIBLE
    }

    private fun smoothSwipeOutItem() {
        smoothSwipeOutItem(TRANSLATION_DURATION)
    }

    private fun smoothSwipeOutItem(duration: Int) {
        translateContentView(duration, contentView.translationX, (-contentView.width).toFloat()) {
            swipeCallback?.onSwipedOut()
        }
    }

    private fun smoothSwipeBackItem() {
        smoothSwipeBackItem(TRANSLATION_DURATION)
    }

    private fun smoothSwipeBackItem(duration: Int) {
        translateContentView(duration, contentView.translationX, 0f) { hideMenuView() }
    }

    private fun judgeOpenClose(dx: Int) {
        if (Math.abs(dx) < scaledTouchSlop) {
            return
        }
        val absTranslation = Math.abs(contentView.translationX)
        when {
            absTranslation >= contentView.width * SWIPE_TO_DISMISS_PERCENT -> smoothSwipeOutItem()
            absTranslation >= menuView.width * SWIPE_MENU_OPEN_PERCENT -> smoothOpenMenu()
            else -> smoothSwipeBackItem()
        }
    }

    private fun translateContentView(duration: Int, from: Float, to: Float, endAction: (() -> Unit)?) {
        if (translationAnimator != null && translationAnimator!!.isRunning) {
            translationAnimator!!.end()
        }

        translationAnimator = ValueAnimator.ofFloat(from, to)

        translationAnimator!!.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                translationAnimator = null
                endAction?.invoke()
            }
        })
        translationAnimator!!.addUpdateListener { animation ->
            val translationX = animation.animatedValue as Float
            contentView.translationX = translationX
            swipeCallback?.onSwipeChanged(translationX.toInt())
        }
        translationAnimator!!.setDuration(duration.toLong()).start()
    }

    private fun getMoveLen(event: MotionEvent): Int {
        val translationX = contentView.translationX
        return (event.x - translationX).toInt()
    }

    private fun smoothOpenMenu() {
        smoothOpenMenu(TRANSLATION_DURATION)
    }

    private fun smoothCloseMenu() {
        smoothCloseMenu(TRANSLATION_DURATION)
    }

    private fun smoothOpenMenu(duration: Int) {
        showMenuView()
        translateContentView(duration, contentView.translationX, (-menuView.width).toFloat()) {
            swipeCallback?.onMenuOpened()
        }
    }

    private fun smoothCloseMenu(duration: Int) {
        showMenuView()
        translateContentView(duration, contentView.translationX, 0f) {
            swipeCallback?.onMenuClosed()
        }
    }

    /**
     * compute finish duration
     *
     * @param ev       up event
     * @param velocity velocity
     * @param len      full length
     * @return finish duration
     */
    private fun getSwipeFinishDuration(ev: MotionEvent, velocity: Int, len: Int): Int {
        val moveLen = getMoveLen(ev)
        val halfLen = len / 2
        val distanceRatio = Math.min(1f, 1.0f * Math.abs(moveLen) / len)
        val distance = halfLen + halfLen * distanceInfluenceForSnapDuration(distanceRatio)
        val duration = if (velocity > 0) {
            4 * Math.round(1000 * Math.abs(distance / velocity))
        } else {
            val pageDelta = Math.abs(moveLen).toFloat() / len
            ((pageDelta + 1) * 100).toInt()
        }
        return Math.min(duration, TRANSLATION_DURATION)
    }

    private fun distanceInfluenceForSnapDuration(f: Float): Float {
        val c = f - 0.5 // center the values about 0.
        return Math.sin(c * 0.3 * Math.PI / 2.0).toFloat()
    }

    fun openMenu() {
        smoothOpenMenu(0)
    }

    fun closeMenu() {
        smoothCloseMenu(0)
    }

    fun checkAndShowTip() {
        tipHelper = TipHelper(context.applicationContext)
        tipHelper?.checkAndShowDelayedTip {
            showMenuView()
            translateContentView(TIP_DURATION, contentView.translationX,
                    (-menuView.width).toFloat() / 2) {
                postDelayed({ smoothSwipeBackItem(TIP_DURATION) }, TIP_PAUSE)
            }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        tipHelper?.cancel()
    }

    interface SwipeCallback {
        fun onSwipeChanged(translationX: Int)
        fun onMenuOpened()
        fun onMenuClosed()
        fun onSwipedOut()
    }

}
