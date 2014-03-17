package com.timpo.batphone.holders;

import java.util.NoSuchElementException;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class RoundRobinHolderTest {

    private RoundRobinHolder<String> holder;

    @Before
    public void setUp() {
        holder = new ArrayListHolder<>();
    }

    @Test
    public void testSingleElementHolders() {
        String a = "a";

        holder.add(a);

        assertEquals(a, holder.next());
        assertEquals(a, holder.next());
    }

    @Test(expected = NoSuchElementException.class)
    public void testEmptyHoldersCantGetNextElement() {
        String empty = holder.next();
    }

    @Test
    public void testMultiElementHolders() {
        String a = "a";
        String b = "b";

        holder.add(a);
        holder.add(b);

        assertEquals(a, holder.next());
        assertEquals(b, holder.next());

        //and again
        assertEquals(a, holder.next());
        assertEquals(b, holder.next());
    }

    @Test
    public void testRemovingElements() {
        String a = "a";
        String b = "b";

        holder.add(a);
        holder.add(b);

        holder.remove("a");

        //this should only contain 'b' now
        assertEquals(b, holder.next());
        assertEquals(b, holder.next());

        holder.remove(b);

        try {
            holder.next();
            fail();
        } catch (NoSuchElementException ex) {
            //this is expected, since the holder is empty
        } catch (Exception ex) {
            fail();
        }

        holder.add(a);
        assertEquals(a, holder.next());
    }

    @Test
    public void testClearingElements() {
        //TODO: implement
//        String a = "a";
//        String b = "b";
//
//        holder.replaceAll(Lists.newArrayList(a, b));
//        
//        assertEquals(a, holder.next());
//        assertEquals(b, holder.next());
    }
}