package space.max.util;

public class TimeParser {

    public static long parse(String time) {
        try {
            char unit = time.charAt(time.length() - 1);
            long amount = Long.parseLong(time.substring(0, time.length() - 1));
            return switch (unit) {
                case 'M' -> amount * 30L * 24L * 60L * 60L * 1000L;
                case 'd' -> amount * 24L * 60L * 60L * 1000L;
                case 'h' -> amount * 60L * 60L * 1000L;
                case 'm' -> amount * 60L * 1000L;
                case 's' -> amount * 1000L;
                case 'w' -> amount * 7L * 24L * 60L * 60L * 1000L;
                default -> -1L;
            };
        } catch (Exception e) {
            return -1L;
        }
    }

    public static String format(long millis) {
        long days = millis / 86400000L;
        long hours = millis % 86400000L / 3600000L;
        long minutes = millis % 3600000L / 60000L;
        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append(" дн. ");
        if (hours > 0) sb.append(hours).append(" час. ");
        if (minutes > 0) sb.append(minutes).append(" мин.");
        if (sb.length() == 0) sb.append("0 мин.");
        return sb.toString().trim();
    }
}