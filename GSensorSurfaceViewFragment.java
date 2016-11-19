package com.example.apidemo;

import android.support.v4.app.Fragment;
import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;
import android.view.SurfaceHolder.Callback;

public class GSensorSurfaceViewFragment extends Activity{

    MyView mAnimView = null;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // ȫ����ʾ����
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        //ǿ�ƺ��� 
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        
        // ��ʾ�Զ������ϷView
        mAnimView = new MyView(this);
        setContentView(mAnimView);
    }

    public class MyView extends SurfaceView implements Callback,Runnable ,SensorEventListener{

         /**ÿ50֡ˢ��һ����Ļ**/  
        public static final int TIME_IN_FRAME = 50; 

        /** ��Ϸ���� **/
        Paint mPaint = null;
        Paint mTextPaint = null;
        SurfaceHolder mSurfaceHolder = null;

        /** ������Ϸ����ѭ�� **/
        boolean mRunning = false;

        /** ��Ϸ���� **/
        Canvas mCanvas = null;

        /**������Ϸѭ��**/
        boolean mIsRunning = false;
        
        /**SensorManager������**/
        private SensorManager mSensorMgr = null;    
        Sensor mSensor = null;    
        Sensor mOrientationSensor = null;
        /**�ֻ���Ļ���**/
        int mScreenWidth = 0;
        int mScreenHeight = 0;
        
        /**С����Դ�ļ�Խ������**/
        private int mScreenBallWidth = 0;
        private int mScreenBallHeight = 0;
        
        /**��Ϸ�����ļ�**/
        private Bitmap mbitmapBg;
        
        /**С����Դ�ļ�**/
        private Bitmap mbitmapBall;
        
        /**С�������λ��**/
        private float mPosX = 200;
        private float mPosY = 0;
        
        /**������ӦX�� Y�� Z�������ֵ**/
        private float mGX = 0;
        private float mGY = 0;
        private float mGZ = 0;
        
        float[] rotMat = new float[9];
        float[] vals = new float[3];
        float azimuth = 0;
        float pitch = 0;
        float roll = 0;
        
        public MyView(Context context) {
            super(context);
            /** ���õ�ǰViewӵ�п��ƽ��� **/
            this.setFocusable(true);
            /** ���õ�ǰViewӵ�д����¼� **/
            this.setFocusableInTouchMode(true);
            /** �õ�SurfaceHolder���� **/
            mSurfaceHolder = this.getHolder();
            /** ��mSurfaceHolder��ӵ�Callback�ص������� **/
            mSurfaceHolder.addCallback(this);
            /** �������� **/
            mCanvas = new Canvas();
            mTextPaint = new Paint();
            mTextPaint.setColor(Color.BLUE);
            mTextPaint.setTextSize(41);
            mTextPaint.setAntiAlias(true);
            mTextPaint.setStyle(Style.FILL);
            /** �������߻��� **/
            mPaint = new Paint();
            mPaint.setColor(Color.WHITE);
            /**����С����Դ**/
            mbitmapBall = BitmapFactory.decodeResource(this.getResources(), R.drawable.ball);
            /**������Ϸ����**/
            mbitmapBg = BitmapFactory.decodeResource(this.getResources(), R.drawable.lighthouse);
            
            /**�õ�SensorManager����**/
            mSensorMgr = (SensorManager) getSystemService(SENSOR_SERVICE);   
            mSensor = mSensorMgr.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            mOrientationSensor = mSensorMgr.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
            // ע��listener�������������Ǽ��ľ�ȷ��  
            //SENSOR_DELAY_FASTEST ������ ��Ϊ̫����û��Ҫʹ��
            //SENSOR_DELAY_GAME    ��Ϸ������ʹ��
            //SENSOR_DELAY_NORMAL  �����ٶ�
            //SENSOR_DELAY_UI      �������ٶ�
            mSensorMgr.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
            mSensorMgr.registerListener(this, mOrientationSensor, SensorManager.SENSOR_DELAY_GAME);
        }
        
        private void Draw() {
           
            /**������Ϸ����**/
            mCanvas.drawBitmap(mbitmapBg,0,0, mPaint);
            /**����С��**/
            mCanvas.drawBitmap(mbitmapBall, mPosX,mPosY, mPaint);
            /**X�� Y�� Z�������ֵ**/
            mCanvas.drawText("GX��" + mGX+",GY��" + mGY+",GZ��" + mGZ, 100, 100, mTextPaint);
            mCanvas.drawText("azimuth: "+azimuth+",pitch: "+pitch+",roll: "+roll, 100, 200, mTextPaint);
        }
        
        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width,
                int height) {

        }

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            /**��ʼ��Ϸ��ѭ���߳�**/
            mIsRunning = true;
            new Thread(this).start();
            /**�õ���ǰ��Ļ���**/
            mScreenWidth = this.getWidth();
            mScreenHeight = this.getHeight();
            
            /**�õ�С��Խ������**/
            mScreenBallWidth = mScreenWidth - mbitmapBall.getWidth();
            mScreenBallHeight = mScreenHeight - mbitmapBall.getHeight();
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            mIsRunning = false;
        }

        @Override
        public void run() {
            while (mIsRunning) {

                /** ȡ�ø�����Ϸ֮ǰ��ʱ�� **/
                long startTime = System.currentTimeMillis();

                /** ����������̰߳�ȫ�� **/
                synchronized (mSurfaceHolder) {
                    /** �õ���ǰ���� Ȼ������ **/
                    mCanvas = mSurfaceHolder.lockCanvas();
                    Draw();
                    /** ���ƽ����������ʾ����Ļ�� **/
                    mSurfaceHolder.unlockCanvasAndPost(mCanvas);
                }

                /** ȡ�ø�����Ϸ������ʱ�� **/
                long endTime = System.currentTimeMillis();

                /** �������Ϸһ�θ��µĺ����� **/
                int diffTime = (int) (endTime - startTime);

                /** ȷ��ÿ�θ���ʱ��Ϊ50֡ **/
                while (diffTime <= TIME_IN_FRAME) {
                    diffTime = (int) (System.currentTimeMillis() - startTime);
                    /** �̵߳ȴ� **/
                    Thread.yield();
                }

            }

        }
         
        @Override
        public void onAccuracyChanged(Sensor arg0, int arg1) {
            // TODO Auto-generated method stub
            
        }

        
        private boolean sensorHasChanged = false;
        @Override
        public void onSensorChanged(SensorEvent event) {
        	if(event.sensor == null){
        		return;
        	}
        	
        	switch (event.sensor.getType() ) {
        	case Sensor.TYPE_ACCELEROMETER:
	            mGX = event.values[SensorManager.DATA_X];
	            mGY= event.values[SensorManager.DATA_Y];
	            mGZ = event.values[SensorManager.DATA_Z];
	
	            //�������2��Ϊ����С���ƶ��ĸ���
//	            mPosX -= mGX * 2;
//	            mPosY += mGY * 2;
	
	            //���С���Ƿ񳬳��߽�
	            if (mPosX < 0) {
	                mPosX = 0;
	            } else if (mPosX > mScreenBallWidth) {
	                mPosX = mScreenBallWidth;
	            }
	            if (mPosY < 0) {
	                mPosY = 0;
	            } else if (mPosY > mScreenBallHeight) {
	                mPosY = mScreenBallHeight;
	            }
	            break;
        	case Sensor.TYPE_ROTATION_VECTOR:
        		sensorHasChanged = false;
    	        SensorManager.getRotationMatrixFromVector(rotMat,
    	                event.values);
    	        SensorManager
    	                .remapCoordinateSystem(rotMat,
    	                        SensorManager.AXIS_X, SensorManager.AXIS_Y,
    	                        rotMat);
    	        SensorManager.getOrientation(rotMat, vals);
    	        azimuth = (float) Math.toDegrees(vals[0]); // in degrees [-180, +180]
    	        pitch = (float) Math.toDegrees(vals[1]);
    	        roll = (float) Math.toDegrees(vals[2]);
    	        sensorHasChanged = true;
    	        
    	        mPosX -= pitch;
    	        mPosY -= roll;
        		break;
        	}
        }
    }
}