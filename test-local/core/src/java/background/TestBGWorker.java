package background;

import org.s1.background.BackgroundWorker;
import org.s1.objects.Objects;

/**
 * s1v2
 * User: GPykhov
 * Date: 27.01.14
 * Time: 20:06
 */
public class TestBGWorker extends BackgroundWorker {

    public static volatile int a1 = 0;
    public static volatile int a2 = 0;

    @Override
    public void process() {
        if(Objects.get(config,"a",1)==1)
            a1++;
        if(Objects.get(config,"a",1)==2)
            a2++;
        System.out.println("worker tick: " + a1 + ":" + a2);
    }
}
