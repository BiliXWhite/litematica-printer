package me.aleksilassila.litematica.printer.printer.zxy.chesttracker;

import red.jackf.chesttracker.memory.MemoryBank;
import red.jackf.chesttracker.storage.Storage;

public class MemoryUtils {
    public void deleteCurrentStorage(){
        if(MemoryBank.INSTANCE !=null) Storage.delete(MemoryBank.INSTANCE.getId());
    }
}
