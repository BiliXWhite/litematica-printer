//#if MC >= 12111
package me.aleksilassila.litematica.printer.integration.voidwarehouse;

import java.util.List;

/** Network capability; inventory ownership remains in the adapter. */
public interface VoidWarehouseRestockPort {
    boolean isSupported();
    int requestTake(List<String> candidates);
    int statusOf(int nonce);
    void forget();
}
//#endif
