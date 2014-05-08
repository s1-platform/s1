package objects;

import org.s1.objects.Objects;
import org.s1.objects.Ranges;
import org.s1.testing.BasicTest;
import org.s1.testing.LoadTestUtils;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * @author Grigory Pykhov (grigoryp@hflabs.ru)
 */
public class RangeTest extends BasicTest {

    @DataProvider(name = "longs")
    protected Object[][] getLongsCases(){
        return new Object[][]{
                {0,0,10},
                {0,10,10},
                {0,9,10},
                {0,11,10},
                {0,1000,5}
        };
    }

    @DataProvider(name = "doubles")
    protected Object[][] getDoublesCases(){
        return new Object[][]{
                {0,0,10},
                {0,10,10},
                {0,9,10},
                {0,11,10},
                {0,1000,5}
        };
    }

    @DataProvider(name = "dates")
    protected Object[][] getDatesCases(){
        return new Object[][]{
                {"yyyy-MM-dd","2009-12-12","2915-12-12",10},
                {"yyyy-MM-dd","2009-12-12","2015-12-12",10},
                {"yyyy-MM-dd","2009-12-12","2010-12-12",10},
                {"yyyy-MM-dd","2009-12-11","2009-12-13",10},
                {"yyyy-MM-dd","2009-12-11","2009-12-29",10},
                {"yyyy-MM-dd","2009-12-11","2009-12-12",10},
                {"yyyy-MM-dd HH:mm","2009-12-12 12:12","2009-12-12 20:12",10},
                {"yyyy-MM-dd HH:mm","2009-12-12 12:12","2009-12-12 12:42",10},
                {"yyyy-MM-dd HH:mm","2009-12-12 12:12","2009-12-12 12:13",10},
                {"yyyy-MM-dd HH:mm:ss","2009-12-12 12:12:12","2009-12-12 12:12:32",10},
                {"yyyy-MM-dd HH:mm:ss","2009-12-12 12:12:12","2009-12-12 12:12:13",10},
                {"yyyy-MM-dd HH:mm:ss","2009-12-12 12:12:12","2009-12-12 12:12:22",10},
                {"yyyy-MM-dd HH:mm:ss","2009-12-12 12:12:12","2009-12-12 12:12:23",10},
        };
    }

    @Test(dataProvider = "longs")
    public void testLong(long min, long max, int groups){
        trace(Ranges.getLongRange(min, max, groups));
        if(groups>0) {
            assertTrue(Ranges.getLongRange(min, max, groups).size() > 0);
            assertTrue(Ranges.getLongRange(min,max,groups).size()<=groups+1);

            List<Long> l = Ranges.getLongRange(min,max,groups);
            assertTrue(l.get(0)==min);
            if(l.size()>2)
                assertTrue(l.get(l.size()-2)<max);
            assertTrue(l.get(l.size()-1)>=max);
        }else
            assertTrue(Ranges.getLongRange(min,max,groups).size()==0);

    }

    @Test(dataProvider = "longs")
    public void testLongInverted(long max, long min, int groups){
        trace(Ranges.getLongRange(min, max, groups));
        if(groups>0) {
            assertTrue(Ranges.getLongRange(min, max, groups).size() > 0);
            assertTrue(Ranges.getLongRange(min,max,groups).size()<=groups+1);

            List<Long> l = Ranges.getLongRange(min,max,groups);
            assertTrue(l.get(0)==min);
            if(l.size()>2)
                assertTrue(l.get(l.size()-2)>max);
            assertTrue(l.get(l.size()-1)<=max);
        }else
            assertTrue(Ranges.getLongRange(min,max,groups).size()==0);

    }

    @Test(dataProvider = "doubles")
    public void testDoubles(double min, double max, int groups){
        trace(Ranges.getDoubleRange(min, max, groups));
        if(groups>0) {
            if(max==min)
                assertTrue(Ranges.getDoubleRange(min,max,groups).size()==1);
            else
                assertTrue(Ranges.getDoubleRange(min,max,groups).size()==groups+1);

            List<Double> l = Ranges.getDoubleRange(min,max,groups);
            assertTrue(l.get(0)==min);
            if(l.size()>2)
                assertTrue(l.get(l.size()-2)<max);
            assertTrue(l.get(l.size()-1)==max);
        }else
            assertTrue(Ranges.getDoubleRange(min,max,groups).size()==0);

    }

    @Test(dataProvider = "doubles")
    public void testDoublesInverted(double max, double min, int groups){
        trace(Ranges.getDoubleRange(min, max, groups));
        if(groups>0) {
            if(max==min)
                assertTrue(Ranges.getDoubleRange(min,max,groups).size()==1);
            else
                assertTrue(Ranges.getDoubleRange(min,max,groups).size()==groups+1);

            List<Double> l = Ranges.getDoubleRange(min,max,groups);
            assertTrue(l.get(0)==min);
            if(l.size()>2)
                assertTrue(l.get(l.size()-2)>max);
            assertTrue(l.get(l.size()-1)==max);
        }else
            assertTrue(Ranges.getDoubleRange(min,max,groups).size()==0);

    }

    @Test(dataProvider = "dates")
    public void testDates(String format, String smin, String smax, int groups) throws ParseException {

        Date min = new SimpleDateFormat(format).parse(smin);
        Date max = new SimpleDateFormat(format).parse(smax);
        trace(Ranges.getDatePrecision(min,max,groups));
        trace(Ranges.getDateRange(min, max, groups));
        if(groups>0) {
            if(max.getTime()==min.getTime())
                assertTrue(Ranges.getDateRange(min,max,groups).size()==1);
            else
                assertTrue(Ranges.getDateRange(min,max,groups).size()<=groups+1);

            List<Date> l = Ranges.getDateRange(min,max,groups);
            assertTrue(l.get(0).getTime()<=min.getTime());
            if(l.size()>2)
                assertTrue(l.get(l.size()-2).getTime()<max.getTime());
            assertTrue(l.get(l.size()-1).getTime()>=max.getTime());
        }else
            assertTrue(Ranges.getDateRange(min,max,groups).size()==0);

    }

    @Test(dataProvider = "dates")
    public void testDatesInverted(String format, String smax, String smin, int groups) throws ParseException {

        Date min = new SimpleDateFormat(format).parse(smin);
        Date max = new SimpleDateFormat(format).parse(smax);
        trace(Ranges.getDatePrecision(min,max,groups));
        trace(Ranges.getDateRange(min, max, groups));
        if(groups>0) {
            if(max.getTime()==min.getTime())
                assertTrue(Ranges.getDateRange(min,max,groups).size()==1);
            else
                assertTrue(Ranges.getDateRange(min,max,groups).size()<=groups+1);

            List<Date> l = Ranges.getDateRange(min,max,groups);
            assertTrue(l.get(0).getTime()>=min.getTime());
            if(l.size()>2)
                assertTrue(l.get(l.size()-2).getTime()>max.getTime());
            assertTrue(l.get(l.size()-1).getTime()<=max.getTime());
        }else
            assertTrue(Ranges.getDateRange(min,max,groups).size()==0);

    }
}
