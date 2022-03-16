package com.ndhzs.bottomsheettest

import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.animation.addListener
import androidx.core.view.ViewCompat
import androidx.core.view.marginTop
import androidx.recyclerview.widget.RecyclerView
import java.lang.ref.WeakReference
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.pow

/**
 * ...
 * @author 985892345 (Guo Xiangrui)
 * @email 2767465918@qq.com
 * @date 2022/3/14 23:18
 */
class MyBottomSheetBehavior<V : View> : CoordinatorLayout.Behavior<V> {

    private val mContext: Context

    constructor(context: Context) : super() {
        mContext = context
        mTouchSlop = ViewConfiguration.get(context).scaledTouchSlop
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        mContext = context
        val ty = context.obtainStyledAttributes(attrs, R.styleable.MyBottomSheetBehavior_layout)
        setExpandedOffset(
            ty.getDimensionPixelSize(
                R.styleable.MyBottomSheetBehavior_layout_bottomSheet_expandedOffset,

                0
            )
        )
        setPeekHeight(
            ty.getDimensionPixelSize(
                R.styleable.MyBottomSheetBehavior_layout_bottomSheet_peekHeight,
                0
            )
        )
        ty.recycle()
        mTouchSlop = ViewConfiguration.get(context).scaledTouchSlop
    }

    private var mViewRef: WeakReference<View>? = null

    private var mExpandedOffset = 0
    private var mPeekHeight = 0

    private var mIsExistTouchMove = false // 是否存在手指移动
    private var mIsInAnim = false
    private var mIsInNested = false
    private var mIsTouchInChild = false

    private var mInitialX = 0
    private var mInitialY = 0
    private var mLastMoveX = 0
    private var mLastMoveY = 0
    private var mDiffMoveX = 0
    private var mDiffMoveY = 0
    private val mTouchSlop: Int

    private var mLastTouchTime = 0L
    private var mVelocityY = 0F

    override fun onInterceptTouchEvent(
        parent: CoordinatorLayout,
        child: V,
        ev: MotionEvent
    ): Boolean {
        val x = ev.x.toInt()
        val y = ev.y.toInt()
        when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                mInitialX = x
                mInitialY = y
                mLastMoveX = x
                mLastMoveY = y

                mIsExistTouchMove = false // 还原
                mIsInNested = false // 还原

                mLastTouchTime = System.currentTimeMillis()

                val childLeft = child.x.toInt()
                val childRight = childLeft + child.width
                val childTop = child.y.toInt()
                val childBottom = childTop + child.height
                mIsTouchInChild = !(x !in childLeft..childRight || y !in childTop..childBottom)
            }
            MotionEvent.ACTION_MOVE -> {
                mDiffMoveX = mLastMoveX - x
                mDiffMoveY = mLastMoveY - y

                if (!mIsInNested) {
                    val nowTouchTime = System.currentTimeMillis()
                    mVelocityY = mDiffMoveY / ((nowTouchTime - mLastTouchTime) / 1000F)
                    mLastTouchTime = nowTouchTime
                }

                mLastMoveX = x
                mLastMoveY = y

                // 这里只拦截没有开启嵌套滑动的区域
                if (!mIsInNested && mIsTouchInChild) {
                    if (abs(x - mInitialX) > mTouchSlop
                        || abs(y - mInitialY) > mTouchSlop
                    ) {
                        // 这里只要超过阈值就直接拦截
                        return true
                    }
                }
            }
        }
        return false
    }

    override fun onTouchEvent(
        parent: CoordinatorLayout,
        child: V,
        ev: MotionEvent
    ): Boolean {
        val x = ev.x.toInt()
        val y = ev.y.toInt()
        when (ev.action) {
            MotionEvent.ACTION_MOVE -> {
                mIsExistTouchMove = true // 开始

                if (!mIsTouchInChild) return false

                mDiffMoveX = mLastMoveX - x
                mDiffMoveY = mLastMoveY - y

                val nowTouchTime = System.currentTimeMillis()
                mVelocityY = mDiffMoveY / ((nowTouchTime - mLastTouchTime) / 1000F)
                mLastTouchTime = nowTouchTime

                val oldY = getCurrY(child)
                setCurrY(child, oldY - mDiffMoveY)

                mLastMoveX = x
                mLastMoveY = y
            }
            MotionEvent.ACTION_UP -> {
                slideOver(child)
            }
        }
        return true
    }

    override fun onMeasureChild(
        parent: CoordinatorLayout,
        child: V,
        parentWidthMeasureSpec: Int,
        widthUsed: Int,
        parentHeightMeasureSpec: Int,
        heightUsed: Int
    ): Boolean {
        parent.onMeasureChild(
            child,
            parentWidthMeasureSpec,
            widthUsed,
            parentHeightMeasureSpec,
            heightUsed + getExpandedOffset()
        )
        return true
    }

    override fun onLayoutChild(
        parent: CoordinatorLayout,
        child: V,
        layoutDirection: Int
    ): Boolean {
        if (child !== mViewRef?.get()) {
            mViewRef = WeakReference(child)
        }
        child.translationY = getMaxY(child).toFloat()
        return super.onLayoutChild(parent, child, layoutDirection)
    }

    override fun onStartNestedScroll(
        coordinatorLayout: CoordinatorLayout,
        child: V,
        directTargetChild: View,
        target: View,
        axes: Int,
        type: Int
    ): Boolean {
        return axes == ViewCompat.SCROLL_AXIS_VERTICAL
    }

    override fun onNestedScrollAccepted(
        coordinatorLayout: CoordinatorLayout,
        child: V,
        directTargetChild: View,
        target: View,
        axes: Int,
        type: Int
    ) {
        if (type == ViewCompat.TYPE_TOUCH) {
            mIsExistTouchMove = false // 还原
            mIsInNested = true
        }
    }

    override fun onNestedPreScroll(
        coordinatorLayout: CoordinatorLayout,
        child: V,
        target: View,
        dx: Int,
        dy: Int,
        consumed: IntArray,
        type: Int
    ) {
        val nowTouchTime = System.currentTimeMillis()
        mVelocityY = dy / ((nowTouchTime - mLastTouchTime) / 1000F)
        mLastTouchTime = nowTouchTime
        mIsExistTouchMove = true

        if (dy > 0) {
            preSlideUp(child, target, dy, consumed, type) // 向上滑
        } else if (dy < 0) {
            preSlideDown(child, target, dy, consumed, type) // 向下滑
        }
    }

    private fun preSlideUp(child: V, target: View, dy: Int, consumed: IntArray, type: Int) {
        val oldY = getCurrY(child) // 当前位置
        val newY = oldY - dy // 将要移到的位置
        val minY = getMinY(child) // 上限
        when (type) {
            ViewCompat.TYPE_TOUCH -> {
                if (oldY > minY) {
                    // 将要滑到的位置超过了滑动范围
                    if (newY < minY) {
                        setCurrY(child, minY)
                        consumed[1] = oldY - newY
                    } else {
                        setCurrY(child, newY)
                        consumed[1] = dy
                    }
                }
            }
            ViewCompat.TYPE_NON_TOUCH -> {
                if (oldY > minY) {
                    consumed[1] = dy
                }
            }
        }
    }

    private fun preSlideDown(child: V, target: View, dy: Int, consumed: IntArray, type: Int) {
        // nothing
    }

    override fun onNestedScroll(
        coordinatorLayout: CoordinatorLayout,
        child: V,
        target: View,
        dxConsumed: Int,
        dyConsumed: Int,
        dxUnconsumed: Int,
        dyUnconsumed: Int,
        type: Int,
        consumed: IntArray
    ) {
        if (dyUnconsumed > 0) { // 向上滑, 此时一定处于 RecyclerView 底部
            unconsumedSlideUp(child, target, dyConsumed, dyUnconsumed, type, consumed)
        } else if (dyUnconsumed < 0) { // 向下滑, 此时一定处于 RecyclerView 顶部
            unconsumedSlideDown(child, target, dyConsumed, dyUnconsumed, type, consumed)
        }
    }

    private fun unconsumedSlideUp(
        child: V,
        target: View,
        dyConsumed: Int,
        dyUnconsumed: Int,
        type: Int,
        consumed: IntArray
    ) {
        val oldY = getCurrY(child) // 当前位置
        val newY = oldY - dyUnconsumed // 将要移到的位置
        val minY = getMinY(child) // 上限
        when (type) {
            ViewCompat.TYPE_TOUCH -> {
                if (oldY > minY) {
                    if (newY < minY) {
                        setCurrY(child, minY)
                        consumed[1] = oldY - minY
                    } else {
                        setCurrY(child, newY)
                        consumed[1] = dyUnconsumed
                    }
                }
            }
        }
    }

    private fun unconsumedSlideDown(
        child: V,
        target: View,
        dyConsumed: Int,
        dyUnconsumed: Int,
        type: Int,
        consumed: IntArray
    ) {
        val oldY = getCurrY(child) // 当前位置
        val newY = oldY - dyUnconsumed // 将要移到的位置
        val maxY = getMaxY(child) // 下限
        when (type) {
            ViewCompat.TYPE_TOUCH -> {
                if (oldY < maxY) {
                    // 将要滑到的位置超过了滑动范围
                    if (newY > maxY) {
                        setCurrY(child, maxY)
                        consumed[1] = oldY - maxY
                    } else {
                        setCurrY(child, newY)
                        consumed[1] = dyUnconsumed
                    }
                }
            }
            ViewCompat.TYPE_NON_TOUCH -> {
                slideOver(child)
            }
        }
    }

    override fun onStopNestedScroll(
        coordinatorLayout: CoordinatorLayout,
        child: V,
        target: View,
        type: Int
    ) {
        if (type == ViewCompat.TYPE_TOUCH) {
            val currY = getCurrY(child)
            val minY = getMinY(child)
            if (currY != minY) {
                slideOver(child)
            } else {
                if (!target.canScrollVertically(-1)) {
                    // 展开状态只有 target 滑到顶部时才加载动画
                    slideOver(child)
                }
            }
        }
    }

    // 滑动彻底结束时调用(滑动彻底结束并不一定就是手指抬起, 因为可能存在惯性滑动)
    private fun slideOver(child: V) {
        // 这个必须放到最前面, 此值用于手指没有移动就结束
        if (!mIsExistTouchMove) return
        if (mIsInAnim) return

        val oldY = getCurrY(child)
        val minY = getMinY(child)
        val maxY = getMaxY(child)

        val halfY = (minY + maxY) / 2

        // 判断为可以展开或者折叠的最小速度
        val minFlingVelocity = ViewConfiguration.get(mContext).scaledMaximumFlingVelocity * 0.2F

        if (mVelocityY < -minFlingVelocity && oldY != maxY) {
            slowlyAnimate(oldY, maxY) { setCurrY(child, it) }
        } else if (mVelocityY > minFlingVelocity && oldY != minY) {
            slowlyAnimate(oldY, minY) { setCurrY(child, it) }
        } else {
            if (oldY == minY || oldY == maxY) return
            if (oldY < halfY) {
                slowlyAnimate(oldY, minY) { setCurrY(child, it) }
            } else {
                slowlyAnimate(oldY, maxY) { setCurrY(child, it) }
            }
        }
    }

    fun setExpandedOffset(offset: Int) {
        require(offset >= 0) { "offset 大于或等于 0" }
        mExpandedOffset = offset
        mViewRef?.get()?.requestLayout()
    }

    fun getExpandedOffset(): Int {
        return mExpandedOffset
    }

    fun setPeekHeight(peekHeight: Int) {
        require(peekHeight >= 0) { "peekHeight 大于或等于 0" }
        mPeekHeight = peekHeight
    }

    fun getPeekHeight(): Int {
        return mPeekHeight
    }

    /**
     * 当前 Y 值
     */
    private fun getCurrY(view: View): Int {
        return view.y.toInt()
    }

    /**
     * 上限值
     */
    private fun getMinY(view: View): Int {
        val coordinatorLayout = view.parent as CoordinatorLayout
        return getExpandedOffset() + coordinatorLayout.paddingTop + view.marginTop
    }

    /**
     * 下限值
     */
    private fun getMaxY(view: View): Int {
        val coordinatorLayout = view.parent as CoordinatorLayout
        return coordinatorLayout.height - coordinatorLayout.paddingBottom - getPeekHeight() + view.marginTop
    }

    /**
     * 设置当前 Y 值，相当于进行移动
     */
    private fun setCurrY(view: View, newCurrY: Int) {
        view.y = newCurrY.toFloat()
    }

    private var mInterpolator = AccelerateDecelerateInterpolator()
    private var mSlowlyMoveAnimate: ValueAnimator? = null
    private fun slowlyAnimate(
        oldY: Int,
        newY: Int,
        onEnd: (() -> Unit)? = null,
        onCancel: (() -> Unit)? = null,
        onMove: (nowY: Int) -> Unit
    ) {
        mSlowlyMoveAnimate?.cancel()
        mSlowlyMoveAnimate = ValueAnimator.ofInt(oldY, newY)
        mSlowlyMoveAnimate?.run {
            addUpdateListener {
                val nowY = animatedValue as Int
                onMove.invoke(nowY)
            }
            addListener(
                onStart = {
                    mIsInAnim = true
                },
                onEnd = {
                    mIsInAnim = false
                    onEnd?.invoke()
                    mSlowlyMoveAnimate = null
                },
                onCancel = {
                    mIsInAnim = false
                    onCancel?.invoke()
                    mSlowlyMoveAnimate = null
                }
            )
            interpolator = mInterpolator
            duration = (abs(oldY - newY).toFloat().pow(0.6F) + 100).toLong()
            start()
        }
    }
}