package me.aleksilassila.litematica.printer.printer.zxy.Utils;

import me.aleksilassila.litematica.printer.printer.bedrockUtils.Messager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;


public class Verify {
    private Socket soc;
    public static Verify verify;
    private String address;
    private ClientPlayerEntity player;
    long verifyTime = System.currentTimeMillis();
    private byte step = 0; //0未验证 1验证中 2验证完成 3验证失败
    private boolean result = false;

    public Verify(String address, ClientPlayerEntity player) {
        verify = this;
        this.player = player;
        this.address = address;
    }

    public void verifyRequest(String address) {
        new Thread(() -> {
            try {
                soc = new Socket(address, 25665);
                BufferedWriter out = new BufferedWriter(new OutputStreamWriter(soc.getOutputStream(),StandardCharsets.UTF_8));
                out.write("printer usage request,ID: UUID: ");
                out.newLine();
                out.write(player.getGameProfile().getName());
                out.newLine();
                out.write(player.getUuid().toString());
                out.newLine();
                out.flush();

                BufferedReader br = new BufferedReader(new InputStreamReader(soc.getInputStream(), StandardCharsets.UTF_8));
                String str = "???";
                if ("Y".equals(str = br.readLine())) {
                    result = true;
                }else if(str != null){
                    MinecraftClient.getInstance().inGameHud.setOverlayMessage(Text.of(str),false);
                }else {
                    step = 3;
                    return;
                }
                br.close();
                out.close();
                soc.close();
                step = 2;
            } catch (IOException e) {
//                MinecraftClient.getInstance().inGameHud.setOverlayMessage(Text.of("此服务器未限制打印机使用"),false);
                result = true;
            }
        }).start();
    }

    public static Verify getVerify() {
        return verify;
    }

    public boolean tick(String address) {
        switch (step){
            case 0 ->{
                verifyRequest(address);
                step = 1;
            }
            case 1 ->{
                long currTime = System.currentTimeMillis();
                if (this.verifyTime + 3000L < currTime){
                    step = 3;
                    return true; //验证时间超过3秒未回复视为服务器端口并非用于打印机验证
                }
            }
            case 2 ->{
                return result;
            }
            case 3 ->{
                return true;
            }
        }
        return false;
    }
}
