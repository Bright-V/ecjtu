package cn.edu.ecjtu;

import org.junit.Test;
import static org.junit.Assert.*;

public class CourseScheduleParserTest {
    @Test public void splitsCourseEntriesByPeriodAndWeekLine() {
        String[] entries = CourseScheduleParser.splitCourseEntries("1-2\t高等数学 张老师 @教1-201\n1-16 1,2\n3-4\t大学英语 李老师 @语音室");
        assertEquals(2, entries.length);
        assertTrue(entries[0].startsWith("1-2 高等数学"));
        assertTrue(entries[0].contains("1-16"));
        assertTrue(entries[1].startsWith("3-4 大学英语"));
    }
    @Test public void filtersWeekRangesAndParity() {
        assertTrue(CourseScheduleParser.courseOccursInWeek("数学 1-16 1,2", 8));
        assertFalse(CourseScheduleParser.courseOccursInWeek("数学 1-15(单) 1,2", 8));
        assertTrue(CourseScheduleParser.courseOccursInWeek("数学 1-15(单) 1,2", 7));
        assertFalse(CourseScheduleParser.courseOccursInWeek("数学 2-10(双) 1,2", 3));
    }
}
