package com.smdt.arpgetmac;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Locale;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.view.Menu;
import android.widget.TextView;

public class MainActivity extends Activity {


    private final String targ_mac="48:2c:a0:7f:df:4d";
    private TextView textshow,status_show;
    private String arp_flag = "";
    private String arp_mac = "";

    private String targ_flag = "";
    //private String targ_mac = "54:25:ea:dc:ff:1c";

    private final int ONLINE = 0x01;
    private final int OFFLINE = 0x02;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textshow = (TextView)findViewById(R.id.textView1);
        status_show = (TextView)findViewById(R.id.textView2);
        status_show.setTextColor(android.graphics.Color.RED);
        ping_test();

    }

    private void send_data_det() throws IOException
    {
        DatagramPacket dp = new DatagramPacket(new byte[0], 0, 0);
        DatagramSocket socket = new DatagramSocket();
        int position = 1;
        while (position < 255) {
            dp.setAddress(InetAddress.getByName("192.9.51." + String.valueOf(position)));//探测192.xx.x.网段内地址所有IP地址
            socket.send(dp);
            position++;
            if (position == 125) {//分两段掉包，一次性发的话，达到236左右，会耗时3秒左右再往下发
                socket.close();
                socket = new DatagramSocket();
            }
        }
        socket.close();

    }

    Handler myHandler = new Handler(){
        public void handleMessage(Message msg) {
            if(msg.what == ONLINE){
                status_show.setText("OnLine");
            }else if(msg.what == OFFLINE){
                status_show.setText("OffLne");
            }
        };
    };

    private void sendConnectSuccessMsg(int sendMsg)
    {
        Message msg = new Message();
        msg.what = sendMsg;
        myHandler.sendMessage(msg);

    }

    private void ping_test()
    {

        new Thread(new Runnable() {
            @Override
            public void run() {
                // TODO Auto-generated method stub
                while(true){
                    try {
                        send_data_det();
                    } catch (IOException e1) {
                        // TODO Auto-generated catch block
                        e1.printStackTrace();
                    }
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    readArp();
                    Log.d("kfliu","arp_flag is"+arp_flag+"mac is:"+arp_mac);
                    if(targ_flag.equals("0x2") && arp_mac.equals("48:2c:a0:7f:df:4d"))
                        sendConnectSuccessMsg(ONLINE);
                    else
                        sendConnectSuccessMsg(OFFLINE);
                }
            }
        }).start();

    }

    private void readArp() {
        try {
            BufferedReader br = new BufferedReader(
                    new FileReader("/proc/net/arp"));
            String line = "";
            String ip = "";
            String lst_string = "";
            while ((line = br.readLine()) != null) {
                try {
                    line = line.trim();
                    if (line.length() < 63) continue;
                    if (line.toUpperCase(Locale.US).contains("IP")) continue;
                    ip = line.substring(0, 17).trim();
                    arp_flag = line.substring(29, 32).trim();
                    arp_mac = line.substring(41, 63).trim();
                    if(arp_mac.equals(targ_mac)) {
                        targ_flag = arp_flag;
                        arp_mac = targ_mac;
                        break;
                    }
                    if (arp_mac.contains("00:00:00:00:00:00")) continue;
                    lst_string +=("readArp: mac= "+arp_mac+" ; ip= "+ip+" ;flag= "+arp_flag+"\n");
                } catch (Exception e) {
                }
            }
            textshow.setText(lst_string);
            br.close();

        } catch(Exception e) {
        }
    }

}
