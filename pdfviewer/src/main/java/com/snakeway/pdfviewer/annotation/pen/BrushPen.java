package com.snakeway.pdfviewer.annotation.pen;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.RectF;

import com.snakeway.pdflibrary.util.SizeF;
import com.snakeway.pdfviewer.CoordinateUtils;
import com.snakeway.pdfviewer.PDFView;

import java.util.ArrayList;
import java.util.List;

/**
 * @author snakeway
 */
public class BrushPen implements Pen.WritePen {
    private transient static final int MIN_DRAW_POINT_SIZE = 2;
    private transient Paint paint;
    private transient BezierPointF mCurPoint;

    private int color;
    private float penWidthScale;

    BrushPen(int color, float penWidthScale) {
        this.color = Color.rgb(Color.red(color), Color.green(color), Color.blue(color));//取消颜色的透明度
        this.penWidthScale = penWidthScale;
        init();
    }

    @Override
    public void init() {
        paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(this.color);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setAntiAlias(true);
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    @Override
    public void draw(List<SizeF> data, Canvas canvas, float scale, int basePenWidth, PDFView pdfView, int page) {
        if (data == null) {
            return;
        }
        if (data.size() < MIN_DRAW_POINT_SIZE) {
            return;
        }
        List<Point> points = new ArrayList<>();
        for (SizeF sizeF : data) {
            points.add(CoordinateUtils.toPdfPointCoordinateDesc(pdfView, page, sizeF.getWidth(), sizeF.getHeight()));
        }
        //把List<Point> 转换为List<BezierPointF>
        float penWidth = basePenWidth * penWidthScale / scale;
        Conversion conversion = new Conversion(points, penWidth, scale);
        List<BezierPointF> bezierPoints = conversion.getResult();
        if (bezierPoints != null && bezierPoints.size() > 2) {
            mCurPoint = bezierPoints.get(0);
            drawNeetToDo(canvas, bezierPoints);
        }
    }

    @Override
    public void drawWithOptimize(List<SizeF> data, Canvas canvas, float scale, int basePenWidth, PDFView pdfView, int page) {
        if (data == null) {
            return;
        }
        if (data.size() < MIN_DRAW_POINT_SIZE) {
            return;
        }
        this.draw(data,canvas,scale,basePenWidth,pdfView,page);
    }

    public void reset() {
    }


    @Override
    public PenType getPenType() {
        return PenType.BRUSHPEN;
    }

    private void drawNeetToDo(Canvas canvas, List<BezierPointF> datas) {
        for (int i = 1; i < datas.size(); i++) {
            BezierPointF point = datas.get(i);
            doNeetToDo(canvas, point, paint);
            mCurPoint = point;
        }
    }

    private void doNeetToDo(Canvas canvas, BezierPointF point, Paint paint) {
        drawLine(canvas, mCurPoint.x, mCurPoint.y, mCurPoint.width, point.x, point.y, point.width, paint);
    }

    private void drawLine(Canvas canvas, double x0, double y0, double w0, double x1, double y1, double w1, Paint paint) {
        //求两个数字的平方根 x的平方+y的平方在开方记得X的平方+y的平方=1，这就是一个园
        double curDis = Math.hypot(x0 - x1, y0 - y1);
        int steps = 1;
        if (paint.getStrokeWidth() < 6) {
            steps = 1 + (int) (curDis / 2);
        } else if (paint.getStrokeWidth() > 60) {
            steps = 1 + (int) (curDis / 4);
        } else {
            steps = 1 + (int) (curDis / 3);
        }
        double deltaX = (x1 - x0) / steps;
        double deltaY = (y1 - y0) / steps;
        double deltaW = (w1 - w0) / steps;
        double x = x0;
        double y = y0;
        double w = w0;

        for (int i = 0; i < steps; i++) {
            //都是用于表示坐标系中的一块矩形区域，并可以对其做一些简单操作
            //精度不一样。Rect是使用int类型作为数值，RectF是使用float类型作为数值。
            //            Rect rect = new Rect();
            RectF oval = new RectF();
            oval.set((float) (x - w / 4.0f),
                    (float) (y - w / 2.0f),
                    (float) (x + w / 4.0f),
                    (float) (y + w / 2.0f));
            // oval.set((float)(x+w/4.0f), (float)(y+w/4.0f), (float)(x-w/4.0f), (float)(y-w/4.0f));
            //最基本的实现，通过点控制线，绘制椭圆
            canvas.drawOval(oval, paint);
            x += deltaX;
            y += deltaY;
            w += deltaW;
        }
    }

    /**
     * 转换类，由这个类实现了path列表转换为Bezier曲线，并根据速度对path点间进行插桩补点
     */
    private class Conversion {
        /**
         * 这个控制笔锋的控制值
         */
        public static final float DIS_VEL_CAL_FACTOR = 0.007f;
        /**
         * 绘制计算的次数，数值越小计算的次数越多，需要折中
         */
        private static final int STEPFACTOR = 10;
        /**
         * 这个参数控制笔锋粗细变化速度
         */
        private static final float FACTOR = 2.0F;
        private List<Point> points;
        private float mLastWidth = 0F, mLastVel = 0F;
        private float mBaseWidth;
        private List<BezierPointF> pathData = new ArrayList<>();
        private List<BezierPointF> mHWPointList = new ArrayList<>();
        private BezierPointF mLastPoint;
        private Bezier mBezier;
        private float scale;

        Conversion(List<Point> points, float baseWidth, float scale) {
            this.points = points;
            mBaseWidth = baseWidth;
            mBezier = new Bezier();
            this.scale = scale;
        }

        /**
         * 主方法，获取处理后的点列表
         */
        List<BezierPointF> getResult() {
            if (points == null || points.size() < 2) {
                return new ArrayList<>();
            }
            for (int i = 0; i < points.size(); i++) {
                Point point = points.get(i);
                if (i == 0) {
                    onDown(new BezierPointF(point.x / scale, point.y / scale));
                } else {
                    onMove(new BezierPointF(point.x / scale, point.y / scale));
                }
            }
            return mHWPointList;
        }

        private void onDown(BezierPointF mElement) {
            mLastWidth = 0.8F * mBaseWidth;
            //down下的点的宽度
            mElement.width = mLastWidth;
            mLastVel = 0;
            pathData.add(mElement);
            //记录当前的点
            mLastPoint = mElement;

        }

        private void onMove(BezierPointF mElement) {
            double deltaX = mElement.x - mLastPoint.x;
            double deltaY = mElement.y - mLastPoint.y;
            //deltaX和deltay平方和的二次方根 想象一个例子 1+1的平方根为1.4 （x²+y²）开根号
            //同理，当滑动的越快的话，deltaX+deltaY的值越大，这个越大的话，curDis也越大
            double curDis = Math.hypot(deltaX, deltaY);
            //我们求出的这个值越小，画的点或者是绘制椭圆形越多，这个值越大的话，绘制的越少，笔就越细，宽度越小
            double curVel = curDis * DIS_VEL_CAL_FACTOR;
            double curWidth;
            //点的集合少，我们得必须改变宽度,每次点击的down的时候，这个事件
            if (pathData.size() < 2) {
                curWidth = calcNewWidth(curVel, mLastVel, FACTOR);
                mElement.width = (float) curWidth;
                mBezier.init(mLastPoint, mElement);
            } else {
                curWidth = calcNewWidth(curVel, mLastVel, FACTOR);
                mLastVel = (float) curVel;
                mElement.width = (float) curWidth;
                mBezier.addNode(mElement);
            }
            //每次移动的话，这里赋值新的值
            mLastWidth = (float) curWidth;
            pathData.add(mElement);
            moveNeetToDo(curDis);
            mLastPoint = mElement;
        }

        /**
         * 计算宽度
         *
         * @param curVel  2点间距离*{@link #DIS_VEL_CAL_FACTOR} 0.02F
         * @param lastVel 上一次的 curVel
         * @param factor  变化率
         * @return 宽度
         */
        private double calcNewWidth(double curVel, double lastVel, double factor) {
            double calVel = curVel * 0.6 + lastVel * (1 - 0.6);
            //返回指定数字的自然对数
            //手指滑动的越快，这个值越小，为负数
            double vfac = Math.log(factor * 2.0f) * (-calVel);
            //此方法返回值e，其中e是自然对数的基数。
            //Math.exp(vfac) 变化范围为0 到1 当手指没有滑动的时候 这个值为1 当滑动很快的时候无线趋近于0
            //在次说明下，当手指抬起来，这个值会变大，这也就说明，抬起手太慢的话，笔锋效果不太明显
            //这就说明为什么笔锋的效果不太明显
            double exp = Math.exp(vfac);
            return mBaseWidth * exp;
        }

        private void moveNeetToDo(double curDis) {
            int steps = 1 + (int) curDis / STEPFACTOR;
            double step = 1.0 / steps;
            for (double t = 0; t < 1.0; t += step) {
                BezierPointF point = mBezier.getPoint(t);
                mHWPointList.add(point);
            }
        }
    }

    /**
     * 贝塞尔曲线点模型，带宽度
     */
    private final class BezierPointF extends PointF {
        float width;

        BezierPointF(float x, float y) {
            super(x, y);
        }

        BezierPointF() {
        }

        void set(float x, float y, float w) {
            this.x = x;
            this.y = y;
            this.width = w;
        }

        void set(BezierPointF pointF) {
            this.x = pointF.x;
            this.y = pointF.y;
            this.width = pointF.width;
        }
    }

    /**
     * 贝塞尔曲线计算类
     */
    private class Bezier {
        /**
         * 资源的点 p1
         */
        private BezierPointF mSource = new BezierPointF();
        /**
         * 控制点的 p2
         */
        private BezierPointF mControl = new BezierPointF();
        /**
         * 距离 p3
         */
        private BezierPointF mDestination = new BezierPointF();
        /**
         * 下一个需要控制点
         */
        private BezierPointF mNextControl = new BezierPointF();

        Bezier() {
        }

        /**
         * 初始化两个点，
         *
         * @param last 最后的点的信息
         * @param cur  当前点的信息,当前点的信息，当前点的是根据事件获得，同时这个当前点的宽度是经过计算的得出的
         */
        void init(BezierPointF last, BezierPointF cur) {
            init(last.x, last.y, last.width, cur.x, cur.y, cur.width);
        }

        void init(float lastx, float lasty, float lastWidth, float x, float y, float width) {
            //资源点设置，最后的点的为资源点
            mSource.set(lastx, lasty, lastWidth);
            float xmid = getMid(lastx, x);
            float ymid = getMid(lasty, y);
            float wmid = getMid(lastWidth, width);
            //距离点为平均点
            mDestination.set(xmid, ymid, wmid);
            //控制点为当前的距离点
            mControl.set(getMid(lastx, xmid), getMid(lasty, ymid), getMid(lastWidth, wmid));
            //下个控制点为当前点
            mNextControl.set(x, y, width);
        }

        void addNode(BezierPointF cur) {
            addNode(cur.x, cur.y, cur.width);
        }

        /**
         * 替换就的点，原来的距离点变换为资源点，控制点变为原来的下一个控制点，距离点取原来控制点的和新的的一半
         * 下个控制点为新的点
         *
         * @param x     新的点的坐标
         * @param y     新的点的坐标
         * @param width 点宽度
         */
        void addNode(float x, float y, float width) {
            mSource.set(mDestination);
            mControl.set(mNextControl);
            mDestination.set(getMid(mNextControl.x, x), getMid(mNextControl.y, y), getMid(mNextControl.width, width));
            mNextControl.set(x, y, width);
        }

        BezierPointF getPoint(double t) {
            float x = (float) getX(t);
            float y = (float) getY(t);
            float w = (float) getW(t);
            BezierPointF point = new BezierPointF();
            point.set(x, y, w);
            return point;
        }

        /**
         * 三阶曲线的控制点 二次贝塞尔曲线公式
         *
         * @param p0 起点
         * @param p1 控制点
         * @param p2 终点
         * @param t  分量
         * @return 值
         */
        private double getValue(double p0, double p1, double p2, double t) {
            double A = p2 - 2 * p1 + p0;
            double B = 2 * (p1 - p0);
            double C = p0;
            return A * t * t + B * t + C;
        }

        private double getX(double t) {
            return getValue(mSource.x, mControl.x, mDestination.x, t);
        }

        private double getY(double t) {
            return getValue(mSource.y, mControl.y, mDestination.y, t);
        }

        private double getW(double t) {
            return getWidth(mSource.width, mDestination.width, t);
        }

        /**
         * @param x1 一个点的x
         * @param x2 一个点的x
         * @return 平均值
         */
        private float getMid(float x1, float x2) {
            return (x1 + x2) / 2.0F;
        }

        private double getWidth(double w0, double w1, double t) {
            return w0 + (w1 - w0) * t;
        }
    }

}
