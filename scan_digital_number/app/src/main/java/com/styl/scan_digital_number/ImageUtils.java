package com.styl.scan_digital_number;

import android.graphics.Bitmap;
import android.graphics.Matrix;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

public class ImageUtils {
    public static final ImageUtils ourInstance = new ImageUtils();

    public static ImageUtils getInstance() {
        return ourInstance;
    }

    private int SIZE_DISTANCE = 8;

    public ImageUtils() {
    }

    public Bitmap convertBitmapToBinary(Bitmap bitmap) {
//        bitmap = convertBitmapToGray(bitmap);
        bitmap = convertBitmapToBlackWhite(bitmap);
        bitmap = fillGapsBitmap(bitmap);
        bitmap = invertedImage(bitmap);
        bitmap = bildVerarbeitung(bitmap);
        bitmap = fillGaps(bitmap);
        return bitmap;
    }

    public Bitmap rotateBitmap(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    public String readSevenSegment(Bitmap bitmap) {
        OcrManager manager = new OcrManager();
        manager.initAPI();
        String result = manager.startRecognize(bitmap);
        return result;
    }

    private Bitmap convertBitmapToGray(Bitmap b) {
        Mat tmp = new Mat(b.getWidth(), b.getHeight(), CvType.CV_8UC1);
        Utils.bitmapToMat(b, tmp);
        Imgproc.cvtColor(tmp, tmp, Imgproc.COLOR_RGB2GRAY);
        //there could be some processing
        Imgproc.cvtColor(tmp, tmp, Imgproc.COLOR_GRAY2RGB, 4);
        Utils.matToBitmap(tmp, b);
        return b;
    }

    public Bitmap convertBitmapToBlackWhite(Bitmap outmat) {
        Mat srcMat = new Mat();
        Utils.bitmapToMat(outmat, srcMat);

        Mat gray = new Mat();
        Imgproc.cvtColor(srcMat, gray, Imgproc.COLOR_RGBA2GRAY);

        if (gray.type() == CvType.CV_8UC1) {
            Imgproc.threshold(gray, srcMat, 30, 255, Imgproc.THRESH_BINARY + Imgproc.THRESH_OTSU);
        } else {
            gray.convertTo(srcMat, CvType.CV_8UC1);
            Imgproc.threshold(srcMat, srcMat, 30, 255, Imgproc.THRESH_BINARY + Imgproc.THRESH_OTSU);
        }

        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(gray, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        for (int contourIdx = 0; contourIdx < contours.size(); contourIdx++) {
            Imgproc.drawContours(gray, contours, -1, new Scalar(0, 0, 255));
        }

        Bitmap b = Bitmap.createBitmap(srcMat.cols(), srcMat.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(srcMat, b);

        return b;
    }

    private Bitmap fillGapsBitmap(Bitmap bmp) {
        Mat src = new Mat();
        Utils.bitmapToMat(bmp, src);
        Mat gray = new Mat();
        Imgproc.cvtColor(src, gray, Imgproc.COLOR_RGBA2GRAY);

        Imgproc.Canny(gray, gray, 50, 200);
        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        Mat hierarchy = new Mat();
        // find contours:
        Imgproc.findContours(gray, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);
        for (int contourIdx = 0; contourIdx < contours.size(); contourIdx++) {
            Imgproc.drawContours(src, contours, contourIdx, new Scalar(153, 255, 204), -1);
        }

        Bitmap bitmap = Bitmap.createBitmap(src.cols(), src.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(src, bitmap);


        return bitmap;
    }

    private Bitmap invertedImage(Bitmap footbm) {
        Mat mat = new Mat();

        //convert bitmap to opencv Mat
        Utils.bitmapToMat(footbm, mat);

        Mat grayscaledMat = new Mat();
        Imgproc.cvtColor(mat, grayscaledMat, Imgproc.COLOR_BGR2GRAY);

        Mat thresh = new Mat();
        Imgproc.threshold(grayscaledMat, thresh, 30, 255, Imgproc.THRESH_BINARY_INV);

        Bitmap bmp = Bitmap.createBitmap(thresh.cols(), thresh.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(thresh, bmp);


        return bmp;
    }

    public Bitmap bildVerarbeitung(Bitmap image) {

        Mat tmp = new Mat(image.getWidth(), image.getHeight(), CvType.CV_8UC1);
        Utils.bitmapToMat(image, tmp);
        Imgproc.cvtColor(tmp, tmp, Imgproc.COLOR_RGB2GRAY);
        Imgproc.GaussianBlur(tmp, tmp, new Size(3, 3), 0);
        Imgproc.threshold(tmp, tmp, 0, 255, Imgproc.THRESH_OTSU);
        Utils.matToBitmap(tmp, image);


        return image;
    }

    private Bitmap fillGaps(Bitmap bmp) {
        Mat src = new Mat();
        Utils.bitmapToMat(bmp, src);
        Mat gray = new Mat();
        Imgproc.cvtColor(src, gray, Imgproc.COLOR_RGBA2GRAY);

        Imgproc.Canny(gray, gray, 30, 100);
        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(gray, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);
        Mat kernel = new Mat(new Size(SIZE_DISTANCE, SIZE_DISTANCE), CvType.CV_8UC1, new Scalar(255));
        Mat tmp = new Mat();
        Imgproc.morphologyEx(src, tmp, Imgproc.MORPH_OPEN, kernel);
        Imgproc.morphologyEx(tmp, gray, Imgproc.MORPH_CLOSE, kernel);
        Bitmap bitmap = Bitmap.createBitmap(gray.cols(), gray.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(gray, bitmap);

        return bitmap;
    }

}
