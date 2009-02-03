package net.sf.gratia.services;

public class GCHelper {

    private static final Runtime s_runtime = Runtime.getRuntime ();

    public static void runGC() throws Exception {
        for (int r = 0; r < 4; ++r) _runGC();
    }

    private static void _runGC() throws Exception {
        long usedMem1 = usedMemory(), usedMem2 = Long.MAX_VALUE;
        for (int i = 0; (usedMem1 < usedMem2) && (i < 500); ++i) {
            s_runtime.runFinalization();
            s_runtime.gc();
            Thread.currentThread().yield();
            usedMem2 = usedMem1;
            usedMem1 = usedMemory();
        }
    }

    private static long usedMemory() {
        return s_runtime.totalMemory() - s_runtime.freeMemory();
    }

}
