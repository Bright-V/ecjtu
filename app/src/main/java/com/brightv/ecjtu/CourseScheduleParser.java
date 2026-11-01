package cn.edu.ecjtu;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Pure timetable parsing logic, independent from Android views. */
final class CourseScheduleParser {
    private CourseScheduleParser() {}

    static String[] splitCourseEntries(String rawDay) {
        ArrayList<String> entries = new ArrayList<>();
        if (rawDay == null || rawDay.trim().isEmpty()) return new String[0];
        String currentSlot = "";
        ArrayList<String> block = new ArrayList<>();
        Pattern slotPattern = Pattern.compile("^(1-2|3-4|5-6|7-8|9-10|11-12|13-14)(?:\\t| {2,})(.*)$");
        Pattern weekPattern = Pattern.compile("^\\d+(?:-\\d+)?(?:\\s*,\\s*\\d+(?:-\\d+)?)*(?:\\s*[（(](?:单|双)[）)])?\\s+[\\d,]+$");
        for (String rawLine : rawDay.split("\\r?\\n")) {
            String line = rawLine.trim().replaceFirst("^[•·]\\s*", "");
            if (line.isEmpty()) continue;
            Matcher slot = slotPattern.matcher(line);
            if (slot.matches()) {
                append(entries, currentSlot, block, null);
                currentSlot = slot.group(1);
                block.clear();
                if (!slot.group(2).trim().isEmpty()) block.add(slot.group(2).trim());
                continue;
            }
            if (weekPattern.matcher(line).matches()) {
                append(entries, currentSlot, block, line);
                block.clear();
            } else if (!line.equals("-") && !line.equals("无")) {
                block.add(line);
            }
        }
        append(entries, currentSlot, block, null);
        return entries.toArray(new String[0]);
    }

    private static void append(ArrayList<String> entries, String slot, ArrayList<String> block, String weekLine) {
        if (block.isEmpty()) return;
        StringBuilder item = new StringBuilder();
        if (slot != null && !slot.isEmpty()) item.append(slot);
        for (String part : block) {
            if (item.length() > 0) item.append(" ");
            item.append(part.trim());
        }
        if (weekLine != null && !weekLine.trim().isEmpty()) item.append(" ").append(weekLine.trim());
        entries.add(item.toString().trim());
    }

    static boolean courseOccursInWeek(String line, int weekNo) {
        if (weekNo <= 0) return true;
        Matcher tail = Pattern.compile("((?:\\d+(?:-\\d+)?(?:\\s*[（(](?:单|双)[）)])?\\s*,\\s*)*\\d+(?:-\\d+)?(?:\\s*[（(](?:单|双)[）)])?)\\s+[\\d,]+$").matcher(line.trim());
        if (!tail.find()) return true;
        String[] ranges = tail.group(1).split("\\s*,\\s*");
        for (String range : ranges) {
            Matcher part = Pattern.compile("^(\\d+)(?:-(\\d+))?\\s*[（(]?(单|双)?[）)]?$").matcher(range.trim());
            if (!part.matches()) continue;
            try {
                int first = Integer.parseInt(part.group(1));
                int last = part.group(2) == null ? first : Integer.parseInt(part.group(2));
                String parity = part.group(3);
                if (weekNo < first || weekNo > last) continue;
                if ("单".equals(parity) && weekNo % 2 == 0) continue;
                if ("双".equals(parity) && weekNo % 2 != 0) continue;
                return true;
            } catch (Exception ignored) {}
        }
        return false;
    }
}
