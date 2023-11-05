package ldr.server.storage.index;

import java.util.Set;

import ldr.server.storage.IHardMemory;

public interface IFastIndex extends IHardMemory {
    Set<Long> getNearest(double[] vector);
}
