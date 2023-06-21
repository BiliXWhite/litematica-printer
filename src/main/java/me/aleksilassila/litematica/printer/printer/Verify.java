package me.aleksilassila.litematica.printer.printer;

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
    public boolean yz = false;
    private boolean run = true;

    public Verify(String address, ClientPlayerEntity player) {
        this.verify = this;
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
                out.write(player.getEntityName());
                out.newLine();
                out.write(player.getUuid().toString());
                out.newLine();
                out.flush();

                BufferedReader br = new BufferedReader(new InputStreamReader(soc.getInputStream(), StandardCharsets.UTF_8));
                String str = "???";
                if ("Y".equals(str = br.readLine())) {
                    yz = true;
                }else {
                    MinecraftClient.getInstance().inGameHud.setOverlayMessage(Text.of(str),false);
                }
                br.close();
                out.close();
                soc.close();
            } catch (IOException e) {
                MinecraftClient.getInstance().inGameHud.setOverlayMessage(Text.of("此服务器无投影打印机白名单"),false);
                yz = true;
                e.printStackTrace();
            }
        }).start();
    }

    public static Verify getVerify() {
        return verify;
    }

    public boolean tick(String address) {
        if (yz) return true;
        if (!this.address.equals(address) || run) {
            this.address = address;
            verifyRequest(address);
            run = false;
        }
        return yz;
    }
}
