package utils;

public class MathUtils {
    /**
     * Method rounds value with specified number of decimal digits
     * @param value  Source value
     * @param precision Number of decimal digits
     * @return Rounded value
     */
    public static Double round(Object value,int precision) {
        Double result = Double.valueOf(value.toString());
        return (double)Math.round(result*(10*precision))/(10*precision);
    }
}
