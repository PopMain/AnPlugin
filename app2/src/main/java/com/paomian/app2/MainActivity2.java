package com.paomian.app2;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.paomian.app2.util.Caculator;

public class MainActivity2 extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        System.out.println("result = " + Caculator.add());
    }
}
