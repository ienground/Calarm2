package zone.ien.calarm.utils

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.view.ViewGroup
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AnimationUtils
import android.widget.RelativeLayout
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textview.MaterialTextView
import zone.ien.calarm.R
import zone.ien.calarm.activity.TAG
import zone.ien.calarm.callback.SwipeAnimationListener


class SwipeAnimationButton: RelativeLayout {
    companion object {
        const val RIGHT = true
        const val LEFT = false
    }

    private var isButtonClicked = false
    private var swipeAnimationListener: SwipeAnimationListener? = null
    private lateinit var background: RelativeLayout
    private lateinit var backgroundCard: MaterialCardView

    private lateinit var slidingButton: MaterialButton
    private var initialX = 0f
    private var isActive = false
    private var initialButtonWidth = 0
    private var initialButtonHeight = 0

    private lateinit var leftSwipeTextView: MaterialTextView
    private lateinit var rightSwipeTextView: MaterialTextView

    private var backgroundCardColor: Int = 0
    private var defaultDrawable: Drawable? = null
    private var defaultBackgroundColor: Int = 0
    private var rightSwipeDrawable: Drawable? = null
    private var rightSwipeBackgroundColor: Int = 0
    private var leftSwipeDrawable: Drawable? = null
    private var leftSwipeBackgroundColor: Int = 0
    private var currentDrawable: Drawable? = null
    private var leftSwipeText: String = ""
    private var rightSwipeText: String = ""
    private var fontType: Typeface? = null
    private var textSize: Float = 12f
    private var isLeftSwipeEnabled = true
    private var isRightSwipeEnabled = true

    private var duration = 0L

    constructor(context: Context): super(context) {
        init(context, null, -1, -1)
    }

    constructor(context: Context, attrs: AttributeSet): super(context, attrs) {
        init(context, attrs, -1, -1)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int): super(context, attrs, defStyleAttr) {
        init(context, attrs,defStyleAttr, -1)
    }

    fun setOnSwipeAnimationListener(swipeAnimationListener: SwipeAnimationListener) {
        this.swipeAnimationListener = swipeAnimationListener
    }

    fun setLeftSwipeText(swipeText: String) {
        this.leftSwipeText = swipeText
        leftSwipeTextView.text = swipeText
    }

    fun setRightSwipeText(swipeText: String) {
        this.rightSwipeText = swipeText
        rightSwipeTextView.text = swipeText
    }

    fun setLeftSwipeEnabled(isEnabled: Boolean) {
        isLeftSwipeEnabled = isEnabled
        if (isLeftSwipeEnabled && !isRightSwipeEnabled) {
            slidingButton.x = width.toFloat()
        }
    }

    fun setRightSwipeEnabled(isEnabled: Boolean) {
        isRightSwipeEnabled = isEnabled
        if (isRightSwipeEnabled && !isLeftSwipeEnabled) {
            slidingButton.x = 0f
        }
    }

    private fun init(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) {

        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.SwipeAnimationButton)

        backgroundCardColor = typedArray.getColor(R.styleable.SwipeAnimationButton_backgroundCardColor, ContextCompat.getColor(context, R.color.black))
        defaultBackgroundColor = typedArray.getColor(R.styleable.SwipeAnimationButton_defaultBackgroundColor, ContextCompat.getColor(context, R.color.colorRED))
        defaultDrawable = ContextCompat.getDrawable(context, typedArray.getResourceId(R.styleable.SwipeAnimationButton_defaultDrawable, R.drawable.sentimental_neutral))
        currentDrawable = defaultDrawable
        isLeftSwipeEnabled = typedArray.getBoolean(R.styleable.SwipeAnimationButton_leftSwipeEnabled, true)
        isRightSwipeEnabled = typedArray.getBoolean(R.styleable.SwipeAnimationButton_rightSwipeEnabled, true)
        rightSwipeDrawable = ContextCompat.getDrawable(context, typedArray.getResourceId(R.styleable.SwipeAnimationButton_rightSwipeDrawable, R.drawable.swipe_sentimental_satisfied))
        rightSwipeBackgroundColor = typedArray.getColor(R.styleable.SwipeAnimationButton_rightSwipeBackgroundColor, ContextCompat.getColor(context, R.color.colorBLUE))
        leftSwipeDrawable = ContextCompat.getDrawable(context, typedArray.getResourceId(R.styleable.SwipeAnimationButton_leftSwipeDrawable, R.drawable.swipe_sentimental_dissatisfied))
        leftSwipeBackgroundColor = typedArray.getColor(R.styleable.SwipeAnimationButton_leftSwipeBackgroundColor, ContextCompat.getColor(context, R.color.colorGREEN))
        leftSwipeText = typedArray.getString(R.styleable.SwipeAnimationButton_leftSwipeText) ?: ""
        rightSwipeText = typedArray.getString(R.styleable.SwipeAnimationButton_rightSwipeText) ?: ""
        textSize = typedArray.getDimension(R.styleable.SwipeAnimationButton_text_size, MyUtils.spToPx(context, 16f).toFloat())
        fontType = typedArray.getFont(R.styleable.SwipeAnimationButton_fontType)
        duration = typedArray.getInteger(R.styleable.SwipeAnimationButton_duration, 200).toLong()

        background = RelativeLayout(context)

        val layoutParamsView = LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        layoutParamsView.addRule(CENTER_IN_PARENT, TRUE)
        background.setPadding(40, 40, 40, 40)
        addView(background, layoutParamsView)

        backgroundCard = if (isInEditMode) MaterialCardView(context) else MaterialCardView(context, null, com.google.android.material.R.style.Widget_Material3_CardView_Elevated)
        val layoutParamsBackground = LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, MyUtils.dpToPx(context, 100f))
        backgroundCard.radius = MyUtils.dpToPx(context, 100f).toFloat()
        backgroundCard.elevation = 0f
        backgroundCard.isChecked = true
        backgroundCard.setCardBackgroundColor(backgroundCardColor)
        addView(backgroundCard, layoutParamsBackground)

        val swipeButton = MaterialButton(context).apply {
            isClickable = false
            isFocusable = false
            iconSize = MyUtils.dpToPx(context, 28f)
            iconGravity = MaterialButton.ICON_GRAVITY_TEXT_START
            gravity = Gravity.CENTER
        }

        this.slidingButton = swipeButton


        slidingButton.icon = defaultDrawable
        slidingButton.setPadding(66, 60, 66, 60)

        val layoutParamsButton = LayoutParams(MyUtils.dpToPx(context, 100f), MyUtils.dpToPx(context, 68f))
        layoutParamsButton.addRule(CENTER_HORIZONTAL, TRUE)
        layoutParamsButton.addRule(CENTER_VERTICAL, TRUE)

        swipeButton.icon = defaultDrawable
        swipeButton.elevation = 20f
        swipeButton.setBackgroundColor(defaultBackgroundColor)

        ValueAnimator.ofFloat(0f, -15f).apply {
            duration = 100
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = AnimationUtils.loadInterpolator(context, android.R.anim.accelerate_decelerate_interpolator)

            var count = 0

            addUpdateListener {
                val bitmap = currentDrawable?.toBitmap(200, 200, Bitmap.Config.ARGB_8888)
                bitmap?.density = DisplayMetrics.DENSITY_HIGH
                if (bitmap != null) {
                    val icon = bitmap.rotate(if ((count / 2) % 2 == 0) -(it.animatedValue as Float) else it.animatedValue as Float)
                    swipeButton.icon = BitmapDrawable(resources, icon)
                    slidingButton.icon = BitmapDrawable(resources, icon)
                }
            }
            addListener(object: AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator) {
                    super.onAnimationStart(animation)
                }

                override fun onAnimationRepeat(animation: Animator) {
                    super.onAnimationRepeat(animation)
                    if (++count > 7) {
                        animation.startDelay = 400
                        animation.start()
                        count = 0
                    }
                }

                override fun onAnimationEnd(animation: Animator, isReverse: Boolean) {
                    super.onAnimationEnd(animation, isReverse)

                }
            })
        }.start()

        // TextView
        leftSwipeTextView = MaterialTextView(context)
        leftSwipeTextView.text = leftSwipeText
        leftSwipeTextView.textSize = textSize
        leftSwipeTextView.typeface = fontType
        leftSwipeTextView.setTextColor(ContextCompat.getColor(context, R.color.white))
        val layoutParamsLeftSwipeTextView = LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        layoutParamsLeftSwipeTextView.addRule(ALIGN_PARENT_START)
        layoutParamsLeftSwipeTextView.addRule(CENTER_VERTICAL)
        layoutParamsLeftSwipeTextView.marginStart = MyUtils.dpToPx(context, 14f)

        rightSwipeTextView = MaterialTextView(context)
        rightSwipeTextView.text = rightSwipeText
        rightSwipeTextView.textSize = textSize
        rightSwipeTextView.typeface = fontType
        rightSwipeTextView.setTextColor(ContextCompat.getColor(context, R.color.white))
        val layoutParamsRightSwipeTextView = LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        layoutParamsRightSwipeTextView.addRule(ALIGN_PARENT_END)
        layoutParamsRightSwipeTextView.addRule(CENTER_VERTICAL)
        layoutParamsRightSwipeTextView.marginEnd = MyUtils.dpToPx(context, 14f)

        addView(leftSwipeTextView, layoutParamsLeftSwipeTextView)
        addView(rightSwipeTextView, layoutParamsRightSwipeTextView)
        addView(swipeButton, layoutParamsButton)

        background.viewTreeObserver.addOnGlobalLayoutListener(object: OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                background.viewTreeObserver.removeOnGlobalLayoutListener(this)
                moveToCenter()
            }
        })

        setOnTouchListener(getButtonTouchListener(context))
        typedArray.recycle()
    }

    private fun getButtonTouchListener(context: Context) = OnTouchListener { view, motionEvent ->
        performClick()

        when (motionEvent.action) {
            MotionEvent.ACTION_DOWN -> {
                if (isLeftSwipeEnabled && isRightSwipeEnabled && (motionEvent.x in width / 2 - slidingButton.width / 2f .. width / 2 + slidingButton.width / 2f)) {
                    isButtonClicked = true
                } else if (!isLeftSwipeEnabled && isRightSwipeEnabled && (motionEvent.x in 0f .. slidingButton.width + MyUtils.dpToPx(context, 16f).toFloat())) {
                    isButtonClicked = true
                } else if (!isRightSwipeEnabled && isLeftSwipeEnabled && (motionEvent.x in width - slidingButton.width - MyUtils.dpToPx(context, 16f).toFloat() .. width.toFloat())) {
                    isButtonClicked = true
                }
                true
            }
            MotionEvent.ACTION_MOVE -> {
                if (isButtonClicked) {
                    if (initialX == 0f) {
                        initialX = slidingButton.x
                    }

                    if (motionEvent.x > initialX + slidingButton.width / 2 && motionEvent.x + slidingButton.width / 2 < width) {
                        // sliding right
                        slidingButton.x = if (motionEvent.x + slidingButton.width / 2 > width - MyUtils.dpToPx(context, 16f)) width - MyUtils.dpToPx(context, 16f).toFloat() - slidingButton.width else motionEvent.x - slidingButton.width / 2
                    }

                    if (motionEvent.x < initialX + slidingButton.width / 2 && motionEvent.x + slidingButton.width / 2 < width) {
                        // sliding left
                        slidingButton.x = if (motionEvent.x - slidingButton.width / 2 < MyUtils.dpToPx(context, 16f)) MyUtils.dpToPx(context, 16f).toFloat() else motionEvent.x - slidingButton.width / 2
                    }

                    if (motionEvent.x + slidingButton.width / 2 < width && slidingButton.x < 4) {
                        slidingButton.x = MyUtils.dpToPx(context, 16f).toFloat()
                    }
                }

                true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isButtonClicked = false
                if (isActive) {
                    collapseButton()
                } else {
                    initialButtonWidth = slidingButton.width
                    initialButtonHeight = slidingButton.height

                    if (slidingButton.x + slidingButton.width > width * 0.8 && isRightSwipeEnabled) {
                        expandButton(RIGHT)
                    } else if (slidingButton.x < width * 0.2 && isLeftSwipeEnabled) {
                        expandButton(LEFT)
                    } else {
                        moveToCenter()
                    }
                }
                true
            }
            else -> false
        }

    }

    private fun expandButton(isRight: Boolean) {
        val positionAnimator = ValueAnimator.ofFloat(slidingButton.x, 0f).apply {
            addUpdateListener {
                slidingButton.x = it.animatedValue as Float
            }
        }
        val widthAnimator = ValueAnimator.ofInt(slidingButton.width, width).apply {
            addUpdateListener {
                val params = slidingButton.layoutParams
                params.width = it.animatedValue as Int
                slidingButton.layoutParams = params
                if (isRight) {
                    slidingButton.icon = rightSwipeDrawable
                    slidingButton.setBackgroundColor(rightSwipeBackgroundColor)
                } else {
                    slidingButton.icon = leftSwipeDrawable
                    slidingButton.setBackgroundColor(leftSwipeBackgroundColor)
                }
                slidingButton.setPadding(66, 70, 66, 70)
            }
        }
        val heightAnimator = ValueAnimator.ofInt(slidingButton.height, MyUtils.dpToPx(context, 100f)).apply {
            addUpdateListener {
                val params = slidingButton.layoutParams
                params.height = it.animatedValue as Int
                slidingButton.layoutParams = params
                if (isRight) {
                    slidingButton.icon = rightSwipeDrawable
                    currentDrawable = rightSwipeDrawable
                    slidingButton.setBackgroundColor(rightSwipeBackgroundColor)
                } else {
                    slidingButton.icon = leftSwipeDrawable
                    currentDrawable = leftSwipeDrawable
                    slidingButton.setBackgroundColor(leftSwipeBackgroundColor)
                }
                slidingButton.setPadding(66, 70, 66, 70)
            }
        }
        val animatorSet = AnimatorSet().apply {
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)
                    isActive = true
                }
            })
        }
        animatorSet.playTogether(positionAnimator, widthAnimator, heightAnimator)
        animatorSet.start()
        swipeAnimationListener?.onSwiped(isRight)
    }

    private fun collapseButton() {
        val widthAnimator = ValueAnimator.ofInt(slidingButton.width, initialButtonWidth).apply {
            addUpdateListener {
                val params = slidingButton.layoutParams
                params.width = it.animatedValue as Int
                slidingButton.layoutParams = params
            }
        }
        val heightAnimator = ValueAnimator.ofInt(slidingButton.height, initialButtonHeight).apply {
            addUpdateListener {
                val params = slidingButton.layoutParams
                params.height = it.animatedValue as Int
                slidingButton.layoutParams = params
            }
        }
        val animatorSet = AnimatorSet().apply {
            addListener(object: AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)
                    isActive = false
                    slidingButton.setPadding(66, 60, 66, 60)
                    slidingButton.icon = defaultDrawable
                    currentDrawable = defaultDrawable
                    slidingButton.setBackgroundColor(defaultBackgroundColor)
                }
            })
        }
        animatorSet.playTogether(widthAnimator, heightAnimator)
        animatorSet.start()
        moveToCenter()
    }

    private fun moveToCenter() {
        if (initialButtonWidth == 0) initialButtonWidth = slidingButton.width

        ValueAnimator.ofFloat(slidingButton.x,
            if (isLeftSwipeEnabled && !isRightSwipeEnabled) (width.toFloat() - initialButtonWidth - MyUtils.dpToPx(context, 16f))
            else if (isRightSwipeEnabled && !isLeftSwipeEnabled) MyUtils.dpToPx(context, 16f).toFloat()
            else (width / 2f - initialButtonWidth / 2f)).apply {
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener {
                slidingButton.x = it.animatedValue as Float
            }
            addListener(object: AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)
                    Log.d(TAG, "${slidingButton.x}")
                }
            })
            this.duration = duration
        }.start()
    }

    private fun Bitmap?.rotate(angle: Float): Bitmap? {
        if (this == null) return null
        var newWidth = width
        var newHeight = height
        if (angle == 90f || angle == 270f) {
            newWidth = height
            newHeight = width
        }
        val rotatedBitmap = Bitmap.createBitmap(newWidth, newHeight, config)
        val canvas = Canvas(rotatedBitmap)
        val rect = Rect(0, 0, newWidth, newHeight)
        val matrix = Matrix()
        val px: Float = rect.exactCenterX()
        val py: Float = rect.exactCenterY()
        matrix.postTranslate((-width / 2).toFloat(), (-height / 2).toFloat())
        matrix.postRotate(angle)
        matrix.postTranslate(px, py)
        canvas.drawBitmap(this, matrix, Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG or Paint.FILTER_BITMAP_FLAG))
        matrix.reset()

        return rotatedBitmap
    }
}