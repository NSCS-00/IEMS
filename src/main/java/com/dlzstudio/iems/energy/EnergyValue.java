package com.dlzstudio.iems.energy;

import java.math.BigInteger;
import java.util.Objects;

/**
 * 高精度能量值类
 * 使用 BigInteger 存储能量值，支持 SE/GE/FE/AE 单位
 * 
 * 换算关系:
 * 1 SE = 100,000,000 GE
 * 1 GE = 9,000,000,000,000,000,000 FE (9×10^18)
 * 1 SE = 9×10^26 FE
 */
public class EnergyValue implements Comparable<EnergyValue> {
    
    private static final BigInteger GE_TO_FE = BigInteger.valueOf(9_000_000_000_000_000_000L);
    private static final BigInteger SE_TO_GE = BigInteger.valueOf(100_000_000);
    private static final BigInteger SE_TO_FE = GE_TO_FE.multiply(SE_TO_GE);
    private static final BigInteger AE_TO_FE = BigInteger.ONE;
    
    private final BigInteger valueInFE;
    
    public enum EnergyUnit {
        FE("FE", BigInteger.ONE),
        AE("AE", AE_TO_FE),
        GE("GE", GE_TO_FE),
        SE("SE", SE_TO_FE);
        
        private final String name;
        private final BigInteger toFE;
        
        EnergyUnit(String name, BigInteger toFE) {
            this.name = name;
            this.toFE = toFE;
        }
        
        public String getName() { return name; }
        public BigInteger getToFE() { return toFE; }
    }
    
    public EnergyValue(long value, EnergyUnit unit) {
        this(BigInteger.valueOf(value), unit);
    }
    
    public EnergyValue(BigInteger value, EnergyUnit unit) {
        this.valueInFE = value.multiply(unit.toFE);
    }
    
    public BigInteger getValueInFE() { return valueInFE; }
    
    public EnergyUnit getBestDisplayUnit() {
        if (valueInFE.compareTo(SE_TO_FE) >= 0) return EnergyUnit.SE;
        if (valueInFE.compareTo(GE_TO_FE) >= 0) return EnergyUnit.GE;
        return EnergyUnit.FE;
    }
    
    @Override
    public String toString() {
        EnergyUnit unit = getBestDisplayUnit();
        return valueInFE.divide(unit.toFE).toString() + unit.name;
    }
    
    public String toStringInUnit(EnergyUnit unit) {
        return valueInFE.divide(unit.toFE).toString() + unit.name;
    }
    
    public BigInteger getValueIn(EnergyUnit unit) {
        return valueInFE.divide(unit.toFE);
    }
    
    public BigInteger getFE() { return valueInFE; }
    public BigInteger getSE() { return valueInFE.divide(SE_TO_FE); }
    public BigInteger getGE() { return valueInFE.divide(GE_TO_FE); }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EnergyValue that = (EnergyValue) o;
        return Objects.equals(valueInFE, that.valueInFE);
    }
    
    @Override
    public int hashCode() { return Objects.hash(valueInFE); }
    
    @Override
    public int compareTo(EnergyValue other) {
        return valueInFE.compareTo(other.valueInFE);
    }
    
    public EnergyValue add(EnergyValue other) {
        return new EnergyValue(this.valueInFE.add(other.valueInFE), EnergyUnit.FE);
    }
    
    public EnergyValue subtract(EnergyValue other) {
        return new EnergyValue(this.valueInFE.subtract(other.valueInFE), EnergyUnit.FE);
    }
    
    public EnergyValue multiply(long factor) {
        return new EnergyValue(this.valueInFE.multiply(BigInteger.valueOf(factor)), EnergyUnit.FE);
    }
    
    public EnergyValue divide(long divisor) {
        return new EnergyValue(this.valueInFE.divide(BigInteger.valueOf(divisor)), EnergyUnit.FE);
    }
    
    public EnergyValue min(EnergyValue other) {
        return this.compareTo(other) <= 0 ? this : other;
    }
    
    public EnergyValue max(EnergyValue other) {
        return this.compareTo(other) >= 0 ? this : other;
    }
    
    public boolean isEmpty() { return valueInFE.equals(BigInteger.ZERO); }
    public boolean isZero() { return valueInFE.equals(BigInteger.ZERO); }
    public boolean isFull(EnergyValue capacity) { return valueInFE.compareTo(capacity.valueInFE) >= 0; }
    
    public static EnergyValue zero() {
        return new EnergyValue(BigInteger.ZERO, EnergyUnit.FE);
    }
}
