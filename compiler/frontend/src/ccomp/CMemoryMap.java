package ccomp;

/**
 * We emulate a C memory map with the HEAP at 0, the STACK at 0x5000 0000,
 * and a string table (for string constants) at 0x7000 0000
 * 
 * All structures grow towards increasing addresses. 
 */
public class CMemoryMap {
  public static final int STACK = 0x50000000;
  public static final int HEAP = 0;
  public static final int STRING_TABLE = 0x70000000;
  
  public static final int STACK_MAX = STRING_TABLE;
  public static final int HEAP_MAX = STACK;
}
