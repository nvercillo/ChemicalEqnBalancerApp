package com.example.chemicalbalancer;

import android.content.Intent;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class HomePage extends AppCompatActivity {

    private Button balance;
    private Button rules;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home_page);

        balance = findViewById(R.id.balanceBtn);
        balance.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openMain2Activity();
            }
        });

        rules = findViewById(R.id.rulesBtn);
        rules.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openMain3Activity();
            }
        });


    }

    public void openMain2Activity() {
        Intent intent = new Intent(this, ChemicalBalancer.class);
        startActivity(intent);
    }


    public void openMain3Activity(){
        Intent intent = new Intent(this, RulesPage.class);
        startActivity(intent);

    }
}