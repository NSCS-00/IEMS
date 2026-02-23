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
    // 基础单位�?FE (Forge Energy)
    // 所有内部计算都�?FE 为基�?    private static final BigInteger GE_TO_FE = BigInteger.valueOf(9_000_000_000_000_000_000L); // 9×10^18
    private static final BigInteger SE_TO_GE = BigInteger.valueOf(100_000_000); // 10^8
    private static final BigInteger SE_TO_FE = GE_TO_FE.multiply(SE_TO_GE); // 9×10^26
    
    // AE2 转换�?(1 AE = 1 FE，可根据需要调�?
    private static final BigInteger AE_TO_FE = BigInteger.ONE;
    
    private final BigInteger valueInFE; // �?FE 为内部存储单�?    private final EnergyUnit displayUnit; // 显示单位
    
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
        
        public String getName() {
            return name;
        }
        
        public BigInteger getToFE() {
            return toFE;
        }
    }
    
    public EnergyValue(long value, EnergyUnit unit) {
        this(BigInteger.valueOf(value), unit);
    }
    
    public EnergyValue(BigInteger value, EnergyUnit unit) {
        if (value.compareTo(BigInteger.ZERO) < 0) {
            value = BigInteger.ZERO;
        }
        this.valueInFE = value.multiply(unit.getToFE());
        this.displayUnit = unit;
    }
    
    public EnergyValue(BigInteger valueInFE) {
        if (valueInFE.compareTo(BigInteger.ZERO) < 0) {
            valueInFE = BigInteger.ZERO;
        }
        this.valueInFE = valueInFE;
        this.displayUnit = getBestDisplayUnit(valueInFE);
    }
    
    public static EnergyValue zero() {
        return new EnergyValue(BigInteger.ZERO, EnergyUnit.FE);
    }
    
    public static EnergyValue oneSE() {
        return new EnergyValue(BigInteger.ONE, EnergyUnit.SE);
    }
    
    public static EnergyValue oneGE() {
        return new EnergyValue(BigInteger.ONE, EnergyUnit.GE);
    }
    
    /**
     * �?FE 转换 (低级能源)
     */
    public static EnergyValue fromFE(long fe) {
        return new EnergyValue(BigInteger.valueOf(fe), EnergyUnit.FE);
    }
    
    /**
     * �?GE 转换
     */
    public static EnergyValue fromGE(String geString) {
        return new EnergyValue(new BigInteger(geString), EnergyUnit.GE);
    }
    
    /**
     * �?SE 转换
     */
    public static EnergyValue fromSE(String seString) {
        return new EnergyValue(new BigInteger(seString), EnergyUnit.SE);
    }
    
    /**
     * 获取最佳显示单�?     */
    private static EnergyUnit getBestDisplayUnit(BigInteger valueInFE) {
        if (valueInFE.compareTo(SE_TO_FE) >= 0) {
            return EnergyUnit.SE;
        } else if (valueInFE.compareTo(GE_TO_FE) >= 0) {
            return EnergyUnit.GE;
        } else if (valueInFE.compareTo(AE_TO_FE) >= 0) {
            return EnergyUnit.AE;
        }
        return EnergyUnit.FE;
    }
    
    /**
     * 转换为指定单位的字符串表�?(向下取整)
     */
    public String toString(EnergyUnit unit) {
        BigInteger result = valueInFE.divide(unit.getToFE());
        return result.toString() + unit.getName();
    }
    
    /**
     * 转换为最适合的单位显�?(向下取整)
     */
    public String toString() {
        EnergyUnit unit = getBestDisplayUnit(valueInFE);
        return toString(unit);
    }
    
    /**
     * 获取指定单位的整数�?(向下取整)
     */
    public BigInteger getValue(EnergyUnit unit) {
        return valueInFE.divide(unit.getToFE());
    }
    
    /**
     * 获取 FE �?     */
    public BigInteger getValueInFE() {
        return valueInFE;
    }
    
    /**
     * 获取 SE �?(向下取整)
     */
    public BigInteger getValueInSE() {
        return valueInFE.divide(SE_TO_FE);
    }
    
    /**
     * 获取 GE �?(向下取整)
     */
    public BigInteger getValueInGE() {
        return valueInFE.divide(GE_TO_FE);
    }
    
    /**
     * 加法
     */
    public EnergyValue add(EnergyValue other) {
        return new EnergyValue(this.valueInFE.add(other.valueInFE));
    }
    
    /**
     * 减法
     */
    public EnergyValue subtract(EnergyValue other) {
        BigInteger result = this.valueInFE.subtract(other.valueInFE);
        if (result.compareTo(BigInteger.ZERO) < 0) {
            result = BigInteger.ZERO;
        }
        return new EnergyValue(result);
    }
    
    /**
     * 乘法
     */
    public EnergyValue multiply(long factor) {
        return new EnergyValue(this.valueInFE.multiply(BigInteger.valueOf(factor)));
    }
    
    /**
     * 除法
     */
    public EnergyValue divide(long divisor) {
        return new EnergyValue(this.valueInFE.divide(BigInteger.valueOf(divisor)));
    }
    
    /**
     * 比较
     */
    @Override
    public int compareTo(EnergyValue other) {
        return this.valueInFE.compareTo(other.valueInFE);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EnergyValue that = (EnergyValue) o;
        return valueInFE.equals(that.valueInFE);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(valueInFE);
    }
    
    /**
     * 检查是否为�?     */
    public boolean isEmpty() {
        return valueInFE.equals(BigInteger.ZERO);
    }
    
    /**
     * 检查是否已�?(相对于容�?
     */
    public boolean isFull(EnergyValue capacity) {
        return valueInFE.compareTo(capacity.valueInFE) >= 0;
    }
    
    /**
     * 获取转换后的�?(用于能量转换�?
     * 低级能源可以转为高级能源，但高级转低级可能溢�?     */
    public EnergyValue convertTo(EnergyUnit targetUnit) {
        BigInteger result = valueInFE.divide(targetUnit.getToFE());
        return new EnergyValue(result, targetUnit);
    }
    
    /**
     * 检查是否可以安全转换为目标单位 (不会丢失精度)
     */
    public boolean canSafeConvertTo(EnergyUnit targetUnit) {
        BigInteger remainder = valueInFE.mod(targetUnit.getToFE());
        return remainder.equals(BigInteger.ZERO);
    }
}
