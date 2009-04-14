package de.jexp.embeddedtestrunner;

import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Michael Hunger
 * @since 12.04.2009
 */
@RunWith(EmbeddedRunner.class)
public class Calculator {
    public int add(final int a, final int b) {
        class AddTest {
            @Test
            public void testAdd() {
                assertEquals(3,add(1,2));
                assertEquals(1,add(0,1));
                assertEquals(0,add(-1,1));
                assertEquals(0,add(0,0));
                assertEquals(Integer.MIN_VALUE,add(Integer.MAX_VALUE, 1));
            }
        }
        return a+b;
    }

    public static void main(final String[] args) throws Exception {
        final Calculator calculator = new Calculator();
        final int c = calculator.add(1,2);
        System.out.println("c = " + c);
    }
}
