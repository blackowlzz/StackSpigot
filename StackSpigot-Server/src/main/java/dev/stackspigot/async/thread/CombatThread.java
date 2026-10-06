// From
// https://github.com/Argarian-Network/NachoSpigot/tree/async-kb-hit
package dev.stackspigot.async.thread;

public class CombatThread extends AsyncPacketThread {
    public CombatThread(String s) {
        super(s);
    }

    // Handle packets
    @Override
    public void run() {
        //StackSpigot-Code
        Runnable packet;
        while ((packet = this.packets.poll()) != null) {
            try {
                packet.run();
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
        //End-of-StackSpigot-Code
    }
} 
