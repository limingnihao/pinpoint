/**
 * Autogenerated by Thrift Compiler (0.11.0)
 *
 * DO NOT EDIT UNLESS YOU ARE SURE THAT YOU KNOW WHAT YOU ARE DOING
 *  @generated
 */
package com.navercorp.pinpoint.thrift.dto.command;


public enum TThreadState implements org.apache.thrift.TEnum {
  NEW(0),
  RUNNABLE(1),
  BLOCKED(2),
  WAITING(3),
  TIMED_WAITING(4),
  TERMINATED(5),
  UNKNOWN(6);

  private final int value;

  private TThreadState(int value) {
    this.value = value;
  }

  /**
   * Get the integer value of this enum value, as defined in the Thrift IDL.
   */
  public int getValue() {
    return value;
  }

  /**
   * Find a the enum type by its integer value, as defined in the Thrift IDL.
   * @return null if the value is not found.
   */
  public static TThreadState findByValue(int value) { 
    switch (value) {
      case 0:
        return NEW;
      case 1:
        return RUNNABLE;
      case 2:
        return BLOCKED;
      case 3:
        return WAITING;
      case 4:
        return TIMED_WAITING;
      case 5:
        return TERMINATED;
      case 6:
        return UNKNOWN;
      default:
        return null;
    }
  }
}
