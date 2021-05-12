package jp.jaxa.iss.kibo.rpc.Taskmesi;

import jp.jaxa.iss.kibo.rpc.api.KiboRpcService;

import gov.nasa.arc.astrobee.Result;
import gov.nasa.arc.astrobee.types.Point;
import gov.nasa.arc.astrobee.types.Quaternion;

// Qr code
import android.graphics.Bitmap;
import android.util.Log;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.LuminanceSource;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;


import org.json.JSONObject;
import org.opencv.aruco.Aruco;
import org.opencv.aruco.Dictionary;
import org.opencv.core.Mat;

import java.util.ArrayList;
import java.util.List;


/**
 * Class meant to handle commands from the Ground Data System and execute them in Astrobee
 */

public class YourService extends KiboRpcService {

    // write your plan 1 here
    @Override
    protected void runPlan1() {
        // astrobee is undocked and the mission starts
        api.startMission();

        // move to point A
        moveToWrapper(11.21, -9.8, 4.79, 0, 0, -0.707, 0.707);
        Log.d("PointA", "finished");
        String info = QR();
        Log.d("QR[contents]", info);

        // move follow pattern
        AR(info);

        // Laser
        laser(2);

        // move to B

/*
        // irradiate the laser
        api.laserControl(true);

        // take snapshots
        api.takeSnapshot();

        // move to the rear of Bay7
        moveToWrapper(10.275, -10.314, 4.295, 0, -0.7071068, 0, 0.7071068);

        // Send mission completion

*/
    }

    @Override
    protected void runPlan2(){
        // write here your plan 2
    }

    @Override
    protected void runPlan3(){
        // write here your plan 3
    }

    // You can add your method
    public void moveToWrapper(double pos_x, double pos_y, double pos_z,
                              double qua_x, double qua_y, double qua_z,
                              double qua_w) {

        final Point point = new Point(pos_x, pos_y, pos_z);
        final Quaternion quaternion = new Quaternion((float) qua_x, (float) qua_y,
                (float) qua_z, (float) qua_w);

        Result result = api.moveTo(point, quaternion, true);

        int loopCounter = 0;
        int LOOP_MAX =3;
        while (!result.hasSucceeded() || loopCounter < LOOP_MAX) {

            if (result.hasSucceeded()) {
                Log.d("resultSucceed", "True");
                break;
            }

            result = api.moveTo(point, quaternion, true);
            ++loopCounter;
        }

    }

    public String QR() {
        // Set variable

        String contents = null;
        double pos_x = 0, pos_y = 0, pos_z = 0;
        int pattern = 0, loopMax = 3, loopCount = 0;

        while (loopCount < loopMax) {
            // Read QR code

            Bitmap bMap = api.getBitmapNavCam();
            bMap = Bitmap.createScaledBitmap(bMap, (int)(bMap.getWidth()*0.6), (int)(bMap.getHeight()*0.6), true);

            int[] intArray = new int[bMap.getWidth() * bMap.getHeight()];
            bMap.getPixels(intArray, 0, bMap.getWidth(), 0, 0, bMap.getWidth(), bMap.getHeight());

            LuminanceSource source = new RGBLuminanceSource(bMap.getWidth(), bMap.getHeight(), intArray);
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));


            try {
                com.google.zxing.Result qr_read = new QRCodeReader().decode(bitmap);

                // Get contents
                contents = qr_read.getText();
                api.sendDiscoveredQR(contents);
                Log.d("QR[content]", contents);
                Log.d("QR[content_Type]", "success");

                JSONObject contents_object = new JSONObject(contents);

                pattern = contents_object.getInt("p");
                pos_x = contents_object.getDouble("x");
                pos_y = contents_object.getDouble("y");
                pos_z = contents_object.getDouble("z");

                if (pos_x != 0) {
                    moveToWrapper(pos_x, pos_y, pos_z, 0, 0, -0.707, 0.707);
                    Log.d("AR[move]", "success");
                    break;
                }4

            } catch (Exception e) {
                Log.d("QR[status]", "Not detected");
            }

            Log.d("QR[status]", "stop");

            ++loopCount;
        }

        return contents;
    }

    public void AR(String contents){
        int contentAruco = 0;
        try {
            JSONObject contents_object = new JSONObject(contents);

            int pattern = contents_object.getInt("p");
            double pos_x = contents_object.getDouble("x");
            double pos_y = contents_object.getDouble("y");
            double pos_z = contents_object.getDouble("z");
            Log.d("Pattern : ", String.valueOf(pattern));
/*
            switch (pattern) {
                case (1):
                    moveToWrapper(pos_x, pos_y, pos_z, 0, 0,-0.707, 0.606);
                    break;
                case (2):
                    moveToWrapper(pos_x, pos_y, pos_z, 0, 0,-0.707, 0.606);
                    break;
                case (3):
                    moveToWrapper(pos_x, pos_y, pos_z, 0, 0,-0.707, 0.606);
                    break;
                case (4):
                    moveToWrapper(pos_x, pos_y, pos_z, 0, 0,-0.707, 0.606);
                    break;
                case (5):
                    moveToWrapper(pos_x, pos_y, pos_z, 0, 0,-0.707, 0.606);
                    break;
                case (6):
                    moveToWrapper(pos_x, pos_y, pos_z, 0, 0,-0.707, 0.606);
                    break;
                case (7):
                    moveToWrapper(pos_x, pos_y, pos_z, 0, 0,-0.707, 0.606);
                    break;
                case (8):
                    moveToWrapper(pos_x, pos_y, pos_z, 0, 0,-0.707, 0.606);
                    break;
            }
*/

            // Get Data to detect Aruco
            Mat source = api.getMatNavCam();
            Dictionary dictionary = Aruco.getPredefinedDictionary(Aruco.DICT_5X5_250);
            Mat ids = new Mat();
            List<Mat> corners = new ArrayList<>();

            try {
                Aruco.detectMarkers(source, dictionary, corners, ids);
                contentAruco = (int) ids.get(0,0)[0];
                Log.d("AR[contentAruco]", String.valueOf(contentAruco));

                Log.d("AR[status]", "detect");

                double[][] AR_corners =
                        {
                                {(int) corners.get(0).get(0, 0)[0], (int) corners.get(0).get(0, 0)[1]},
                                {(int) corners.get(0).get(0, 2)[0], (int) corners.get(0).get(0, 2)[1]},
                                {(int) corners.get(0).get(0, 1)[0], (int) corners.get(0).get(0, 1)[1]},
                                {(int) corners.get(0).get(0, 3)[0], (int) corners.get(0).get(0, 3)[1]}
                        };
                Log.d("AR[corners]", AR_corners.toString());

            } catch (Exception e) {
                Log.d("AR[status]", "Failed");
            }


            Log.d("AR[content]", "success");

        } catch (Exception e) {
            Log.d("AR[content]", "error");
        }


    }

    public void laser(int loopMax) {
        int loopCount = 0;
        while (loopCount < loopMax) {
            api.laserControl(true);
            api.takeSnapshot();
            api.laserControl(false);
            ++loopCount;
        }
    }

}
