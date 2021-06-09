package com.example.ipaint.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import androidx.annotation.Nullable;

import com.example.ipaint.R;

import java.util.ArrayList;

public class PaintSurfaceView extends SurfaceView implements SurfaceHolder.Callback,Runnable {

    private Bitmap btmBackground,btmView,image,captureImage,originalImage,rotateImage;
    private Paint mPaint = new Paint();
    private Path mPath = new Path();
    private int colorBackground,sizeBrush,sizeEraser;
    private float mX, mY;
    private Canvas mCanvas;
    private float  DEFFERENCE_SPACE = 4;
    private ArrayList<Bitmap> listAction = new ArrayList<>();
    private int leftImage = 50, topImage =50;
    public static boolean toMove = false;
    private boolean toResize = false;
    private float refX, refY;
    private int xCenter,yCenter;
    private float xRotate, yRotate;
    private int angle = 0;

    private SurfaceHolder holder;
    private Thread drawThread;
    private boolean surfaceReady = false;
    private boolean drawingActive = false;
    private static final int MAX_FRAME_TIME = (int) (1000.0/60.0);
    private static final String LOGTAG = "Surface view";


    public PaintSurfaceView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        init();
    }

    private void init() {

        holder = getHolder();
        holder.addCallback(this);

        sizeEraser = sizeBrush = 5;
        colorBackground = Color.WHITE;

        mPaint.setColor(Color.BLACK);
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeWidth(toPx(sizeBrush));
        // because it is xml
        Drawable drawable = getResources().getDrawable(R.drawable.ic_rotate_right_black_24dp);
        rotateImage = drawableToBitmap(drawable);
        //because it is png
        captureImage = BitmapFactory.decodeResource(getResources(), R.drawable.capture);
    }

    private Bitmap drawableToBitmap(Drawable drawable) {
        if(drawable instanceof BitmapDrawable){
            return ((BitmapDrawable)drawable).getBitmap();
        }
        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),drawable.getIntrinsicHeight(),Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bitmap);
        drawable.setBounds(0,0,c.getWidth(),c.getHeight());
        drawable.draw(c);

        return bitmap;
    }

    private float toPx(int sizeBrush) {
        return sizeBrush*(getResources().getDisplayMetrics().density);

    }
    /*
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh){
        super.onSizeChanged(w ,h, oldw, oldh);

        btmBackground =Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);
        btmView = Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);
        mCanvas = new Canvas(btmView);

    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.drawColor(colorBackground);
        canvas.drawBitmap(btmBackground,0,0,null);

        if(image != null && toMove) {
            drawImage(canvas);
            xCenter = leftImage + image.getWidth() / 2 - captureImage.getWidth() / 2;
            yCenter = topImage + image.getHeight() / 2 - captureImage.getHeight() / 2;

            xRotate = leftImage + image.getWidth() + toPx(5);
            yRotate = topImage + toPx(5);
            canvas.drawBitmap(captureImage, xCenter, yCenter, null);
        }
        canvas.drawBitmap(btmView, 0, 0, null);

    }
*/

    private void drawImage(Canvas canvas) {
        Matrix matrix = new Matrix();
        matrix.setRotate(angle, image.getWidth()/2, image.getHeight()/2);
        matrix.postTranslate(leftImage,topImage);
        canvas.drawBitmap(image,matrix,null);
    }

    public void setColorBackground(int color){
        colorBackground = color;
        invalidate();
    }
    public void setSizeBrush(int s)
    {
        sizeBrush = s;
        mPaint.setStrokeWidth(toPx(sizeBrush));
    }
    public void setBrushColor(int color){
        mPaint.setColor(color);
    }
    public void setSizeEraser(int s){
        mPaint.setStrokeWidth(toPx(sizeEraser));
    }
    public void enableEraser(){
        mPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
    }
    public void disableEraser(){
        mPaint.setXfermode(null);
        mPaint.setShader(null);
        mPaint.setMaskFilter(null);
    }

    public void addLastAction(Bitmap bitmap){
        listAction.add(bitmap);
    }
    public void returnLastAction(){
        if(listAction.size() > 0)
        {
            listAction.remove(listAction.size() -1);
            if(listAction.size() >=0)
            {
                btmView = listAction.get(listAction.size()-1);
            }else{
                btmView = Bitmap.createBitmap(getWidth(),getHeight(),Bitmap.Config.ARGB_8888);
            }
            mCanvas = new Canvas(btmView);

            invalidate();
        }
    }
    @Override
    public boolean onTouchEvent (MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()){

            case MotionEvent.ACTION_DOWN:
                touchStart(x,y);
                refX = x;
                refY = y;

                if(toMove){

                    if(isToResize(refX,refY)){
                        toResize = true;
                    }else
                    {
                        toResize = false;
                    }


                    if((refX >= xCenter && refX< xCenter + captureImage.getWidth())
                        &&(refY >= yCenter && refY < yCenter + captureImage.getHeight())){

                        Canvas newCanvas = new Canvas(btmBackground);
                        drawImage(newCanvas);
                        invalidate();
                    }

                    if((refX >= xRotate && refX <= xRotate+rotateImage.getWidth())
                    && (refY >= yRotate && refY <= yRotate + rotateImage.getHeight())){
                        angle += 10;
                        invalidate();
                    }


                }

                break;
            case MotionEvent.ACTION_MOVE:
                if(!toMove)
                    touchMove(x,y);
                else {
                    float nX = event.getX();
                    float nY = event.getY();


                    if(toResize){
                        int xScale = 0;
                        int yScale = 0;
                        if(nX > refX) {
                            xScale = (int) (image.getWidth() + (nX-refX));
                        }else{
                            xScale = (int) (image.getWidth() + (refX - nX));
                        }

                        if(nY > refY) {
                            yScale = (int) (image.getHeight() + (nY - refY));
                        }else{
                            yScale = (int) (image.getHeight() + (refY - nY));
                        }
                        if(xScale >0 && yScale >0)
                            image = Bitmap.createScaledBitmap(originalImage,xScale,yScale, false);

                    }else {

                        leftImage += nX - refX;
                        topImage += nY - refY;
                    }
                    refX = nX;
                    refY = nY;
                    invalidate();
                }
                break;
            case MotionEvent.ACTION_UP:
                if(!toMove) {
                    touchUp();
                    addLastAction(getBitmap());
                }
                break;


        }
        return true;
    }

    private boolean isToResize(float refX, float refY) {
        if((refX >= leftImage && refX < leftImage + image.getWidth()
                &&((refY >=topImage && refY<=topImage +20) || (refY >= topImage + image.getHeight() - 20 && refY <= topImage + image.getHeight())))) {
            return true;
        }
        return false;
    }

    private void touchUp() {
        mPath.reset();
    }

    private void touchMove(float x, float y) {
        float dx = Math.abs(x-mX);
        float dy = Math.abs(y-mY);

        if(dx >= DEFFERENCE_SPACE || dy >= DEFFERENCE_SPACE) {
            mPath.quadTo(x, y, (x + mX) / 2, (y + mY) / 2);

            mY = y;
            mX = x;
            mCanvas.drawPath(mPath, mPaint);
            invalidate();
        }
    }

    private void touchStart(float x, float y) {
        mPath.moveTo(x,y);
        mX = x;
        mY = y;
    }

    public Bitmap getBitmap(){
        Bitmap bitmap = Bitmap.createBitmap(btmBackground);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(colorBackground);
        canvas.drawBitmap(btmBackground, 0 ,0 ,null);
        canvas.drawBitmap(btmView,0,0,null);
        return bitmap;

    }

    public void setImage(Bitmap bitmap) {
        toMove = true;
        image = Bitmap.createScaledBitmap(bitmap, getWidth() / 2, getHeight() / 2, true);
        originalImage = image;
        invalidate();
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        this.holder = surfaceHolder;
        if(drawThread != null) {
            drawingActive = false;
            try{
                drawThread.join();
            }catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        surfaceReady = true;
        startDrawThread();


    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int w, int h){
        if(w == 0 | h == 0){
            return;
        }

        btmBackground =Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);
        btmView = Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);
        mCanvas = new Canvas(btmView);
    }

    public void startDrawThread() {
        if(surfaceReady && drawThread == null){
            drawThread = new Thread(this,"Draw Thread");
            drawingActive = true;
            drawThread.start();
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        stopDrawThread();
        surfaceHolder.getSurface().release();
        this.holder = null;
        surfaceReady = false;

    }

    public void stopDrawThread()  {
        if(drawThread == null){
            Log.d(LOGTAG,"DrawThread is null");
            return;
        }
        drawingActive = false;
        while(true){
            try {
                Log.d(LOGTAG,"Request last frame");
                drawThread.join(5000);
                break;
            }catch(InterruptedException e){
                Log.e(LOGTAG,"Couldnot draw with draw thread");

            }
        }

        drawThread = null;
    }

    @Override
    public void run() {
        Log.d(LOGTAG,"Draw Thread started");
        long frameStartTime;
        long frameTime;
        if(Build.BRAND.equalsIgnoreCase("google") && Build.MANUFACTURER.equalsIgnoreCase("asus")
            && Build.MODEL.equalsIgnoreCase("OnePlus 7")){

            Log.w(LOGTAG,"Sleep 500ms (Device : OnePLUS 7");
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }

        try{

            while(drawingActive){
                if(holder == null){
                    return;
                }
                frameStartTime = System.nanoTime();
                Canvas canvas = holder.lockCanvas();

                if(null != canvas){

                    try{

                        canvas.drawColor(colorBackground);
                        canvas.drawBitmap(btmBackground,0,0,null);

                        if(image != null && toMove) {
                            drawImage(canvas);
                            xCenter = leftImage + image.getWidth() / 2 - captureImage.getWidth() / 2;
                            yCenter = topImage + image.getHeight() / 2 - captureImage.getHeight() / 2;

                            xRotate = leftImage + image.getWidth() + toPx(5);
                            yRotate = topImage + toPx(5);
                            canvas.drawBitmap(captureImage, xCenter, yCenter, null);
                        }
                        canvas.drawBitmap(btmView, 0, 0, null);


                    }finally {
                        holder.unlockCanvasAndPost(canvas);
                    }
                }

                frameTime = (System.nanoTime() - frameStartTime)/1000000;
                if(frameTime == MAX_FRAME_TIME){
                    try{
                        Thread.sleep(MAX_FRAME_TIME - frameStartTime);
                    }catch (Exception e){

                    }
                }
            }

            Log.d(LOGTAG,"Draw thread finished");
        }catch (Exception e){
            Log.w(LOGTAG,"Exception while unlocking | looking");
        }

    }
}
