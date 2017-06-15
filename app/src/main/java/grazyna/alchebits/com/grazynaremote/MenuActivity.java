package grazyna.alchebits.com.grazynaremote;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

public class MenuActivity extends AppCompatActivity {
    private String m_btDevName = "";
    private String m_btMacAddress = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);
        setButtonsListeners();

        Bundle extras = getIntent().getExtras();
        if(extras != null) {
            m_btDevName = extras.getString(Extra.EXTRA_BT_NAME);
            m_btMacAddress = extras.getString(Extra.EXTRA_BT_MAC);

            Log.d(MainActivity.APP_NAME, "taken bt name and mac: "+m_btDevName+"@"+m_btMacAddress);
        }
    }

    protected void setButtonsListeners()
    {
        Button calibrateBttn = (Button) findViewById(R.id.calibrate_bttn);
        Button controlBttn = (Button) findViewById(R.id.control_bttn);

        final Context ctx = this;
        calibrateBttn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(ctx, CalibrateActivity.class);
                intent.putExtra(Extra.EXTRA_BT_NAME, m_btDevName);
                intent.putExtra(Extra.EXTRA_BT_MAC, m_btMacAddress);
                ctx.startActivity(intent);
            }
        });
        controlBttn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(ctx, ControlActivity.class);
                intent.putExtra(Extra.EXTRA_BT_NAME, m_btDevName);
                intent.putExtra(Extra.EXTRA_BT_MAC, m_btMacAddress);
                ctx.startActivity(intent);
            }
        });
    }

}
