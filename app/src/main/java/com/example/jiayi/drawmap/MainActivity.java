package com.example.jiayi.drawmap;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ImageView image;
    // 画笔
    private Paint paint;
    private Paint paint_test;
    // 画布
    private Canvas canvas;
    // 缩放后的图片
    private Bitmap bitmap;
    // 缩放后的图片副本
    private Bitmap copyBitmap;

    private Button choose;
    private Button save;
    private Button start;
    private Button reset;
    private EditText userHeight;
    private EditText fileName;
    private TextView point;
    private TextView screen;
    private TextView bitmapXY;
    private TextView rotationXYZ;
    private TextView stepLength;
    private final static int RESULT = 0;

    private float downx = 0;
    private float downy = 0;
    private float x = 0;
    private float y = 0;
    private float width;
    private float height;
    private float scale;

    boolean walkStraight = true;
    //Store data
    private String filename;


    //----------------------------------------
    //Sensor
    private SensorManager mSensorManager;
    private float[] smoothed = new float[3];

    //step sensor
    private Sensor stepSensor;
    private float step_old;
    private float step_new;
    private float step_temp;
    private double dStep = 0.70;
    //test
    private double dStep_h;

    //Acc-Mag-orientation
    private Sensor accelerometer; // 加速度传感器
    private Sensor magnetic; // 地磁场传感器
    private float[] accelerometerValues = new float[3];
    private float[] magneticFieldValues = new float[3];
    // Rotation data
    private float[] rotation = new float[9];
    private float[] inclineMatrix = new float[9];
    private double mInclination;
    // orientation (azimuth, pitch, roll)
    private float[] orientation = new float[3];
    private float heading;
    private float heading_Initial;
    private float heading_Refine;
    static final float ALPHA = 0.25f; // if ALPHA = 1 OR 0, no filter applies.
    private int count = 1;
    //test
    private float heading_Refine_test;
    private double z_degree_last;

    // Gravity Sensor
    private Sensor GAccelerator;
    float gx = 0;
    float gy = 0;
    float gz = 0;
    float Lax=0, Lay=0,Laz=0;
    float M_ax=0, M_ay=0,M_az=0;
    double rad_x=0,cos_x=0;
    double rad_y=0,cos_y=0;
    double rad_z=0,cos_z=0;
    float[] gravity = {0,0,0};

   //	private Sensor mGyroscope;
    private Sensor mGyroscope;
    private long lasttimestamp = 0;
    private double X_max=0, Y_max=0, Z_max=0;
    private float timestamp;
    float dT;
    private static final double C= 180 / Math.PI / 1000000000;
    private static final float NS2S = 1.0f / 1000000000.0f;
    double Rt=0,EPSILON=0.1,VS=0;
    private double degree_X = 0 , offset_X = 0 ;
    private double degree_Y = 0 , offset_Y = 0 ;
    private double degree_Z = 0 , offset_Z = 0 ;
    private final float[] deltaRotationVector = new float[4];
    private float axisX,axisY,axisZ;
    private double axis_z_degree;
    private double axis_z_degree_Initial;

    //

    //Kalman Filter
    private static float Q_angle  =  (float)0.01;
    private static float Q_gyro   =  (float)0.0003;
    private static float R_angle  =  (float)0.01;
    private static float x_bias = 0;
    private static float y_bias = 0;
    private static float XP_00 = 0, XP_01 = 0, XP_10 = 0, XP_11 = 0;
    private static float YP_00 = 0, YP_01 = 0, YP_10 = 0, YP_11 = 0;
    private static float KFangleX = (float)0.0;
    private static float KFangleY = (float)0.0;

    //Complementary
    private double tau=0.075;
    private double a = 0.0;
    private float x_angleC = 0;

    //Heading
    boolean fixed = false;
    float gyo_degree = 0;
    float gyo_degree_last = 0;


    //Calculate new location
    private double location_x;
    private double location_y;
    private double location_x_last;
    private double location_y_last;
    private final double PI = Math.PI/180;
    //test
    private double location_x_h;
    private double location_y_h;
    //
    private Thread trackThread;
    private Handler handler;
    private boolean isRunning = true;
    private double distance = 0.0;
    //test
    private double distance_h = 0.0;

    //getGeoNorthDeclination()
    private LocationManager locManager;
    private float mag_declination;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Sensor
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        stepSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetic = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        mGyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        GAccelerator = mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
        if(GAccelerator==null){
            Toast.makeText(this, "您的设备不支持重力计！",Toast.LENGTH_LONG).show();
        }
        // 获取id
        image = (ImageView) findViewById(R.id.image);
        choose = (Button) findViewById(R.id.chooseButton);
        save = (Button) findViewById(R.id.saveButton);
        start = (Button) findViewById(R.id.startButton);
        reset = (Button) findViewById(R.id.resetButton);
        point = (TextView) findViewById(R.id.point);
        screen = (TextView) findViewById(R.id.screen);
        bitmapXY = (TextView) findViewById(R.id.bitmap);
        rotationXYZ = (TextView) findViewById(R.id.rotation);
        userHeight = (EditText) findViewById(R.id.userHeight);
        fileName = (EditText) findViewById(R.id.fileName);
        stepLength = (TextView) findViewById(R.id.stepLength);
    }

    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        // 注册监听
        super.onResume();
        step_temp = 0;
        step_new = step_old = 0;
        lasttimestamp = 0;
        degree_X = 0 ;
        degree_Y = 0 ;
        degree_Z = 0 ;
        calScale();
        mSensorManager.registerListener(new MySensorEventListener(),
                stepSensor, SensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener(new MySensorEventListener(),
                accelerometer, SensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener(new MySensorEventListener(),
                magnetic, SensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener(new MySensorEventListener(),
                mGyroscope, SensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener(new MySensorEventListener(),
                GAccelerator, SensorManager.SENSOR_DELAY_NORMAL);


        locManager=(LocationManager)getSystemService(Context.LOCATION_SERVICE);

        if(!locManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
            Toast.makeText(this, "请开启GPS导航...", Toast.LENGTH_SHORT).show();
            //返回开启GPS导航设置界面
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivityForResult(intent,0);
            return;
        }
    }

    @Override protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(new MySensorEventListener());
    }

    public void start(View view){
        //step_new = 0;
        double height =  Double.parseDouble(userHeight.getText().toString());

        dStep_h = (height - 155.911)/0.262/100;
        BigDecimal sl = new BigDecimal(dStep_h);


        userHeight.setVisibility(View.GONE);
        //stepLength.setText("SL:0.75  SL_H:" + sl.setScale(2, BigDecimal.ROUND_DOWN));

        //dStep = height;
        filename = fileName.getText().toString();

        location_x_last = location_x = downx;
        location_y_last = location_y = downy;
        location_x_h = location_x;
        location_y_h = location_y;
        heading_Initial = (float)height;
        z_degree_last = axis_z_degree_Initial = axis_z_degree;

        SimpleDateFormat sDateFormat = new SimpleDateFormat("yyyy-MM-dd    hh:mm:ss");
        String date = sDateFormat.format(new java.util.Date()) + "\n";
        write(date);
        isRunning = true;
        mag_declination = getGeoNorthDeclination();;
//        heading_Initial += mag_declination;
//        heading_Refine_test = heading_Initial;
        //rotationXYZ.setText(String.format("磁偏角：%7.3f", (mag_declination)));

        setupDetectorTimestampUpdaterThread();
    }

    private void setupDetectorTimestampUpdaterThread() {
        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                BigDecimal x = new BigDecimal(location_x);
                BigDecimal y = new BigDecimal(location_y);
                //test
//                BigDecimal x_h = new BigDecimal(location_x_h);
//                BigDecimal y_h = new BigDecimal(location_y_h);

                //rotationXYZ.setText("axis_z_degree:" + axis_z_degree);
                if(walkStraight == true) stepLength.setText("正在直行");
                else stepLength.setText("正在转弯");

//                paint.setColor(Color.RED);
//                canvas.drawPoint((float) location_x_h, (float) location_y_h, paint);

                //point.setText("loactionX: " + location_x + " loactionY: " + location_y);
                point.setText("loactionX: " + x.setScale(2, BigDecimal.ROUND_DOWN) + " loactionY: " + y.setScale(2, BigDecimal.ROUND_DOWN));
                //screen.setText("heading:" + heading + "\norientation:" + heading_Refine + "\ncomplementary:" + heading_Refine_test);
                screen.setText("orientation:" + heading_Refine);
                paint.setColor(Color.GREEN);
                canvas.drawPoint((float) location_x, (float) location_y, paint);
                image.invalidate();

                String input;
                input = "loactionX: " + x.setScale(2,BigDecimal.ROUND_DOWN) + " loactionY: " + y.setScale(2, BigDecimal.ROUND_DOWN)+"\n";
                write(input);

            }
        };

        trackThread = new Thread() {
            @Override
            public void run() {
                while (isRunning) {
                    try {
                        Thread.sleep(1000);

                        distance = (step_new - step_temp) * dStep;
                        distance = distance * scale;
                        // test
//                        distance_h = (step_new - step_temp) * dStep_h;
//                        distance_h = distance_h * scale;
                        step_temp = step_new;


                        if(gyo_degree_last == 0) {
                            gyo_degree_last = gyo_degree = (float) (axis_z_degree - axis_z_degree_Initial);
                        }
                        else {
                            gyo_degree = (float) (axis_z_degree - axis_z_degree_Initial);
                            if(abs(gyo_degree_last - gyo_degree) < 5){
                                gyo_degree = gyo_degree_last;
                                walkStraight = true;
                            }else{
                                walkStraight = false;
                            }
                        }
                        heading_Refine = normalAngle(heading_Initial - normalAngle(gyo_degree));
                        calculateLatitude(location_x, location_y, distance, heading_Refine);
                        gyo_degree_last = gyo_degree;


                        //test height
//                        float gyo_degree_interval = (float) (axis_z_degree - z_degree_last);
//                        System.out.println("gyo_degree_interval:" + gyo_degree_interval);
//                        System.out.println("heading_Refine_test:" + heading_Initial);
//                        float heading_diff = heading - heading_Refine_test;
//                        System.out.println("heading_diff:" + heading_diff );
//                        System.out.println("---------------------");
//                        if (gyo_degree_interval < 0 ){
//                            if(heading_diff < -180)
//                                heading_Refine_test += complementary(gyo_degree_interval, normalAngle(heading_diff));
//                            else
//                                heading_Refine_test += complementary(gyo_degree_interval, heading_diff);
//                        }else{
//                            if(heading_diff < 0) {
//                                if(heading_diff < -180)
//                                    heading_Refine_test += complementary(gyo_degree_interval, normalAngle(heading_diff));
//                                else
//                                    heading_Refine_test += complementary(gyo_degree_interval, heading_diff);
//                            }
//                            else
//                                 heading_Refine_test -= gyo_degree_interval;
//                        }
//
//                        heading_Refine_test = normalAngle(heading_Refine_test);
//                        calculateLatitude_h(location_x_h, location_y_h, distance, heading_Refine_test);
//                        z_degree_last = axis_z_degree;

                        handler.sendEmptyMessage(0);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }

        };
        trackThread.start();
    }

    float complementary( float gyo,float mag) {
        float newAngle = (float)(-0.92 *(gyo) + 0.08*mag);
        return newAngle ;
    }



    public void reset(View view){
        step_old = step_new;
        bitmapXY.setText("Steps: " + step_new);
        // 创建缩放后的图片副本
        copyBitmap = Bitmap.createBitmap(bitmap.getWidth(),
                bitmap.getHeight(), bitmap.getConfig());
        //bitmapXY.setText("bitmapX:"+bitmap.getWidth()+"  bitmapY:"+bitmap.getHeight());
        // 创建画布
        canvas = new Canvas(copyBitmap);
        // 开始作画，把原图的内容绘制在白纸上
        canvas.drawBitmap(bitmap, new Matrix(), paint);
        // 将处理后的图片放入imageview中
        image.setImageBitmap(copyBitmap);
        // 设置imageview监听
        image.setOnTouchListener(new MyTouchListener());
    }

    class MySensorEventListener implements SensorEventListener {
        @Override
        public void onSensorChanged(SensorEvent event) {
            // TODO Auto-generated method stub

            if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
                if(step_old == 0) {
                    step_new = step_temp = step_old = event.values[0];
                    bitmapXY.setText("Steps: " + 0);
                }else {
                    step_new = event.values[0];
                    bitmapXY.setText("Steps: " + (step_new - step_old));
                }
            }
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER){
                accelerometerValues = lowPass(event.values.clone(), accelerometerValues);
                M_ax = accelerometerValues[0];
                M_ay = accelerometerValues[1];
                M_az = accelerometerValues[2];
                //accelerometerValues = event.values;
            }
            if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD){
                magneticFieldValues = lowPass(event.values.clone(), magneticFieldValues);
                //magneticFieldValues = event.values;
            }
            if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE){
                if( abs(event.values[0]) > abs(X_max)){
                    X_max = event.values[0];
                }

                if( abs(event.values[1]) > abs(Y_max)){
                    Y_max = event.values[1];
                }

                if( abs(event.values[2]) > abs(Z_max)){
                    Z_max = event.values[2];
                }
                if (timestamp != 0) {
                    dT = (event.timestamp - timestamp) * NS2S;
                    axisX = event.values[0];
                    axisY = event.values[1];
                    axisZ = event.values[2];

//                     timestamp = event.timestamp;

                    // Calculate the angular speed of the sample
                    float omegaMagnitude = (float) Math.sqrt(axisX*axisX + axisY*axisY + axisZ*axisZ);

                    // Normalize the rotation vector if it's big enough to get the axis
                    // (that is, EPSILON should represent your maximum allowable margin of error)
                    if (omegaMagnitude > EPSILON) {

                        degree_X += axisX * dT;
                        degree_Y += axisY * dT;
                        degree_Z += axisZ * dT;

                        //	degree_X = 180*deltaRotationVector[0]/Math.PI;
                    }
                }
                timestamp = event.timestamp;
                axis_z_degree = 180*degree_Z/Math.PI;
                //rotationXYZ.setText(" Z:"+String.valueOf(axis_z_degree) );
            }
            if(event.sensor.getType() == Sensor.TYPE_GRAVITY){


                float[] values = event.values;
//                Lax = values[0];
//                Lay = values[1];
//                Laz = values[2];
                gravity[0] = values[0];
                gravity[1] = values[1];
                gravity[2] = values[2];
//
                final float alpha = (float) 0.8;
                gravity[0] = alpha * gravity[0] + (1 - alpha) * M_ax;
                gravity[1] = alpha * gravity[1] + (1 - alpha) * M_ay;
                gravity[2] = alpha * gravity[2] + (1 - alpha) * M_az;
                Lax = gravity[0];
                Lay = gravity[1];
                Laz = gravity[2];

//                gx=M_ax-Lax;
//                gy=M_ay-Lay;
//                gz=M_az-Laz;
                //	Rt=Math.sqrt(Lax*Lax+Lay*Lay+Laz*Laz);

                if(Laz>0){
                    Rt=Math.sqrt(Lax*Lax+Lay*Lay+Laz*Laz);
                }else{
                    Rt=-Math.sqrt(Lax*Lax+Lay*Lay+Laz*Laz);
                }

                VS += Rt*dT;
                timestamp = event.timestamp;

//                rotationXYZ.setText("X="+String.valueOf(Lax)+"   "+ String.valueOf(M_ax)
//                                    +"\nY="+String.valueOf(Lay)+"   "+ String.valueOf(M_ay)
//                                    +"\nZ="+String.valueOf(Laz)+"   "+ String.valueOf(M_az)
//                                    +"\nR="+String.valueOf(VS));

            }

            if(SensorManager.getRotationMatrix(rotation, null, accelerometerValues,
                    magneticFieldValues)) {
                SensorManager.getOrientation(rotation, orientation);
                mInclination = SensorManager.getInclination(inclineMatrix);
                if (count++ % 100 == 0) {
                    heading = normalAngle((float) Math.toDegrees(orientation[0]));
//                    screen.setText("heading:" + normalAngle(heading));
                    count = 1;
                }
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // TODO Auto-generated method stub

        }
    }

    private void calculateLatitude(double x,double y,double s,float q){
        q = normalAngle(q);
        //System.out.println("q:" + q + "  s:" + s);
        if (q>=0 &&q <90) {
            x = x + s * Math.sin(q * PI);
            y = y - s * Math.cos(q * PI);
        }else if(q>=90 && q<180){
            x = x + s * Math.cos((q - 90) * PI);
            y = y + s * Math.sin((q - 90) * PI);
        }else if(q>=180 && q<270){
            x = x - s * Math.sin((q - 180) * PI);
            y = y + s * Math.cos((q - 180) * PI);
        }else if(q>=270 && q<=360){
            x = x - s * Math.cos((q - 270) * PI);
            y = y - s * Math.sin((q - 270) * PI);
        }
        location_x = x;
        location_y = y;
        //System.out.println("loactionX: " + location_x + " loactionY: " + location_y);
    }

    private void calculateLatitude_h(double x,double y,double s,float q){
        q = normalAngle(q);
        //System.out.println("q:" + q + "  s:" + s);
        if (q>=0 &&q <90) {
            x = x + s * Math.sin(q * PI);
            y = y - s * Math.cos(q * PI);
        }else if(q>=90 && q<180){
            x = x + s * Math.cos((q - 90) * PI);
            y = y + s * Math.sin((q - 90) * PI);
        }else if(q>=180 && q<270){
            x = x - s * Math.sin((q - 180) * PI);
            y = y + s * Math.cos((q - 180) * PI);
        }else if(q>=270 && q<=360){
            x = x - s * Math.cos((q - 270) * PI);
            y = y - s * Math.sin((q - 270) * PI);
        }
        location_x_h = x;
        location_y_h = y;
        //System.out.println("downx: " + downx + " downy: " + downy);
        //System.out.println("loactionX: " + location_x + " loactionY: " + location_y);
    }


    float kalmanFilterY(float DT, float accAngle, float gyroRate)
    {
        float  y, S;
        float K_0, K_1;

        KFangleY += DT * (gyroRate - y_bias);

        YP_00 +=  - DT * (YP_10 + YP_01) + Q_angle * DT;
        YP_01 +=  - DT * YP_11;
        YP_10 +=  - DT * YP_11;
        YP_11 +=  + Q_gyro * DT;

        y = accAngle - KFangleY;
        S = YP_00 + R_angle;
        K_0 = YP_00 / S;
        K_1 = YP_10 / S;

        KFangleY +=  K_0 * y;
        y_bias  +=  K_1 * y;
        YP_00 -= K_0 * YP_00;
        YP_01 -= K_0 * YP_01;
        YP_10 -= K_1 * YP_00;
        YP_11 -= K_1 * YP_01;

        return KFangleY;
    }

    //LowPass Filter
    protected float[] lowPass( float[] input, float[] output ) {
        if ( output == null ) return input;

        for ( int i=0; i<input.length; i++ ) {
            output[i] = output[i] + ALPHA * (input[i] - output[i]);
        }
        return output;
    }

    public class MyTouchListener implements View.OnTouchListener {

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            int action = event.getAction();
            switch (action) {
                // 按下
                case MotionEvent.ACTION_DOWN:
                    downx = event.getX();
                    downy = event.getY();
                    canvas.drawPoint(downx, downy, paint);
                    point.setText("X:" + downx + "  Y:" + downy);
                    image.invalidate();
                    break;
                // 移动
                case MotionEvent.ACTION_MOVE:
                    // 路径画板
                    x = event.getX();
                    y = event.getY();
                    // 画线
                    canvas.drawLine(downx, downy, x, y, paint);
                    // 刷新image
                    image.invalidate();
                    downx = x;
                    downy = y;
                    break;
                case MotionEvent.ACTION_UP:
                    break;

                default:
                    break;
            }
            // true：告诉系统，这个触摸事件由我来处理
            // false：告诉系统，这个触摸事件我不处理，这时系统会把触摸事件传递给imageview的父节点
            return true;
        }

    }

    // 选择图片
    public void choose(View view) {
        // 进入图库
        Intent intent = new Intent(Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

        startActivityForResult(intent, RESULT);
    }

    // 保存图片
    public void save(View view) {
        // 保存画好的图片
        if (copyBitmap != null) {
            try {

                // 获取图库Uri路径
                Uri imageUri = getContentResolver().insert(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI, new ContentValues());
                // 获取输出流
                OutputStream outputStream = getContentResolver()
                        .openOutputStream(imageUri);
                // 将alterBitmap存入图库
                copyBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                //getApplicationContext()获取应用Activity的context
                Toast.makeText(getApplicationContext(), "保存成功!",
                        Toast.LENGTH_SHORT).show();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
        //canvas.drawLine(0, 0, 200, 200, paint);
    }

    @Override
    // 从图库中选取图片后返回
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // TODO Auto-generated method stub
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            // 获取选中的图片的Uri
            Uri imageFileUri = data.getData();
            // 获取屏幕大小
            Display display = getWindowManager().getDefaultDisplay();
            float dw = display.getWidth();
            float dh = display.getHeight();
            //screen.setText("screenX:"+dw+"  screenY:"+dh);

            try {
                // 解析图片时需要使用到的参数都封装在这个对象里了
                BitmapFactory.Options options = new BitmapFactory.Options();
                // 不为像素申请内存，只获取图片宽高
                options.inJustDecodeBounds = true;
                bitmap = BitmapFactory.decodeStream(getContentResolver()
                        .openInputStream(imageFileUri), null, options);
                // 设置缩放比例
                int heightRatio = (int) Math.ceil(options.outHeight / dh);
                int widthRatio = (int) Math.ceil(options.outWidth / dw);

                if (heightRatio > 1 && widthRatio > 1) {
                    if (heightRatio > widthRatio) {
                        options.inSampleSize = heightRatio;
                    } else {
                        options.inSampleSize = widthRatio;
                    }
                }

                width = options.outWidth;
                height = options.outHeight;
                point.setText("X:" + options.outWidth + "  Y:" + options.outHeight);

                // 为像素申请内存
                options.inJustDecodeBounds = false;
                // 获取缩放后的图片
                bitmap = BitmapFactory.decodeStream(getContentResolver()
                        .openInputStream(imageFileUri), null, options);
                // 创建缩放后的图片副本
                copyBitmap = Bitmap.createBitmap(bitmap.getWidth(),
                        bitmap.getHeight(), bitmap.getConfig());
                //bitmapXY.setText("bitmapX:"+bitmap.getWidth()+"  bitmapY:"+bitmap.getHeight());
                // 创建画布
                canvas = new Canvas(copyBitmap);
                // 创建画笔
                paint = new Paint();
                paint_test = new Paint();
                // 设置画笔颜色
                paint.setColor(Color.GREEN);
                paint_test.setColor(Color.RED);
                // 设置画笔宽度
                paint.setStrokeWidth(10);
                paint_test.setStrokeWidth(10);
                // 开始作画，把原图的内容绘制在白纸上
                canvas.drawBitmap(bitmap, new Matrix(), paint);
                // 将处理后的图片放入imageview中
                image.setImageBitmap(copyBitmap);
                // 设置imageview监听
                image.setOnTouchListener(new MyTouchListener());
            } catch (FileNotFoundException e) {

                e.printStackTrace();
            }

        }
    }

    public float getGeoNorthDeclination(){
        String providerName = locManager.getBestProvider(new Criteria(), true);
        Location loc = locManager.getLastKnownLocation(providerName);
        //如果我们开启了gps，通常优选为gps，但是室内实际上很难马上获得gps，我们可以通过位置改变监听器的方式获取，为了方便，本例我们将改用network的基站三角定位的方式获得。
        if(loc == null && locManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)){
            //在室内，由于不容易搜索到GPS，建议采用network方式。请注意，有些设备要在配置那里打开网络定位的选项，否则，network方式不能enabled，不能有效使用网络方式，getLastKnownLocation()仍会为null。正规的应用发现disabled，应该询问用户，并通过intent打开相关的配置页。
            loc = locManager.getLastKnownLocation( LocationManager.NETWORK_PROVIDER );
        }

        if(loc == null)
            return 0;

        GeomagneticField geo = new GeomagneticField((float)loc.getLatitude(),(float)loc.getLongitude(),
                (float)loc.getAltitude(),System.currentTimeMillis());
        float declination = geo.getDeclination();
        return  declination;
    }

    public void write(String msg){
        // 步骤1：获取输入值
        if(msg == null) return;
        FileOutputStream outputStream;
        try {
            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                File sdCardDir = Environment.getExternalStorageDirectory();//获取SDCard目录
                File sdFile = new File(sdCardDir, "/project/"+ filename + ".txt");
                //FileOutputStream fos = new FileOutputStream(sdFile);
                BufferedWriter bw = new BufferedWriter(new FileWriter(sdFile, true));
                bw.write(msg);// 写入
                bw.close(); // 关闭输出流
            } else {
                Toast.makeText(getApplicationContext(),"数据写入失败", Toast.LENGTH_SHORT).show();
            }
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    public double abs(double val){
        if(val < 0) return -val;
        return val;
    }

    public float normalAngle(float angle){
        if(angle<0){
            while(angle<0) angle+=360;
        }
        if(angle>=360){
            while(angle>=360) angle-=360;
        }
        return angle;
    }

    public void calScale(){
        //scale = height / (float)61.5;
         scale = height / (float)52;
    }
}
