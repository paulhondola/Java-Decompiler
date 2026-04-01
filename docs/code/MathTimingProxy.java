// Demo for Dynamic Proxy
// Defines  a Timing proxy -  invocationhanndler
// Applies Timing proxy to a Math service

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

// The Math service
interface IMath {
    int add(int a, int b);
    int mult(int a, int b);
}

class Math implements IMath {
    public int add(int a, int b) {
        return a + b;
    }

    public int mult(int a, int b) {
        return a * b;
    }
}

// Defines Invocationhandler for a Timing proxy
class TimingHandler implements InvocationHandler {

    private Object target;

    public TimingHandler(Object target) {
        this.target = target;
    }

    public Object invoke(Object proxy, Method method, Object[] args)
            throws Throwable {

        long start = System.nanoTime();
        Object result = method.invoke(target, args);
        long elapsed = (System.nanoTime() - start)/1000000;

        System.out.println("Execution of method " + method.getName()
                + " finished in " + elapsed + " ms");

        return result;
    }
}

//Constructs a Math Timing Proxy applying the TimingHandler with the IMath interface
// on a Math target object
public class MathTimingProxy {
    public static void main(String[] args) {

        IMath target = new Math();

        IMath mathTimingProxyInstance = (IMath) Proxy.newProxyInstance(
                MathTimingProxy.class.getClassLoader(),
                new Class[]{IMath.class},
                new TimingHandler(target)
        );

        System.out.println("Class of dynamic proxy is " +
                mathTimingProxyInstance.getClass().getName());

        mathTimingProxyInstance.add(3, 4);
        mathTimingProxyInstance.add(30, 40);
        mathTimingProxyInstance.mult(5, 7);
    }
}