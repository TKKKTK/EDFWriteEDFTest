package com.wg.edfwriteedftest;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.wg.edfwriteedftest.EDFlib.EDFException;
import com.wg.edfwriteedftest.EDFlib.EDFwriter;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private Button edf_Button_start,edf_Button_stop;
    private boolean isWrite = false;
    private double[][] data= new double[32][500];
    private final String[] Labels = new String[]{"FP1","AF3","F7","F3","FC1","FC5","T7","C3","Cz","FC2","FC6","F8","F4","Fz","AF4","FP2","O2","PO4","P4","P8","CP6","CP2","C4","T8","CP5","CP1","PZ","P3","P7","PO3","O1","OZ"};
    private EDFwriter hdl;
    private int total = 0;
    private int write_count = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        edf_Button_start = findViewById(R.id.edfStart);
        edf_Button_stop = findViewById(R.id.edfStop);

        edf_Button_start.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.Q)
            @Override
            public void onClick(View view) {
                write_count ++;
                for (int i = 0; i < data.length; i++) {
                    for (int j = 0; j < data[i].length /2; j++) {
                        data[i][j] =  10;
                    }
                    for (int j = data[i].length /2; j < data[i].length ; j++) {
                        data[i][j] = -10;
                    }
                }
                //初始化edf数据头信息
                int sf1 = 500, // 通道1的采样频率
                        edfsignals = 32; //通道数
                try {
                    hdl = new EDFwriter( "edf_test"+write_count+".bdf", EDFwriter.EDFLIB_FILETYPE_BDFPLUS, edfsignals, MainActivity.this);
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                } catch (EDFException e) {
                    e.printStackTrace();
                    return;
                }
                //String[] channelStrings = new String[]{"FP1", "AF3", "F7", "F3", "FC1", "FC5", "T7", "C3", "CZ", "FC2", "FC6", "F8", "F4", "FZ", "AF4", "FP2", "O2", "PO4", "P4", "P8", "CP6", "CP2", "C4", "T8", "CP5", "CP1", "PZ", "P3", "P7", "PO3", "O1", "OZ"};
                for (int i = 0; i < edfsignals; i++) {
                    //设置信号的最大物理值
                    hdl.setPhysicalMaximum(i, (int) Math.pow(2, 23) - 1);
                    //设置信号的最小物理值
                    hdl.setPhysicalMinimum(i, (int) Math.pow(-2, 23));
                    //设置信号的最大数字值
                    hdl.setDigitalMaximum(i, (int) Math.pow(2, 23) - 1);
                    //设置信号的最小数字值
                    hdl.setDigitalMinimum(i, (int) Math.pow(-2, 23));
                    //设置信号的物理单位
                    hdl.setPhysicalDimension(i, String.format("uV"));

                    //设置采样频率
                    hdl.setSampleFrequency(i, sf1);

                    //设置信号标签
                    hdl.setSignalLabel(i, String.format(Labels[i], 0 + 1));
                }
                hdl.setNumberOfAnnotationSignals(50);
                isWrite = true;
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        while (isWrite){
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            if (hdl == null){
                                return;
                            }

                            for (int i = 0; i < 500; i++) {
                                total++;
                                if (total % 10 == 0){
                                    long time = calculateTime(total);
                                    int error = hdl.writeAnnotation(time, -1, "F"+ 1);
                                    if (error != -1){
                                        Log.d("MainActivity", "标签写入反馈:"+ error + "时间：" + time);
                                    }
                                }
                            }

                            for (int i = 0; i < edfsignals; i++) {
                                try {
                                    int err = hdl.writePhysicalSamples(data[i]);
                                    if (err != 0) {
                                        System.out.printf("writePhysicalSamples() returned error: %d\n", err);
                                        return;
                                    }
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }

                        }
                    }
                }).start();
            }
        });

        edf_Button_stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                  isWrite = false;
                  closeEdf();
            }
        });
    }

    /**
     * 关闭edf文件流
     */
    public void closeEdf() {
        /**
         * 结尾写入标签
         */
        try {
            hdl.close();
            hdl = null;
            //Toast.makeText(EchartsActivity.this, "导出EDF文件成功", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        } catch (EDFException e) {
            e.printStackTrace();
            return;
        }
    }

    /**
     * 计算当前点的时间 -- 用于记录标签
     *
     * @param total
     * @return
     */
    private long calculateTime(int total) {
        float totalTime = (float) total / 500 * 10000;
        return (long) totalTime;
    }
}