package com.example.epubreader.reader

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewConfiguration
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Page turning with a hand-drawn page curl.
 *
 * Pages arrive as ready-made bitmaps, so a turn is only a compositing problem:
 *
 *  1. the destination page fills the view,
 *  2. the sheet that is lifting drops a shadow on it, along the crease,
 *  3. the current page is drawn flat, on the near side of the crease,
 *  4. the part of the sheet that has folded over is drawn as the page's own back: the
 *     theme's paper, a faint mirrored print-through of the page, and a cylinder
 *     gradient taken across the roll.
 *
 * The sheet is hinged on the spine, the way a page in a book is. The crease always runs
 * through one corner of the reading area — a corner on the spine line — and turns about
 * it: at rest it lies along the outer edge and nothing is folded, and a quarter turn
 * later it lies along the spine with the whole sheet over. The hand drives that angle,
 * so the foot of the crease never leaves the gutter and the sheet never reads as a loose
 * leaf lying across the middle of the page.
 *
 * The folded-over paper is the exact mirror of the part that has left the page, which is
 * where a sheet of paper actually lands when it folds back on itself: it comes to rest
 * against the spine, not in the air. Its width is the page's own depth past the crease,
 * so it is automatically narrow at the start of a pull and covers the page at the end.
 *
 * Releasing past half the pull, or flicking fast enough, completes the turn; otherwise
 * the sheet falls back onto the page being read. A page is turned by dragging it — a tap
 * that stays put belongs to the chrome, which shows or hides itself, or starts reading
 * from the sentence under the finger.
 */
class CurlView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    /** Supplies the already-rendered page bitmaps. */
    interface PageSource {
        val pageCount: Int
        fun bitmapFor(index: Int): Bitmap?

        /**
         * Rectangles (in view space) covering [sentenceIndex] on [pageIndex], for the
         * highlight. Empty when that sentence is not on that page.
         */
        fun highlightRects(pageIndex: Int, sentenceIndex: Int): List<RectF> = emptyList()
    }

    var pageSource: PageSource? = null
        set(value) {
            if (field === value) return
            field = value
            val count = value?.pageCount ?: 0
            if (currentIndex > count - 1) currentIndex = maxOf(0, count - 1)
            baseIndex = currentIndex
            direction = 0
            sweep = 0f
            leanRad = 0f
            invalidate()
        }

    /** Fired after a turn settles, with the new page index. */
    var onPageChanged: ((Int) -> Unit)? = null

    /**
     * Fired when a tap landed on the page rather than turning it, in view-space
     * pixels. Turning is a drag; a tap is the reader talking to the chrome.
     */
    var onPageTap: ((Float, Float) -> Unit)? = null

    /** Global sentence index to highlight, or -1. Only drawn while the page is idle. */
    var highlightSentence: Int = -1
        set(value) {
            if (field == value) return
            field = value
            invalidate()
        }

    private val density = resources.displayMetrics.density
    private val shadowWidth = 42f * density
    private val crestWidth = 2.4f * density
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop

    private val faintPaint = Paint(Paint.FILTER_BITMAP_FLAG).apply { alpha = 22 }
    private val shadowPaint = Paint()
    private val cylinderPaint = Paint()
    private val highlightPaint = Paint().apply { color = 0x59FFFFFF }
    private val flapEdgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.2f * density
    }
    private val speechHighlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = HIGHLIGHT }
    private val highlightRadius = 3f * density

    private var currentIndex = 0

    /** The page being turned away from; equals `currentIndex` while idle. */
    private var baseIndex = 0

    /** +1 turning forward, -1 turning backward, 0 idle. */
    private var direction = 0

    /** How far the sheet has been pulled over: 0 at rest, 1 fully turned. */
    private var sweep = 0f

    /**
     * How far the crease is leaned off straight across, in radians: zero for a pull
     * straight at the spine, positive when the hand also goes down the page. Fixed once
     * the pull is under way, so a wandering hand cannot twist the fold.
     */
    private var leanRad = 0f

    private var dragging = false
    private var downX = 0f
    private var downY = 0f

    /** How far the finger has travelled this pull, in pixels — what `sweep` is made from. */
    private var fingerTravel = 0f

    private var velocity: VelocityTracker? = null
    private var animator: ValueAnimator? = null
    private var animToken = 0
    private var pendingCommit = false

    /** A page the view has been asked to reach, which may be several single curls away. */
    private var queuedTarget = -1

    /** The fold the screenshot harness has asked to hold, put back if the view is remeasured. */
    private var heldFold: HeldFold? = null

    /** The pull the harness plays back through the drag path, so it is on the glass only once. */
    private var dragRun: ValueAnimator? = null
    private var dragPlayed = false

    // Scratch polygons for the two half-plane clips (a clipped rectangle keeps at
    // most 6 corners).
    private val polyX = FloatArray(8)
    private val polyY = FloatArray(8)
    private val clipX = FloatArray(8)
    private val clipY = FloatArray(8)
    private val foldX = FloatArray(8)
    private val foldY = FloatArray(8)
    private val mirrorMatrix = Matrix()

    val pageIndex: Int
        get() = currentIndex

    /** Pushes an index in from the outside (table of contents, restore). */
    fun setPageIndex(index: Int, notify: Boolean = false) {
        val source = pageSource
        val max = ((source?.pageCount ?: 0) - 1).coerceAtLeast(0)
        val coerced = index.coerceIn(0, max)
        // A page that is already the one being turned away from must be left alone. The
        // reader re-asserts the index it is on every time it recomposes, and that may not
        // cancel a curl that is under the reader's hand — or one the harness is holding
        // open for a photograph.
        if (coerced == currentIndex) {
            if (direction != 0) {
                Log.i(
                    TAG,
                    "page $coerced re-asserted mid-turn (direction=$direction, " +
                        "sweep=${fmt(sweep)}px): the curl is left alone",
                )
            }
            return
        }
        if (animator?.isRunning == true) return
        queuedTarget = -1
        currentIndex = coerced
        baseIndex = coerced
        direction = 0
        sweep = 0f
        leanRad = 0f
        invalidate()
        if (notify) onPageChanged?.invoke(coerced)
    }

    /**
     * Moves to [index] with the curl animation.
     *
     * A jump of more than one page is played as a run of single-page curls, so the turn
     * that follows the reading voice looks like paper being turned even when the next
     * sentence starts a couple of pages on. A very long leap (a table-of-contents jump
     * across half the book) is set instantly instead: flipping through a dozen pages is
     * just noise.
     */
    fun animateToPage(index: Int) {
        val source = pageSource ?: return
        val max = (source.pageCount - 1).coerceAtLeast(0)
        val target = index.coerceIn(0, max)
        // A turn that is already under way towards this page must be allowed to finish:
        // the reading voice reports the page it is on while the sheet is still folding,
        // and that is no reason to drop the curl.
        if (target == currentIndex) return
        if (abs(target - currentIndex) > MAX_QUEUED_STEPS) {
            queuedTarget = -1
            setPageIndex(target, notify = true)
            return
        }
        queuedTarget = target
        stepQueue(source)
    }

    /** Plays the next queued single-page curl, if one is still owed. */
    private fun stepQueue(source: PageSource) {        val target = queuedTarget
        if (target < 0 || target == currentIndex) {
            queuedTarget = -1
            return
        }
        if (animator?.isRunning == true || direction != 0) return
        val step = if (target > currentIndex) 1 else -1
        baseIndex = currentIndex
        direction = step
        animateTo(1f, commit = true)
    }

    /**
     * Holds a half-turned sheet still, so a fold can be photographed without a hand on
     * the glass: `--ef demoCurl 0.45 --ef demoCurlAngle 30 [--ez demoCurlBack true]`.
     *
     * [pull] is how far the sheet has been dragged, 0 at rest and 1 fully over.
     * [tiltDegrees] leans the crease off straight across: a positive angle tips its far
     * end up the page, so the fold opens from the top, which is what a hand dragging the
     * outer edge downwards does; a negative angle opens it from the bottom. Zero folds
     * straight across, both ends of the crease level.
     */
    fun holdFold(pull: Float, tiltDegrees: Float, back: Boolean) {
        val source = pageSource ?: return
        val wanted = if (back) -1 else 1
        if (currentIndex + wanted !in 0 until source.pageCount) return
        heldFold = HeldFold(pull, tiltDegrees, back)
        applyHeldFold(wanted)
    }

    /** Places the held fold now that the view has a size. */
    private fun applyHeldFold(wanted: Int) {
        val held = heldFold ?: return
        animator?.cancel()
        animator = null
        animToken++
        pendingCommit = false
        queuedTarget = -1
        baseIndex = currentIndex
        direction = wanted
        leanRad = Math.toRadians(held.tiltDegrees.toDouble()).toFloat().coerceIn(-MAX_LEAN, MAX_LEAN)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) {
            // No page to fold yet: the hold is put in place from `onSizeChanged`.
            sweep = 0f
            invalidate()
            return
        }
        sweep = held.pull.coerceIn(0f, 1f)
        invalidate()
        // The geometry is read back off the state the drawing will use, so the fold in
        // the screenshot and the fold in the log are the same fold.
        val drawn = foldGeometry(w, h)
        Log.i(
            TAG,
            "curl fold held: pull=${fmt(held.pull, 3)} lean=${fmt(Math.toDegrees(leanRad.toDouble()).toFloat())}deg " +
                "back=${held.back} sweep=${fmt(drawn.sweep * 100f, 0)}% " +
                "normal=(${fmt(drawn.nx, 4)}, ${fmt(drawn.ny, 4)}) " +
                "crease=${fmt(drawn.creaseX)}/${fmt(drawn.creaseY)} " +
                "folded=${fmt(drawn.revealed)}px -- ${creaseReport(drawn, w, h)}",
        )
    }

    /**
     * Plays a pull back through the same code the finger drives, so the curl can be
     * watched moving without a hand on the glass:
     * `--ef demoDragX -260 --ef demoDragY 90 --ef demoDragSeconds 1.2`.
     */
    fun playDrag(dx: Float, dy: Float, seconds: Float) {
        if (dragPlayed) return
        dragPlayed = true
        postDelayed({
            val source = pageSource
            if (source == null || source.pageCount == 0) return@postDelayed
            val span = seconds.coerceAtLeast(0.1f)
            Log.i(TAG, "curl drag: playing dx=$dx dy=$dy through the drag path over ${span}s")
            beginDrag()
            dragRun = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = (span * 1000f).toLong()
                interpolator = LinearInterpolator()
                addUpdateListener { animation ->
                    val t = animation.animatedValue as Float
                    moveDrag(dx * t, dy * t)
                }
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        dragRun = null
                        endDrag(dx / span)
                    }
                })
                start()
            }
        }, DEMO_DRAG_DELAY_MS)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // The fold the harness asked for is placed once the page has a size.
        if (heldFold != null && direction != 0) applyHeldFold(direction)
    }

    override fun onDetachedFromWindow() {
        animator?.cancel()
        animator = null
        dragRun?.cancel()
        dragRun = null
        velocity?.recycle()
        velocity = null
        super.onDetachedFromWindow()
    }

    // ------------------------------------------------------------------ draw

    override fun onDraw(canvas: Canvas) {
        canvas.drawColor(ReaderTheme.paper)
        val source = pageSource ?: return
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        if (direction == 0) {
            source.bitmapFor(baseIndex)?.let { canvas.drawBitmap(it, 0f, 0f, null) }
            drawHighlight(canvas, source)
            return
        }

        val current = source.bitmapFor(baseIndex)
        val target = source.bitmapFor(baseIndex + direction)
        if (current == null || target == null) {
            (current ?: target)?.let { canvas.drawBitmap(it, 0f, 0f, null) }
            return
        }

        val fold = foldGeometry(w, h)
        // Nothing has been pulled yet: the page is still lying flat and whole.
        if (fold.sweep <= 0.002f) {
            canvas.drawBitmap(current, 0f, 0f, null)
            return
        }

        // 1. the page underneath, uncovered as the sheet lifts away
        canvas.drawBitmap(target, 0f, 0f, null)

        // 2. the shadow the lifting sheet casts on it, a band just past the crease
        foldRegion(fold, w, h, from = fold.crease, to = fold.crease + shadowWidth)?.let { band ->
            canvas.save()
            canvas.clipPath(band)
            shadowPaint.shader = LinearGradient(
                fold.nx * fold.crease, fold.ny * fold.crease,
                fold.nx * (fold.crease + shadowWidth), fold.ny * (fold.crease + shadowWidth),
                SHADOW_DARK, SHADOW_CLEAR, Shader.TileMode.CLAMP,
            )
            canvas.drawRect(0f, 0f, w, h, shadowPaint)
            canvas.restore()
        }

        // 3. the page that is still flat, on the near side of the crease
        foldRegion(fold, w, h, to = fold.crease)?.let { flat ->
            canvas.save()
            canvas.clipPath(flat)
            canvas.drawBitmap(current, 0f, 0f, null)
            canvas.restore()
        }

        // 4. the part of the sheet that has folded over. It is the exact mirror of the
        //    paper that has left the page, so it comes to rest against the spine the way
        //    a real leaf does, and its width follows the pull on its own.
        val foldedOver = reflectedRegion(fold, w, h, from = fold.crease)
        if (foldedOver != null && fold.revealed > 0.6f) {
            canvas.save()
            canvas.clipPath(foldedOver)
            canvas.drawColor(ReaderTheme.paper)

            if (fold.revealed > 3f) {
                // Faint print-through: the page mirrored about the crease, at 9%, the way
                // a printed sheet shows its own type through its back. The mirror is the
                // same reflection the folded-over paper itself was laid out with.
                canvas.save()
                canvas.concat(mirrorAbout(fold))
                canvas.drawBitmap(current, 0f, 0f, faintPaint)
                canvas.restore()
            }

            // Cylinder shading, taken across the roll: dark where it leaves the page
            // and where it meets the next one, bright along the crown. On dark paper the
            // sheet is the same colour as the page, so the curl is read off the light it
            // catches rather than off a shadow it could not show.
            val dark = ReaderTheme.isDark
            cylinderPaint.shader = LinearGradient(
                fold.creaseX - fold.nx * fold.revealed, fold.creaseY - fold.ny * fold.revealed,
                fold.creaseX, fold.creaseY,
                if (dark) CYLINDER_COLORS_DARK else CYLINDER_COLORS, CYLINDER_POSITIONS,
                Shader.TileMode.CLAMP,
            )
            canvas.drawRect(0f, 0f, w, h, cylinderPaint)

            // A thin highlight right on the crest of the fold.
            foldRegion(fold, w, h, from = fold.crease - crestWidth, to = fold.crease)?.let { crest ->
                canvas.drawPath(crest, highlightPaint)
            }

            // The sheet's own edge: the thickness of the paper, all the way round the
            // piece that has folded over. It is what separates the back from the page it
            // lies on when both are the same colour.
            flapEdgePaint.color = Palette.blend(ReaderTheme.paper, ReaderTheme.ink, 0.30f)
            canvas.drawPath(foldedOver, flapEdgePaint)

            canvas.restore()
        }
    }

    // --------------------------------------------------------------- highlight

    /** Draws the amber band for the sentence being read, over the idle page. */
    private fun drawHighlight(canvas: Canvas, source: PageSource) {
        if (highlightSentence < 0) return
        val rects = source.highlightRects(baseIndex, highlightSentence)
        for (index in rects.indices) {
            canvas.drawRoundRect(rects[index], highlightRadius, highlightRadius, speechHighlightPaint)
        }
    }

    // ---------------------------------------------------------------- touches

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val source = pageSource ?: return false
        if (source.pageCount == 0) return false

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                beginDrag()
                downX = event.x
                downY = event.y
                velocity = VelocityTracker.obtain().also { it.addMovement(event) }
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                velocity?.addMovement(event)
                moveDrag(event.x - downX, event.y - downY)
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                velocity?.addMovement(event)
                velocity?.computeCurrentVelocity(1000)
                val vx = velocity?.xVelocity ?: 0f
                velocity?.recycle()
                velocity = null

                if (event.actionMasked == MotionEvent.ACTION_CANCEL) {
                    if (direction != 0) animateTo(0f, commit = false)
                    return true
                }

                if (!dragging) {
                    // A tap never turns a page: the page is turned by dragging it,
                    // and a touch that stays put is the reader talking to the chrome
                    // — show or hide it, or read from the sentence under the finger.
                    onPageTap?.invoke(event.x, event.y)
                    return true
                }

                endDrag(vx)
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    // ------------------------------------------------------------- the pull

    private fun beginDrag() {
        queuedTarget = -1
        settleAnimation()
        baseIndex = currentIndex
        direction = 0
        sweep = 0f
        leanRad = 0f
        dragging = false
        invalidate()
    }

    /**
     * One frame of a pull, in view-space pixels from where the finger went down.
     *
     * The pull has to be sideways enough to be a turn of the page rather than a shift
     * of the hand. The crease is then drawn square to the pull: a hand going straight
     * across folds the sheet straight across, both ends of the crease the same distance
     * from the spine, and a hand that also travels up or down leans the fold that way.
     * The lean is read while the fold is still small and held for the rest of the
     * gesture, so a wandering hand cannot twist it. The pull itself is measured against
     * the page's width, so a pull as long as the page is wide takes the crease all the
     * way onto the spine.
     */
    private fun moveDrag(dx: Float, dy: Float) {
        val source = pageSource ?: return
        if (direction == 0) {
            if (hypot(dx, dy) <= touchSlop || abs(dx) <= abs(dy) * DRAG_AXIS_FRACTION) return
            val wanted = if (dx < 0f) 1 else -1
            direction = if (currentIndex + wanted in 0 until source.pageCount) wanted else 0
            if (direction == 0) return
            dragging = true
        }
        val width = width.toFloat()
        if (width <= 0f) return
        fingerTravel = hypot(dx, dy)
        val pull = (fingerTravel / width).coerceIn(0f, 1f)
        if (pull < LEAN_LOCK_FRACTION) {
            leanRad = atan2(dy, abs(dx)).coerceIn(-MAX_LEAN, MAX_LEAN)
        }
        sweep = pull
        invalidate()
    }

    private fun endDrag(velocityX: Float) {
        if (direction == 0) return
        val fling = abs(velocityX) > FLING_VELOCITY &&
            ((direction > 0 && velocityX < 0f) || (direction < 0 && velocityX > 0f))
        val complete = sweep > COMMIT_FRACTION || fling
        val w = width.toFloat()
        val h = height.toFloat()
        val drawn = if (w > 0f && h > 0f) foldGeometry(w, h) else null
        Log.i(
            TAG,
            "curl pull: direction=$direction lean=${fmt(Math.toDegrees(leanRad.toDouble()).toFloat())}deg " +
                "finger=${fmt(fingerTravel)}px of ${fmt(w)} wide -> " +
                "pull=${fmt(sweep * 100f, 0)}% vx=${fmt(velocityX, 0)}px/s -> " +
                if (complete) "turn" else "fall back",
        )
        if (drawn != null) Log.i(TAG, "curl pull: ${creaseReport(drawn, w, h)}")
        animateTo(if (complete) 1f else 0f, commit = complete)
    }

    // ------------------------------------------------------------- animation

    private fun animateTo(target: Float, commit: Boolean) {
        animator?.cancel()
        val token = ++animToken
        pendingCommit = commit
        val from = sweep
        // The span is the pull itself, 0..1, so a fold that has further to travel the
        // crease takes longer than one that only has to fall back.
        val span = abs(target - from)

        animator = ValueAnimator.ofFloat(from, target).apply {
            duration = (150f + 240f * span).toLong().coerceIn(150L, 400L)
            interpolator = DecelerateInterpolator(1.7f)
            addUpdateListener { animation ->
                if (token != animToken) return@addUpdateListener
                sweep = animation.animatedValue as Float
                invalidate()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (token != animToken) return
                    animator = null
                    if (pendingCommit) commitTurn() else resetTurn()
                }
            })
            start()
        }
    }

    /** A new touch interrupted an animation: snap to whichever end it was heading for. */
    private fun settleAnimation() {
        val running = animator?.isRunning == true
        if (!running) {
            resetTurn()
            return
        }
        animToken++
        animator?.removeAllListeners()
        animator?.cancel()
        animator = null
        if (pendingCommit) commitTurn() else resetTurn()
        pendingCommit = false
    }

    private fun commitTurn() {
        pendingCommit = false
        val next = baseIndex + direction
        val count = pageSource?.pageCount ?: 0
        val queued = queuedTarget >= 0
        if (direction != 0 && next in 0 until count) {
            currentIndex = next
            // A programmatic run of curls (a jump from the contents, or the page following
            // the reading voice) reports the page it *arrives at*, once. The pages it flips
            // through on the way are not pages anyone opened: telling the reader about each
            // of them would drag a reading voice along for the ride.
            if (!queued || next == queuedTarget) onPageChanged?.invoke(next)
        }
        direction = 0
        sweep = 0f
        leanRad = 0f
        baseIndex = currentIndex
        if (heldFold != null) {
            // A fold the harness is holding is put back where it was asked for, so a
            // held fold survives any jump the harness also asked for.
            applyHeldFold(if (heldFold?.back == true) -1 else 1)
            return
        }
        invalidate()
        // A multi-page move continues from here, one curl at a time.
        pageSource?.let { stepQueue(it) }
    }

    private fun resetTurn() {
        pendingCommit = false
        direction = 0
        sweep = 0f
        leanRad = 0f
        baseIndex = currentIndex
        invalidate()
    }

    // -------------------------------------------------------------- geometry

    /**
     * Where the sheet is folded.
     *
     * The crease is a straight line across the page with normal [nx]/[ny], and everything
     * is measured along that normal: `s(p) = p·n`. The crease is the line `s = [crease]`,
     * the page still lying flat is `s < crease`, and the paper that has folded over is the
     * mirror of `s > crease`, which is exactly where it lands on the flat page.
     *
     * The normal is the pull's own direction, so a hand that goes straight across folds
     * the sheet straight across — both ends of the crease the same distance from the
     * spine — and a hand that also goes up or down leans the fold that way. [sweep] is
     * how far the sheet has been pulled over, and it is measured across the page's
     * *width*, so the crease travels from the outer corner to the spine corner nearest
     * the pull and stops there. (Measured across the page's whole diagonal instead, a
     * leaning crease swept on past the gutter and lifted the bound edge with it — the
     * page came away from the spine, which is exactly what it must never do.)
     *
     * [revealed] is how deep the page runs past the crease, so it is also how deep the
     * folded-over paper is.
     */
    private class Fold(
        val nx: Float,
        val ny: Float,
        /** `p·n` at the crease. */
        val crease: Float,
        /** How deep the page runs past the crease, in pixels. */
        val revealed: Float,
        /** How far the sheet has been pulled over: 0..1. */
        val sweep: Float,
    ) {
        /** A point on the crease. */
        val creaseX: Float get() = nx * crease
        val creaseY: Float get() = ny * crease
    }

    /**
     * Reads the fold off the state: a line square to the pull, slid in from the outer
     * edge by the pull's share of the page's width. The spine is never crossed, so the
     * bound edge of the leaf stays where it is bound.
     */
    private fun foldGeometry(w: Float, h: Float): Fold {
        val pull = sweep.coerceIn(0f, 1f)
        val outward = if (direction < 0) -1f else 1f
        // The fold carries the hand's lean while the sheet is still a folded corner, then
        // straightens as the leaf comes over. A crease that kept the lean all the way
        // could never finish: on a page much taller than it is wide the leaning crease
        // leaves through the fore-edge long before the spine, so the last of the leaf —
        // a triangle at the head (going up) or at the tail (going down) — stayed lying
        // flat, and the curl never reached the end of the book it was curling towards.
        // A page finishes along its gutter, where the crease stands square to it, so the
        // fold turns upright as it arrives and the head (or the tail) curls with it.
        val lean = leanRad * leanScale(pull)
        val nx = outward * cos(lean)
        // The peeled side is the side the sheet is pulled *away* from, so the normal
        // runs against the finger's own lean: a hand that drags down carries the outer
        // edge down, and the fold opens from the top. The sign was the other way in
        // Part 8, which made a downward drag peel the page up from its bottom edge.
        val ny = -sin(lean)
        val corners = floatArrayOf(0f, 0f, w, 0f, w, h, 0f, h)
        var far = -Float.MAX_VALUE
        for (i in 0 until 4) {
            val s = corners[i * 2] * nx + corners[i * 2 + 1] * ny
            if (s > far) far = s
        }
        // The width's own projection on the normal: a leaning fold has that much less
        // page to cross before it reaches the gutter, and the crease ends up against
        // the gutter corner instead of somewhere past it. With the lean faded out by
        // the end of the pull this is the whole width, so the crease lands on the spine
        // and the page comes away whole.
        val travel = max(w * abs(cos(lean)), 1f)
        val crease = far - pull * travel
        return Fold(
            nx = nx,
            ny = ny,
            crease = crease,
            revealed = pull * travel,
            sweep = pull,
        )
    }

    /**
     * How much of the hand's tilt the fold still carries at a given pull: all of it
     * while the sheet is a corner being lifted, none of it once the leaf is coming off
     * the spine. Smoothstepped so the crease turns without a kink, and over with well
     * before the turn commits, so the curl reaches the head (or the tail) of the book
     * while the hand is still holding it there.
     */
    private fun leanScale(pull: Float): Float {
        if (pull <= LEAN_FADE_FROM) return 1f
        val t = ((pull - LEAN_FADE_FROM) / (LEAN_FADE_TO - LEAN_FADE_FROM)).coerceIn(0f, 1f)
        return 1f - t * t * (3f - 2f * t)
    }

    /**
     * Where the crease meets the page's border, as percents of the width and height, and
     * whether any of the bound edge has been peeled up. For the log: a screenshot shows
     * the fold, these two numbers say what the geometry was built to do.
     */
    private fun creaseReport(fold: Fold, w: Float, h: Float): String {
        // A unit vector along the crease, from the foot of the normal.
        val ux = -fold.ny
        val uy = fold.nx
        val points = ArrayList<Pair<Float, Float>>(2)
        val ends = ArrayList<String>(2)
        fun consider(x: Float, y: Float) {
            if (x < -0.5f || x > w + 0.5f || y < -0.5f || y > h + 0.5f) return
            // A crease through a corner meets two edges: report the point once.
            if (points.any { abs(it.first - x) < 2f && abs(it.second - y) < 2f }) return
            points.add(x to y)
            ends.add("(${fmt(x / w * 100f, 0)}%, ${fmt(y / h * 100f, 0)}%)")
        }
        if (abs(uy) > 1e-4f) {
            val t = -fold.creaseY / uy
            consider(fold.creaseX + ux * t, fold.creaseY + uy * t)
            val s = (h - fold.creaseY) / uy
            consider(fold.creaseX + ux * s, fold.creaseY + uy * s)
        }
        if (abs(ux) > 1e-4f) {
            val t = -fold.creaseX / ux
            consider(fold.creaseX + ux * t, fold.creaseY + uy * t)
            val s = (w - fold.creaseX) / ux
            consider(fold.creaseX + ux * s, fold.creaseY + uy * s)
        }
        // The bound edge must stay on the page: at full pull the crease reaches the
        // spine's corner exactly, which is touching it, not peeling it. That is the
        // forward turn's invariant; a back turn lays the previous page open from the
        // gutter on purpose, so the test says nothing there.
        val spine = when {
            direction < 0 -> "n/a (back)"
            max(0f, h * fold.ny) - fold.crease > 0.5f -> "PEELED"
            else -> "clear"
        }
        // The fold's own lean: the hand's lean, faded by the pull (see foldGeometry).
        val foldLean = Math.toDegrees(atan2(-fold.ny, abs(fold.nx)).toDouble()).toFloat()
        return "crease border ${ends.joinToString(" -> ")}; " +
            "fold lean=${fmt(foldLean)}deg; " +
            "spine $spine"
    }

    /**
     * The part of the view lying between two lines across the normal, or null when the
     * fold has left nothing of the page there. One Sutherland–Hodgman pass per line,
     * which is what a crease that goes through a corner needs instead of a rectangle.
     */
    private fun foldRegion(
        fold: Fold,
        w: Float,
        h: Float,
        from: Float? = null,
        to: Float? = null,
    ): Path? {
        val count = clipFold(fold, w, h, from, to)
        if (count < 3) return null
        return pathOf(foldX, foldY, count)
    }

    /**
     * [foldRegion] mirrored across the crease: where the folded-over sheet comes to rest.
     * A mirror, not a squeeze — the paper keeps its length when it folds, so the back
     * lands against the spine the way a real leaf does, and its own edge is the crease
     * moved out by however much paper has left the page.
     */
    private fun reflectedRegion(
        fold: Fold,
        w: Float,
        h: Float,
        from: Float? = null,
        to: Float? = null,
    ): Path? {
        val count = clipFold(fold, w, h, from, to)
        if (count < 3) return null
        val ux = -fold.ny
        val uy = fold.nx
        for (i in 0 until count) {
            val dx = foldX[i] - fold.creaseX
            val dy = foldY[i] - fold.creaseY
            val along = dx * ux + dy * uy
            foldX[i] = fold.creaseX + 2f * along * ux - dx
            foldY[i] = fold.creaseY + 2f * along * uy - dy
        }
        return pathOf(foldX, foldY, count)
    }

    /**
     * The reflection the folded-over paper is drawn through: a mirror about the crease,
     * built out of the fold's own normal so it can only ever agree with
     * [reflectedRegion], which lays the paper itself out from the same two numbers.
     */
    private fun mirrorAbout(fold: Fold): Matrix {
        val ux = -fold.ny
        val uy = fold.nx
        val a = 2f * ux * ux - 1f
        val b = 2f * ux * uy
        val d = 2f * uy * uy - 1f
        val cx = fold.creaseX
        val cy = fold.creaseY
        mirrorMatrix.setValues(
            floatArrayOf(
                a, b, cx - a * cx - b * cy,
                b, d, cy - b * cx - d * cy,
                0f, 0f, 1f,
            ),
        )
        return mirrorMatrix
    }

    /** Clips the view's corners to the band between two lines up the normal. */
    private fun clipFold(fold: Fold, w: Float, h: Float, from: Float?, to: Float?): Int {
        polyX[0] = 0f; polyY[0] = 0f
        polyX[1] = w; polyY[1] = 0f
        polyX[2] = w; polyY[2] = h
        polyX[3] = 0f; polyY[3] = h
        var xs = polyX
        var ys = polyY
        var count = 4
        if (from != null) {
            count = clipToHalfPlane(xs, ys, count, clipX, clipY, fold.nx, fold.ny, from, keepAbove = true)
            xs = clipX
            ys = clipY
            if (count < 3) return count
        }
        if (to != null) {
            val otherX = if (xs === polyX) clipX else polyX
            val otherY = if (ys === polyY) clipY else polyY
            count = clipToHalfPlane(xs, ys, count, otherX, otherY, fold.nx, fold.ny, to, keepAbove = false)
            xs = otherX
            ys = otherY
        }
        for (i in 0 until count) {
            foldX[i] = xs[i]
            foldY[i] = ys[i]
        }
        return count
    }

    /** Builds the path a clip pass left behind. */
    private fun pathOf(xs: FloatArray, ys: FloatArray, count: Int): Path {
        val path = Path()
        path.moveTo(xs[0], ys[0])
        for (i in 1 until count) path.lineTo(xs[i], ys[i])
        path.close()
        return path
    }

    /** One line of a Sutherland–Hodgman clip: keep the half plane the fold has not reached. */
    private fun clipToHalfPlane(
        inX: FloatArray,
        inY: FloatArray,
        count: Int,
        outX: FloatArray,
        outY: FloatArray,
        nx: Float,
        ny: Float,
        offset: Float,
        keepAbove: Boolean,
    ): Int {
        var out = 0
        for (i in 0 until count) {
            val j = (i + 1) % count
            val sa = inX[i] * nx + inY[i] * ny - offset
            val sb = inX[j] * nx + inY[j] * ny - offset
            val insideA = if (keepAbove) sa >= 0f else sa <= 0f
            val insideB = if (keepAbove) sb >= 0f else sb <= 0f
            if (insideA) {
                outX[out] = inX[i]
                outY[out] = inY[i]
                out++
            }
            if (insideA != insideB) {
                val t = sa / (sa - sb)
                outX[out] = inX[i] + (inX[j] - inX[i]) * t
                outY[out] = inY[i] + (inY[j] - inY[i]) * t
                out++
            }
        }
        return out
    }

    /** A fold the harness holds open, instead of needing a hand on the glass. */
    private class HeldFold(val pull: Float, val tiltDegrees: Float, val back: Boolean)

    /** The logs are read by the harness, so their numbers keep the dot. */
    private fun fmt(value: Float, decimals: Int = 1): String =
        String.format(java.util.Locale.US, "%.${decimals}f", value)

    private companion object {
        const val TAG = "ReaderScreen"

        /** How many single curls a programmatic jump may play before it just cuts. */
        const val MAX_QUEUED_STEPS = 4

        /** The crease may lean this far off straight across before the pull is squashed. */
        val MAX_LEAN = (PI / 4.0).toFloat()

        /**
         * The lean is read while the pull is under this fraction of the way over, so it
         * is settled early and then held; a hand that wanders cannot twist the fold.
         */
        const val LEAN_LOCK_FRACTION = 0.12f

        /**
         * Where the lean starts fading out of the fold, and where it is gone: the crease
         * turns upright between the two, so the sheet can finish against the spine.
         */
        const val LEAN_FADE_FROM = 0.12f
        const val LEAN_FADE_TO = 0.62f

        /** Past half the pull the sheet goes over; before that it falls back. */
        const val COMMIT_FRACTION = 0.5f
        const val FLING_VELOCITY = 900f

        /** A pull has to be at least this much sideways, against how far it went down. */
        const val DRAG_AXIS_FRACTION = 0.35f

        /** The harness's scripted pull waits this long for the page to be on screen. */
        const val DEMO_DRAG_DELAY_MS = 600L

        const val SHADOW_DARK = 0x59000000
        const val SHADOW_CLEAR = 0x00000000

        /** Amber highlight over the sentence being read: ~45% alpha (0x73 / 255). */
        const val HIGHLIGHT = 0x73F2C14E.toInt()

        /** The same roll on dark paper: the sheet catches the light instead of shadowing. */
        val CYLINDER_COLORS_DARK = intArrayOf(
            0x0CFFFFFF,
            0x18FFFFFF,
            0x24FFFFFF,
            0x18FFFFFF,
            0x0CFFFFFF,
        )

        val CYLINDER_COLORS = intArrayOf(
            0x2B000000.toInt(),
            0x0D000000,
            0x14FFFFFF,
            0x0D000000,
            0x2B000000.toInt(),
        )
        val CYLINDER_POSITIONS = floatArrayOf(0f, 0.32f, 0.5f, 0.68f, 1f)
    }
}
