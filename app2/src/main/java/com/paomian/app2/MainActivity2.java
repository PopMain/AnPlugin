package com.paomian.app2;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.Toast;

import com.paomian.app2.util.Caculator;
import com.paomian.app2.util.NativeCaculator;
import com.paomian.app2.util.ResourceUtil;

public class MainActivity2 extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        System.out.println("result = " + NativeCaculator.add());
        Toast.makeText(this, ResourceUtil.getString(this), Toast.LENGTH_SHORT).show();
    }
}
